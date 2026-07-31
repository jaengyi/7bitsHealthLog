package com.sevenbits.myfit.core.domain.model

/**
 * 캘린더 마커용 일자별 요약. (ENT-008 집계 / FN-CAL-001/002)
 *
 * 월간 조회는 30여 건을 한 번에 그리므로, 일지 상세를 로딩하지 않고
 * 집계 캐시(`workout_log.total_*`)만 읽는다. (REQ-NFR-001)
 */
data class DailyWorkoutSummary(
    /** `yyyy-MM-dd` */
    val date: String,
    val logCount: Int,
    val totalVolumeKg: Double,
)
