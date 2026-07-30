package com.sevenbits.myfit.core.domain.calculator

import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutSet

/**
 * 볼륨 산출. (07_핵심로직설계서 §2)
 *
 * 볼륨 = Σ(중량 × 횟수), 웜업 세트 제외.
 * DROP/FAILURE 는 실제 수행한 작업량이므로 포함한다.
 */
object VolumeCalculator {

    /** 세트 1건의 볼륨. 웜업이면 0. */
    fun setVolume(set: WorkoutSet): Double {
        if (set.setType == SetType.WARMUP) return 0.0
        val weight = set.weightKg ?: return 0.0
        val reps = set.reps ?: return 0.0
        return weight * reps
    }

    /** 세트 목록의 총 볼륨 (웜업 제외) */
    fun totalVolume(sets: List<WorkoutSet>): Double = sets.sumOf { setVolume(it) }

    /** 완료된 세트만 합산 — 수행 화면의 실시간 볼륨 표시용 */
    fun completedVolume(sets: List<WorkoutSet>): Double =
        sets.filter { it.isCompleted }.sumOf { setVolume(it) }

    /** 총 횟수 (웜업 제외) */
    fun totalReps(sets: List<WorkoutSet>): Int =
        sets.filter { it.isWorkingSet }.sumOf { it.reps ?: 0 }

    /** 웜업을 제외한 실작업 세트 수 */
    fun workingSetCount(sets: List<WorkoutSet>): Int = sets.count { it.isWorkingSet }
}
