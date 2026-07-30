package com.sevenbits.myfit.core.domain.model

/**
 * 운동일지 — 일자·회차 단위 세션. (ENT-008 / 04_데이터베이스설계서 §3.8)
 */
data class WorkoutLog(
    val id: String = "",
    /** ISO-8601 `yyyy-MM-dd` */
    val workoutDate: String,
    /** 1~3 (REQ-WRK-002) */
    val sessionNo: Int = 1,
    val sessionName: String? = null,
    val status: WorkoutStatus = WorkoutStatus.PLANNED,
    val routineId: String? = null,
    /** 루틴이 삭제되어도 이름을 표시하기 위한 스냅샷 (FN-RTN-004) */
    val routineName: String? = null,
    val startedAtEpochMillis: Long? = null,
    val endedAtEpochMillis: Long? = null,
    val memo: String? = null,
    val conditionSleep: Int? = null,
    val conditionOverall: Int? = null,
    val conditionSoreness: Int? = null,
    val exercises: List<WorkoutLogExercise> = emptyList(),
) {
    val isInProgress: Boolean get() = status == WorkoutStatus.IN_PROGRESS
}

/** 일지 내 수행 종목 항목 (ENT-009) */
data class WorkoutLogExercise(
    val id: String = "",
    val workoutLogId: String = "",
    val exerciseId: String,
    val exerciseName: String = "",
    val recordType: RecordType = RecordType.WEIGHT_REPS,
    val orderNo: Int = 0,
    /** 동일 값끼리 슈퍼세트 (FN-WRK-012) */
    val supersetGroup: Int? = null,
    val memo: String? = null,
    val defaultRestSec: Int? = null,
    val sets: List<WorkoutSet> = emptyList(),
)

/**
 * 세션 요약 지표. (FN-WRK-027/028)
 *
 * 웜업 세트는 볼륨·횟수 집계에서 제외한다. 세트 수는 웜업을 포함한 전체다 —
 * "몇 세트 했나"는 실제 수행한 횟수를 의미하기 때문이다.
 */
data class SessionSummary(
    val totalVolumeKg: Double = 0.0,
    val totalSetCount: Int = 0,
    val workingSetCount: Int = 0,
    val totalReps: Int = 0,
    val durationSec: Int? = null,
    val completedSetCount: Int = 0,
    /** 부위 코드 → 비중(%) */
    val bodyPartRatio: Map<String, Double> = emptyMap(),
) {
    val progressRatio: Float
        get() = if (totalSetCount == 0) 0f else completedSetCount.toFloat() / totalSetCount
}
