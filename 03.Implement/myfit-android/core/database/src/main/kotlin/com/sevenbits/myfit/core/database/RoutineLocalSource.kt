package com.sevenbits.myfit.core.database

import com.sevenbits.myfit.core.database.dao.RoutineDao
import com.sevenbits.myfit.core.database.dao.RoutineExerciseWithExercise
import com.sevenbits.myfit.core.database.dao.RoutineWithExercises
import com.sevenbits.myfit.core.database.dao.WorkoutDao
import com.sevenbits.myfit.core.database.entity.RoutineEntity
import com.sevenbits.myfit.core.database.entity.RoutineExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.Routine
import com.sevenbits.myfit.core.domain.model.RoutineExercise
import com.sevenbits.myfit.core.domain.model.SetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 루틴 로컬 데이터 소스.
 */
@Singleton
class RoutineLocalSource @Inject internal constructor(
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val workoutLocalSource: WorkoutLocalSource,
) {

    fun observeRoutines(): Flow<List<Routine>> =
        routineDao.observeRoutines(ExerciseLocalSource.LOCAL_USER_ID).map { list ->
            list.map { it.toModel(emptyList()) }
        }

    suspend fun findRoutine(routineId: String): Routine? =
        routineDao.findRoutineWithExercises(routineId)?.toModel()

    suspend fun upsertRoutine(routine: Routine): String {
        val now = System.currentTimeMillis()
        val id = routine.id.ifBlank { UUID.randomUUID().toString() }

        routineDao.upsertRoutine(
            RoutineEntity(
                id = id,
                userId = ExerciseLocalSource.LOCAL_USER_ID,
                name = routine.name,
                description = routine.description,
                targetBodyParts = routine.targetBodyParts.joinToString(",").takeIf { it.isNotBlank() },
                isPreset = false,
                useCount = routine.useCount,
                lastPerformedDate = routine.lastPerformedDate,
                createdAt = now,
                updatedAt = now,
            ),
        )

        routineDao.deleteRoutineExercises(id)
        routineDao.upsertRoutineExercises(
            routine.exercises.mapIndexed { index, item ->
                RoutineExerciseEntity(
                    id = item.id.ifBlank { UUID.randomUUID().toString() },
                    routineId = id,
                    exerciseId = item.exerciseId,
                    orderNo = index + 1,
                    targetSetCount = item.targetSetCount,
                    // 절대 중량과 %1RM 은 택일 — 둘 다 저장하면 어느 쪽이 우선인지 모호해진다
                    targetWeightKg = item.targetWeightKg.takeIf { item.targetPercentOneRm == null },
                    targetPercent1rm = item.targetPercentOneRm,
                    targetReps = item.targetReps,
                    restSec = item.restSec,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
        return id
    }

    suspend fun deactivateRoutine(routineId: String) =
        routineDao.deactivateRoutine(routineId, System.currentTimeMillis())

    /**
     * 루틴을 일지로 인스턴스화한다. (FN-RTN-007)
     *
     * 루틴 정의를 **복제**하므로 이후 일지 수정이 루틴에 영향을 주지 않는다. (REQ-RTN-003)
     * 루틴명은 스냅샷으로 남겨 루틴이 삭제되어도 과거 일지에 표시된다. (FN-RTN-004)
     */
    suspend fun instantiateToLog(routineId: String, date: String, sessionNo: Int): String {
        val routine = routineDao.findRoutineWithExercises(routineId) ?: return ""
        val logId = workoutLocalSource.getOrCreateLog(date, sessionNo)
        val now = System.currentTimeMillis()

        workoutDao.findLogById(logId)?.let { log ->
            workoutDao.upsertLog(
                log.copy(
                    routineId = routineId,
                    routineNameSnapshot = routine.routine.name,
                    updatedAt = now,
                    isDirty = true,
                ),
            )
        }

        var order = workoutDao.maxOrderNo(logId)
        routine.exercises.sortedBy { it.routineExercise.orderNo }.forEach { item ->
            order += 1
            val logExerciseId = UUID.randomUUID().toString()
            workoutDao.upsertLogExercise(
                WorkoutLogExerciseEntity(
                    id = logExerciseId,
                    workoutLogId = logId,
                    exerciseId = item.routineExercise.exerciseId,
                    orderNo = order,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            workoutDao.upsertSets(buildTargetSets(logExerciseId, item, now))
        }

        routineDao.markPerformed(routineId, date, now)
        workoutLocalSource.refreshSummaryCache(logId)
        return logId
    }

    /**
     * 목표값으로 세트를 미리 채운다.
     *
     * %1RM 목표는 실행 시점의 추정 1RM 이 필요하다. 추정 1RM 산출은 Phase 2(분석) 기능이라
     * 현 단계에서는 절대 중량 목표만 반영하고, 비율 목표는 중량을 비워 둔다.
     */
    private fun buildTargetSets(
        logExerciseId: String,
        item: RoutineExerciseWithExercise,
        now: Long,
    ): List<WorkoutSetEntity> {
        val target = item.routineExercise
        val setCount = (target.targetSetCount ?: DEFAULT_SET_COUNT).coerceIn(1, MAX_SET_COUNT)
        return (1..setCount).map { setNo ->
            WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                logExerciseId = logExerciseId,
                setNo = setNo,
                setType = SetType.NORMAL.name,
                weightKg = target.targetWeightKg,
                reps = target.targetReps,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /** 수행한 일지 구성을 루틴으로 저장한다. (FN-RTN-008) */
    suspend fun saveLogAsRoutine(logId: String, routineName: String): String {
        val detail = workoutDao.findLogDetailOnce(logId) ?: return ""
        val now = System.currentTimeMillis()
        val routineId = UUID.randomUUID().toString()

        routineDao.upsertRoutine(
            RoutineEntity(
                id = routineId,
                userId = ExerciseLocalSource.LOCAL_USER_ID,
                name = routineName,
                createdAt = now,
                updatedAt = now,
            ),
        )
        routineDao.upsertRoutineExercises(
            detail.exercises.sortedBy { it.logExercise.orderNo }.mapIndexed { index, item ->
                // 실제 수행한 세트 수와 대표 중량·횟수를 목표값으로 삼는다
                val workingSets = item.sets.filter { it.setType != SetType.WARMUP.name }
                RoutineExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    routineId = routineId,
                    exerciseId = item.logExercise.exerciseId,
                    orderNo = index + 1,
                    targetSetCount = workingSets.size.takeIf { it > 0 },
                    targetWeightKg = workingSets.maxOfOrNull { it.weightKg ?: 0.0 }?.takeIf { it > 0 },
                    targetReps = workingSets.mapNotNull { it.reps }.maxOrNull(),
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
        return routineId
    }

    suspend fun suggestNextRoutine(): Routine? =
        routineDao.findLeastRecentlyPerformed(ExerciseLocalSource.LOCAL_USER_ID)
            ?.let { routineDao.findRoutineWithExercises(it.id)?.toModel() }

    // ── 매핑 ─────────────────────────────────────────────

    private fun RoutineWithExercises.toModel() = routine.toModel(
        exercises.sortedBy { it.routineExercise.orderNo }.map { it.toModel() },
    )

    private fun RoutineEntity.toModel(items: List<RoutineExercise>) = Routine(
        id = id,
        name = name,
        description = description,
        targetBodyParts = targetBodyParts?.split(",")?.filter { it.isNotBlank() }.orEmpty(),
        isPreset = isPreset,
        useCount = useCount,
        lastPerformedDate = lastPerformedDate,
        exercises = items,
    )

    private fun RoutineExerciseWithExercise.toModel() = RoutineExercise(
        id = routineExercise.id,
        routineId = routineExercise.routineId,
        exerciseId = routineExercise.exerciseId,
        exerciseName = exercise.name,
        recordType = enumValues<RecordType>()
            .firstOrNull { it.name == exercise.recordType } ?: RecordType.WEIGHT_REPS,
        orderNo = routineExercise.orderNo,
        targetSetCount = routineExercise.targetSetCount,
        targetWeightKg = routineExercise.targetWeightKg,
        targetPercentOneRm = routineExercise.targetPercent1rm,
        targetReps = routineExercise.targetReps,
        restSec = routineExercise.restSec,
    )

    private companion object {
        const val DEFAULT_SET_COUNT = 3
        const val MAX_SET_COUNT = 20
    }
}
