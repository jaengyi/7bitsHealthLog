package com.sevenbits.myfit.core.domain.calculator

import com.sevenbits.myfit.core.domain.model.IntensityZone
import com.sevenbits.myfit.core.domain.model.WorkoutSet

/**
 * 운동 강도(%1RM) 산출. (FN-RPT-004 ~ 006 / 07_핵심로직설계서 §3)
 */
object IntensityCalculator {

    private const val STRENGTH_THRESHOLD = 85.0
    private const val HYPERTROPHY_THRESHOLD = 65.0

    /** 세트 강도(%) = 중량 ÷ 종목 추정1RM × 100 */
    fun intensityPct(weightKg: Double, estimatedOneRm: Double): Double? {
        if (estimatedOneRm <= 0.0) return null
        return OneRepMaxCalculator.round1(weightKg / estimatedOneRm * 100.0)
    }

    /** 강도 구간 판정. 경계값(85, 65)은 상위 구간에 포함한다. */
    fun zone(intensityPct: Double): IntensityZone = when {
        intensityPct >= STRENGTH_THRESHOLD -> IntensityZone.STRENGTH
        intensityPct >= HYPERTROPHY_THRESHOLD -> IntensityZone.HYPERTROPHY
        else -> IntensityZone.ENDURANCE
    }

    /**
     * 세션 평균 강도 — **볼륨 가중평균**.
     *
     * 단순 평균은 1회짜리 고중량 세트와 12회짜리 저중량 세트를 동일 취급하므로
     * 세션의 실제 부하 성격이 왜곡된다.
     *
     * @param estimatedOneRmOf 종목 ID → 추정 1RM. 값이 없으면 해당 세트는 집계에서 제외.
     */
    fun sessionAverageIntensity(
        sets: List<WorkoutSet>,
        estimatedOneRmOf: (exerciseId: String) -> Double?,
    ): Double? {
        var weightedSum = 0.0
        var volumeSum = 0.0

        sets.asSequence()
            .filter { it.isWorkingSet }
            .forEach { set ->
                val weight = set.weightKg ?: return@forEach
                val reps = set.reps ?: return@forEach
                val oneRm = estimatedOneRmOf(set.exerciseId) ?: return@forEach
                if (oneRm <= 0.0) return@forEach

                val volume = weight * reps
                weightedSum += (weight / oneRm * 100.0) * volume
                volumeSum += volume
            }

        if (volumeSum == 0.0) return null
        return OneRepMaxCalculator.round1(weightedSum / volumeSum)
    }

    /** 강도 구간별 세트 분포 (FN-RPT-006) */
    fun zoneDistribution(
        sets: List<WorkoutSet>,
        estimatedOneRmOf: (exerciseId: String) -> Double?,
    ): Map<IntensityZone, Int> {
        val counts = mutableMapOf<IntensityZone, Int>()
        sets.asSequence()
            .filter { it.isWorkingSet }
            .forEach { set ->
                val weight = set.weightKg ?: return@forEach
                val oneRm = estimatedOneRmOf(set.exerciseId) ?: return@forEach
                val pct = intensityPct(weight, oneRm) ?: return@forEach
                val zone = zone(pct)
                counts[zone] = (counts[zone] ?: 0) + 1
            }
        return counts
    }
}
