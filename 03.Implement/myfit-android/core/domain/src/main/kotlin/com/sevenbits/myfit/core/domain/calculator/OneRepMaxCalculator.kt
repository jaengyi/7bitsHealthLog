package com.sevenbits.myfit.core.domain.calculator

import com.sevenbits.myfit.core.domain.model.Confidence
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import kotlin.math.pow
import kotlin.math.round

/**
 * 추정 1RM 산출. (FN-RPT-001 ~ 003 / 07_핵심로직설계서 §1)
 *
 * 순수 함수만 노출한다 — 단위 테스트 커버리지 100% 대상.
 */
object OneRepMaxCalculator {

    /** Brzycki 공식이 정의되지 않는 반복 횟수 하한 */
    private const val BRZYCKI_UNDEFINED_REPS = 37

    /** 추정 신뢰도가 떨어지기 시작하는 반복 횟수 */
    private const val LOW_CONFIDENCE_REPS = 12
    private const val HIGH_CONFIDENCE_REPS = 5

    /**
     * @return 추정 1RM(kg). 입력이 유효하지 않거나 공식이 정의되지 않으면 null.
     *
     * `reps == 1` 은 그 자체가 실측 1RM 이므로 공식 보정을 적용하지 않는다.
     * (Epley 에 r=1 을 대입하면 실측값보다 3.3% 높게 나온다)
     */
    fun estimate(weightKg: Double, reps: Int, formula: OneRmFormula): Double? {
        if (weightKg <= 0.0 || reps <= 0) return null
        if (reps == 1) return round1(weightKg)

        val raw = when (formula) {
            OneRmFormula.EPLEY -> weightKg * (1 + reps / 30.0)
            OneRmFormula.BRZYCKI ->
                if (reps >= BRZYCKI_UNDEFINED_REPS) return null
                else weightKg * 36.0 / (37.0 - reps)
            OneRmFormula.LOMBARDI -> weightKg * reps.toDouble().pow(0.10)
        }
        return round1(raw)
    }

    /** 반복 횟수 기반 추정 신뢰도 (FN-RPT-003) */
    fun confidence(reps: Int): Confidence = when {
        reps <= HIGH_CONFIDENCE_REPS -> Confidence.HIGH
        reps <= LOW_CONFIDENCE_REPS -> Confidence.NORMAL
        else -> Confidence.LOW
    }

    /** 대표 1RM 산출에 사용할 수 있는 세트인지 판정 (07 §1.2) */
    fun isEligibleForRepresentative(reps: Int): Boolean = reps in 1..LOW_CONFIDENCE_REPS

    internal fun round1(value: Double): Double = round(value * 10.0) / 10.0
}
