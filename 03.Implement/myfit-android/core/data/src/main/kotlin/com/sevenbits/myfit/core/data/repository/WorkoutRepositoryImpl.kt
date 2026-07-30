package com.sevenbits.myfit.core.data.repository

import com.sevenbits.myfit.core.database.WorkoutLocalSource
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 운동기록 저장소 구현.
 *
 * 세트 변경 후에는 집계 캐시를 함께 갱신한다 — 캘린더·리포트가 이 값을 읽는다.
 * (04_데이터베이스설계서 §3.8)
 */
@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val local: WorkoutLocalSource,
) : WorkoutRepository {

    override fun observeLog(logId: String): Flow<WorkoutLog?> = local.observeLog(logId)

    override suspend fun getOrCreateLog(date: String, sessionNo: Int): String =
        local.getOrCreateLog(date, sessionNo)

    override suspend fun findLogId(date: String, sessionNo: Int): String? =
        local.findLogId(date, sessionNo)

    override suspend fun findInProgressLogId(): String? = local.findInProgressLogId()

    override suspend fun countSessionsOn(date: String): Int = local.countSessionsOn(date)

    override suspend fun updateMemo(logId: String, memo: String) = local.updateMemo(logId, memo)

    override suspend fun updateStatus(logId: String, status: String, timestampMillis: Long) =
        local.updateStatus(logId, status, timestampMillis)

    override suspend fun deleteLog(logId: String) = local.deleteLog(logId)

    override suspend fun addExercises(logId: String, exerciseIds: List<String>): List<String> {
        val created = local.addExercises(logId, exerciseIds)
        local.refreshSummaryCache(logId)
        return created
    }

    override suspend fun removeLogExercise(logExerciseId: String) =
        local.removeLogExercise(logExerciseId)

    override suspend fun reorderExercises(logId: String, orderedIds: List<String>) =
        local.reorderExercises(orderedIds)

    override suspend fun updateExerciseMemo(logExerciseId: String, memo: String) =
        local.updateExerciseMemo(logExerciseId, memo)

    override fun observeSets(logExerciseId: String): Flow<List<WorkoutSet>> =
        local.observeSets(logExerciseId)

    override suspend fun upsertSet(set: WorkoutSet) = local.upsertSet(set)

    override suspend fun addSet(logExerciseId: String): String = local.addSet(logExerciseId)

    override suspend fun deleteSet(setId: String) = local.deleteSet(setId)

    override suspend fun toggleSetComplete(
        setId: String,
        isCompleted: Boolean,
        timestampMillis: Long,
    ) = local.toggleSetComplete(setId, isCompleted, timestampMillis)

    override suspend fun calcSummary(logId: String): SessionSummary = local.calcSummary(logId)

    override suspend fun refreshSummaryCache(logId: String) = local.refreshSummaryCache(logId)
}
