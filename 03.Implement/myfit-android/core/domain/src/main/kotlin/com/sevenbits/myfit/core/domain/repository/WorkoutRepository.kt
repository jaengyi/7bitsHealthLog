package com.sevenbits.myfit.core.domain.repository

import com.sevenbits.myfit.core.domain.model.DailyWorkoutSummary
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

/**
 * 운동기록 저장소 계약. (03_모듈설계서 §1.3 R-2)
 */
interface WorkoutRepository {

    /** 일지 상세를 관찰한다. 하위 종목·세트가 함께 로드된다. */
    fun observeLog(logId: String): Flow<WorkoutLog?>

    /**
     * 해당 일자·회차의 일지를 가져오되 없으면 만든다. (FN-WRK-001)
     *
     * "오늘 기록" 진입은 가장 빈번한 동선이므로 조회/생성을 한 번에 처리한다.
     */
    suspend fun getOrCreateLog(date: String, sessionNo: Int = 1): String

    suspend fun findLogId(date: String, sessionNo: Int): String?

    /** 진행 중 세션 복원 (FN-WRK-023) */
    suspend fun findInProgressLogId(): String?

    suspend fun countSessionsOn(date: String): Int

    suspend fun updateMemo(logId: String, memo: String)

    suspend fun updateStatus(logId: String, status: String, timestampMillis: Long)

    suspend fun deleteLog(logId: String)

    // ── 종목 ──────────────────────────────────────────────

    /**
     * 일지에 종목을 추가한다. 추가 시 **직전 기록으로 세트를 프리필**한다. (FN-WRK-007/014)
     *
     * @return 생성된 일지-종목 id 목록
     */
    suspend fun addExercises(logId: String, exerciseIds: List<String>): List<String>

    suspend fun removeLogExercise(logExerciseId: String)

    suspend fun reorderExercises(logId: String, orderedIds: List<String>)

    suspend fun updateExerciseMemo(logExerciseId: String, memo: String)

    // ── 세트 ──────────────────────────────────────────────

    fun observeSets(logExerciseId: String): Flow<List<WorkoutSet>>

    /** 입력 즉시 영속화한다. 명시적 저장 버튼에 의존하지 않는다. (P5 / FN-WRK-031) */
    suspend fun upsertSet(set: WorkoutSet)

    /** 직전 세트 값을 복사해 새 세트를 추가한다. (FN-WRK-008/015) */
    suspend fun addSet(logExerciseId: String): String

    /** 삭제 후 세트 순번을 재정렬한다. (FN-WRK-016) */
    suspend fun deleteSet(setId: String)

    suspend fun toggleSetComplete(setId: String, isCompleted: Boolean, timestampMillis: Long)

    // ── 집계 ──────────────────────────────────────────────

    suspend fun calcSummary(logId: String): SessionSummary

    // ── 캘린더 (FN-CAL-001/002) ──────────────────────────

    /** 기간 내 일자별 요약. 월간 캘린더 마커의 입력이다. */
    fun observeDailySummaries(from: String, to: String): Flow<List<DailyWorkoutSummary>>

    /** 스트릭 산출 입력 — 수행 완료 일자 (FN-CAL-008) */
    suspend fun findCompletedDatesSince(from: String): List<String>

    /** 집계 캐시 갱신. 세트 변경과 같은 트랜잭션 흐름에서 호출한다. */
    suspend fun refreshSummaryCache(logId: String)
}
