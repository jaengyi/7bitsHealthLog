package com.sevenbits.myfit.core.domain.repository

import com.sevenbits.myfit.core.domain.model.Routine
import kotlinx.coroutines.flow.Flow

/**
 * 루틴 저장소 계약. (REQ-RTN-001 ~ 003)
 */
interface RoutineRepository {

    /** 최근 수행일·사용빈도순 (FN-RTN-002) */
    fun observeRoutines(): Flow<List<Routine>>

    suspend fun findRoutine(routineId: String): Routine?

    suspend fun upsertRoutine(routine: Routine): String

    /**
     * 루틴 삭제. **논리 삭제**한다 — 과거 일지가 참조하는 루틴명 스냅샷을 보존하기 위해서다.
     * (FN-RTN-004)
     */
    suspend fun deactivateRoutine(routineId: String)

    /**
     * 루틴을 해당 일자의 일지로 인스턴스화한다. (FN-RTN-007 / REQ-RTN-003)
     *
     * 루틴 정의를 일지-종목-세트로 **복제**하며, 이후 일지 수정은 루틴에 영향을 주지 않는다.
     *
     * @return 생성되거나 갱신된 일지 id
     */
    suspend fun instantiateToLog(routineId: String, date: String, sessionNo: Int = 1): String

    /** 수행한 일지 구성을 새 루틴으로 저장한다. (FN-RTN-008) */
    suspend fun saveLogAsRoutine(logId: String, routineName: String): String

    /** 순환 배정의 다음 루틴 제안 (FN-RTN-011) */
    suspend fun suggestNextRoutine(): Routine?
}
