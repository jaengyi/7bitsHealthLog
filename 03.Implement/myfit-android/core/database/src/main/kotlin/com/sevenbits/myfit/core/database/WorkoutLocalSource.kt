package com.sevenbits.myfit.core.database

import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.dao.LogExerciseWithSets
import com.sevenbits.myfit.core.database.dao.UserDao
import com.sevenbits.myfit.core.database.dao.WorkoutDao
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutLogExercise
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 운동기록 로컬 데이터 소스.
 *
 * Room Entity 는 모듈 밖으로 노출하지 않으므로(R-5) 이 클래스가 경계를 담당한다.
 */
@Singleton
class WorkoutLocalSource @Inject internal constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val userDao: UserDao,
) {

    fun observeLog(logId: String): Flow<WorkoutLog?> =
        workoutDao.observeLogDetail(logId).map { detail -> detail?.toModel() }

    /**
     * 일지를 가져오되 없으면 만든다. (FN-WRK-001)
     *
     * `workout_log` 는 `user` 에 FK 가 걸려 있으므로 게스트 사용자 레코드를 먼저 보장한다.
     * Phase 1 은 로그인 없이 로컬 단독 사용이 기본이다. (REQ-CMN-001)
     */
    suspend fun getOrCreateLog(date: String, sessionNo: Int): String {
        ensureLocalUser()
        workoutDao.findLog(ExerciseLocalSource.LOCAL_USER_ID, date, sessionNo)?.let { return it.id }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        workoutDao.upsertLog(
            WorkoutLogEntity(
                id = id,
                userId = ExerciseLocalSource.LOCAL_USER_ID,
                workoutDate = date,
                sessionNo = sessionNo,
                status = WorkoutStatus.PLANNED.name,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    private suspend fun ensureLocalUser() {
        if (userDao.findCurrentUser() != null) return
        val now = System.currentTimeMillis()
        userDao.upsert(
            UserEntity(
                id = ExerciseLocalSource.LOCAL_USER_ID,
                loginType = "GUEST",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun findLogId(date: String, sessionNo: Int): String? =
        workoutDao.findLog(ExerciseLocalSource.LOCAL_USER_ID, date, sessionNo)?.id

    suspend fun findInProgressLogId(): String? =
        workoutDao.findInProgressLog(ExerciseLocalSource.LOCAL_USER_ID)?.id

    suspend fun countSessionsOn(date: String): Int =
        workoutDao.countSessionsOn(ExerciseLocalSource.LOCAL_USER_ID, date)

    suspend fun updateMemo(logId: String, memo: String) {
        val log = workoutDao.findLogById(logId) ?: return
        workoutDao.upsertLog(
            log.copy(memo = memo, updatedAt = System.currentTimeMillis(), isDirty = true),
        )
    }

    suspend fun updateStatus(logId: String, status: String, timestampMillis: Long) {
        val log = workoutDao.findLogById(logId) ?: return
        workoutDao.upsertLog(
            log.copy(
                status = status,
                startedAt = if (status == WorkoutStatus.IN_PROGRESS.name) {
                    log.startedAt ?: timestampMillis
                } else {
                    log.startedAt
                },
                endedAt = if (status == WorkoutStatus.COMPLETED.name) timestampMillis else log.endedAt,
                durationSec = if (status == WorkoutStatus.COMPLETED.name && log.startedAt != null) {
                    ((timestampMillis - log.startedAt) / 1000).toInt()
                } else {
                    log.durationSec
                },
                updatedAt = timestampMillis,
                isDirty = true,
            ),
        )
    }

    suspend fun deleteLog(logId: String) {
        val log = workoutDao.findLogById(logId) ?: return
        // 하위 종목·세트는 FK CASCADE 로 함께 삭제된다.
        // 서버 전파를 위해 tombstone 을 남긴다. (FN-WRK-004 / REQ-DAT-002)
        workoutDao.deleteLog(log)
    }

    // ── 종목 ──────────────────────────────────────────────

    /**
     * 일지에 종목을 추가하고 **직전 기록으로 세트를 프리필**한다. (FN-WRK-007/014)
     *
     * 프리필이 있으면 대부분의 세트 입력이 0~1터치로 끝난다. (U3)
     */
    suspend fun addExercises(logId: String, exerciseIds: List<String>): List<String> {
        val now = System.currentTimeMillis()
        val startOrder = workoutDao.maxOrderNo(logId) + 1
        val createdIds = mutableListOf<String>()

        exerciseIds.forEachIndexed { index, exerciseId ->
            val logExerciseId = UUID.randomUUID().toString()
            workoutDao.upsertLogExercise(
                WorkoutLogExerciseEntity(
                    id = logExerciseId,
                    workoutLogId = logId,
                    exerciseId = exerciseId,
                    orderNo = startOrder + index,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            workoutDao.upsertSets(prefillSets(logExerciseId, exerciseId, logId, now))
            createdIds += logExerciseId
        }
        return createdIds
    }

    /**
     * 프리필 세트 생성. (FN-WRK-014 / 07_핵심로직설계서 §5)
     *
     * 우선순위 2단계: 직전 수행 기록을 복사한다. 값만 복사하고 **수행 이력은 복사하지 않는다** —
     * 완료 체크·메모·RPE 는 초기화한다.
     * (우선순위 1단계인 루틴 목표값은 루틴 실행 기능과 함께 후속 구현한다.)
     */
    private suspend fun prefillSets(
        logExerciseId: String,
        exerciseId: String,
        logId: String,
        now: Long,
    ): List<WorkoutSetEntity> {
        val previous = workoutDao.findLastPerformedSets(
            userId = ExerciseLocalSource.LOCAL_USER_ID,
            exerciseId = exerciseId,
            excludeLogId = logId,
        )
        if (previous.isEmpty()) {
            return listOf(newSet(logExerciseId, setNo = 1, now = now))
        }
        return previous.mapIndexed { index, source ->
            newSet(logExerciseId, setNo = index + 1, now = now).copy(
                setType = source.setType,
                weightKg = source.weightKg,
                reps = source.reps,
                durationSec = source.durationSec,
                distanceKm = source.distanceKm,
            )
        }
    }

    private fun newSet(logExerciseId: String, setNo: Int, now: Long) = WorkoutSetEntity(
        id = UUID.randomUUID().toString(),
        logExerciseId = logExerciseId,
        setNo = setNo,
        setType = SetType.NORMAL.name,
        createdAt = now,
        updatedAt = now,
    )

    suspend fun removeLogExercise(logExerciseId: String) {
        workoutDao.findLogExerciseById(logExerciseId)?.let { workoutDao.deleteLogExercise(it) }
    }

    suspend fun reorderExercises(orderedIds: List<String>) {
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, id ->
            workoutDao.updateExerciseOrder(id, index + 1, now)
        }
    }

    suspend fun updateExerciseMemo(logExerciseId: String, memo: String) {
        val entity = workoutDao.findLogExerciseById(logExerciseId) ?: return
        workoutDao.upsertLogExercise(
            entity.copy(memo = memo, updatedAt = System.currentTimeMillis(), isDirty = true),
        )
    }

    // ── 세트 ──────────────────────────────────────────────

    fun observeSets(logExerciseId: String): Flow<List<WorkoutSet>> =
        workoutDao.observeSets(logExerciseId).map { list -> list.map { it.toModel() } }

    suspend fun upsertSet(set: WorkoutSet) {
        val existing = workoutDao.findSetById(set.id)
        val now = System.currentTimeMillis()
        workoutDao.upsertSet(
            WorkoutSetEntity(
                id = set.id.ifBlank { UUID.randomUUID().toString() },
                logExerciseId = set.logExerciseId,
                setNo = set.setNo,
                setType = set.setType.name,
                weightKg = set.weightKg,
                reps = set.reps,
                durationSec = set.durationSec,
                distanceKm = set.distanceKm,
                avgHeartRate = set.avgHeartRate,
                caloriesKcal = set.caloriesKcal,
                rpe = set.rpe,
                rir = set.rir,
                isCompleted = set.isCompleted,
                completedAt = set.completedAtEpochMillis,
                memo = set.memo,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addSet(logExerciseId: String): String {
        val now = System.currentTimeMillis()
        val existing = workoutDao.findSetsOf(logExerciseId)
        val last = existing.lastOrNull()
        val id = UUID.randomUUID().toString()
        workoutDao.upsertSet(
            WorkoutSetEntity(
                id = id,
                logExerciseId = logExerciseId,
                setNo = (last?.setNo ?: 0) + 1,
                // 직전 세트 값을 그대로 복제한다 (FN-WRK-008/015)
                setType = last?.setType ?: SetType.NORMAL.name,
                weightKg = last?.weightKg,
                reps = last?.reps,
                durationSec = last?.durationSec,
                distanceKm = last?.distanceKm,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun deleteSet(setId: String) {
        val set = workoutDao.findSetById(setId) ?: return
        workoutDao.deleteSet(set)
        // 삭제로 생긴 번호 공백을 메운다 (FN-WRK-016)
        workoutDao.shiftSetNumbersAfter(set.logExerciseId, set.setNo, System.currentTimeMillis())
    }

    suspend fun toggleSetComplete(setId: String, isCompleted: Boolean, timestampMillis: Long) {
        val set = workoutDao.findSetById(setId) ?: return
        workoutDao.upsertSet(
            set.copy(
                isCompleted = isCompleted,
                completedAt = if (isCompleted) timestampMillis else null,
                updatedAt = timestampMillis,
                isDirty = true,
            ),
        )
    }

    // ── 집계 ──────────────────────────────────────────────

    /**
     * 세션 요약 집계.
     *
     * 볼륨·횟수 합산은 SQL 에서 수행한다. 세트 전체를 메모리로 로딩해 합산하는 구현은
     * 3년치 기록에서 성능 기준을 만족하지 못한다. (REQ-NFR-001)
     */
    suspend fun calcSummary(logId: String): SessionSummary {
        val agg = workoutDao.aggregateSession(logId)
        val completed = workoutDao.countCompletedSets(logId)
        val log = workoutDao.findLogById(logId)
        return SessionSummary(
            totalVolumeKg = agg.totalVolumeKg,
            totalSetCount = agg.totalSetCount,
            workingSetCount = workoutDao.countWorkingSets(logId),
            totalReps = agg.totalReps,
            durationSec = log?.durationSec,
            completedSetCount = completed,
        )
    }

    suspend fun refreshSummaryCache(logId: String) {
        val log = workoutDao.findLogById(logId) ?: return
        val agg = workoutDao.aggregateSession(logId)
        workoutDao.upsertLog(
            log.copy(
                totalVolumeKg = agg.totalVolumeKg,
                totalSetCount = agg.totalSetCount,
                totalReps = agg.totalReps,
                updatedAt = System.currentTimeMillis(),
                isDirty = true,
            ),
        )
    }

    // ── 매핑 ─────────────────────────────────────────────

    private fun com.sevenbits.myfit.core.database.dao.WorkoutLogWithDetail.toModel() = WorkoutLog(
        id = log.id,
        workoutDate = log.workoutDate,
        sessionNo = log.sessionNo,
        sessionName = log.sessionName,
        status = enumOrNull<WorkoutStatus>(log.status) ?: WorkoutStatus.PLANNED,
        routineId = log.routineId,
        routineName = log.routineNameSnapshot,
        startedAtEpochMillis = log.startedAt,
        endedAtEpochMillis = log.endedAt,
        memo = log.memo,
        conditionSleep = log.conditionSleep,
        conditionOverall = log.conditionOverall,
        conditionSoreness = log.conditionSoreness,
        exercises = exercises.sortedBy { it.logExercise.orderNo }.map { it.toModel() },
    )

    private fun LogExerciseWithSets.toModel() = WorkoutLogExercise(
        id = logExercise.id,
        workoutLogId = logExercise.workoutLogId,
        exerciseId = logExercise.exerciseId,
        exerciseName = exercise.name,
        recordType = enumOrNull<RecordType>(exercise.recordType) ?: RecordType.WEIGHT_REPS,
        orderNo = logExercise.orderNo,
        supersetGroup = logExercise.supersetGroup,
        memo = logExercise.memo,
        defaultRestSec = exercise.defaultRestSec,
        sets = sets.sortedBy { it.setNo }.map { it.toModel() },
    )

    private fun WorkoutSetEntity.toModel() = WorkoutSet(
        id = id,
        logExerciseId = logExerciseId,
        setNo = setNo,
        setType = enumOrNull<SetType>(setType) ?: SetType.NORMAL,
        weightKg = weightKg,
        reps = reps,
        durationSec = durationSec,
        distanceKm = distanceKm,
        avgHeartRate = avgHeartRate,
        caloriesKcal = caloriesKcal,
        rpe = rpe,
        rir = rir,
        isCompleted = isCompleted,
        completedAtEpochMillis = completedAt,
        memo = memo,
    )

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }
}
