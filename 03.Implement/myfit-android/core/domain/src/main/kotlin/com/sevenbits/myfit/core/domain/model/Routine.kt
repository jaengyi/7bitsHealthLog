package com.sevenbits.myfit.core.domain.model

/**
 * 루틴 — 반복 수행 종목 묶음 템플릿. (ENT-011 / REQ-RTN-001)
 *
 * **루틴 원본과 실제 일지는 완전히 분리된다.** 루틴을 실행하면 정의가 일지로 복제되고,
 * 이후 일지를 수정해도 루틴 정의는 바뀌지 않는다. (REQ-RTN-003)
 */
data class Routine(
    val id: String = "",
    val name: String,
    val description: String? = null,
    /** 대상 부위 코드 */
    val targetBodyParts: List<String> = emptyList(),
    val isPreset: Boolean = false,
    val useCount: Int = 0,
    /** `yyyy-MM-dd`. 순환 배정의 다음 루틴 판정 기준 (FN-RTN-011) */
    val lastPerformedDate: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
) {
    val exerciseCount: Int get() = exercises.size

    /** 목록 표시용 요약 — "벤치프레스 외 5종목" */
    fun summaryLabel(): String = when {
        exercises.isEmpty() -> "종목 없음"
        exercises.size == 1 -> exercises.first().exerciseName
        else -> "${exercises.first().exerciseName} 외 ${exercises.size - 1}종목"
    }
}

/**
 * 루틴 내 종목과 목표값. (ENT-012 / REQ-RTN-002)
 *
 * [targetWeightKg] 와 [targetPercentOneRm] 은 **택일**이다.
 * 비율이 지정되면 실행 시점의 추정 1RM 으로 환산한다. (FN-RTN-006)
 */
data class RoutineExercise(
    val id: String = "",
    val routineId: String = "",
    val exerciseId: String,
    val exerciseName: String = "",
    val recordType: RecordType = RecordType.WEIGHT_REPS,
    val orderNo: Int = 0,
    val targetSetCount: Int? = null,
    val targetWeightKg: Double? = null,
    /** %1RM. [targetWeightKg] 와 동시에 설정하지 않는다. */
    val targetPercentOneRm: Double? = null,
    val targetReps: Int? = null,
    val restSec: Int? = null,
) {
    val usesPercentTarget: Boolean get() = targetPercentOneRm != null && targetWeightKg == null

    /** 편집 화면 요약 — "4세트 · 80kg · 8회" */
    fun targetLabel(): String = listOfNotNull(
        targetSetCount?.let { "${it}세트" },
        targetWeightKg?.let { "${formatWeight(it)}kg" }
            ?: targetPercentOneRm?.let { "1RM ${it.toInt()}%" },
        targetReps?.let { "${it}회" },
    ).joinToString(" · ").ifBlank { "목표 미설정" }

    private fun formatWeight(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
