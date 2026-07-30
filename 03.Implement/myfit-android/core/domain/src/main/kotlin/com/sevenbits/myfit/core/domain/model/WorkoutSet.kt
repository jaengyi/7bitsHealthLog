package com.sevenbits.myfit.core.domain.model

/**
 * 세트 단위 실제 기록.
 *
 * 기록 유형 5종을 하나의 모델로 수용하기 위해 유형별 필드를 모두 nullable 로 둔다.
 * (REQ-NFR-006 / 04_데이터베이스설계서 §3.10)
 *
 * 중량은 **항상 kg**, 거리는 **항상 km** 로 보관한다. 단위 변환은 표시 시점에만 수행한다. (P7)
 */
data class WorkoutSet(
    val id: String = "",
    val logExerciseId: String = "",
    val exerciseId: String = "",
    val setNo: Int = 1,
    val setType: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceKm: Double? = null,
    val avgHeartRate: Int? = null,
    val caloriesKcal: Double? = null,
    val rpe: Double? = null,
    val rir: Int? = null,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    val memo: String? = null,
) {
    /** 집계 대상 세트인가 (웜업 제외) */
    val isWorkingSet: Boolean get() = setType != SetType.WARMUP
}
