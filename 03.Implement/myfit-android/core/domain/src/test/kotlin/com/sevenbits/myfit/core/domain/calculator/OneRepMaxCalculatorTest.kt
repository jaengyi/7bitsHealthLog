package com.sevenbits.myfit.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.domain.model.Confidence
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import org.junit.Test

/** 07_핵심로직설계서 §1.3 검증 케이스 */
class OneRepMaxCalculatorTest {

    @Test
    fun `1회 수행은 실측값을 그대로 반환한다`() {
        // Epley 공식을 적용하면 103.3 이 되지만, 1회는 그 자체가 1RM 이다.
        assertThat(OneRepMaxCalculator.estimate(100.0, 1, OneRmFormula.EPLEY))
            .isWithin(0.05).of(100.0)
    }

    @Test
    fun `Epley 공식 - 100kg 10회`() {
        assertThat(OneRepMaxCalculator.estimate(100.0, 10, OneRmFormula.EPLEY))
            .isWithin(0.05).of(133.3)
    }

    @Test
    fun `Brzycki 공식 - 100kg 10회`() {
        assertThat(OneRepMaxCalculator.estimate(100.0, 10, OneRmFormula.BRZYCKI))
            .isWithin(0.05).of(133.3)
    }

    @Test
    fun `Lombardi 공식 - 100kg 10회`() {
        assertThat(OneRepMaxCalculator.estimate(100.0, 10, OneRmFormula.LOMBARDI))
            .isWithin(0.05).of(125.9)
    }

    @Test
    fun `Epley 공식 - 80kg 8회`() {
        assertThat(OneRepMaxCalculator.estimate(80.0, 8, OneRmFormula.EPLEY))
            .isWithin(0.05).of(101.3)
    }

    @Test
    fun `Brzycki 공식 - 80kg 8회`() {
        assertThat(OneRepMaxCalculator.estimate(80.0, 8, OneRmFormula.BRZYCKI))
            .isWithin(0.05).of(99.3)
    }

    @Test
    fun `Lombardi 공식 - 80kg 8회`() {
        assertThat(OneRepMaxCalculator.estimate(80.0, 8, OneRmFormula.LOMBARDI))
            .isWithin(0.05).of(98.5)
    }

    @Test
    fun `Brzycki 는 37회 이상에서 정의되지 않는다`() {
        assertThat(OneRepMaxCalculator.estimate(100.0, 37, OneRmFormula.BRZYCKI)).isNull()
        assertThat(OneRepMaxCalculator.estimate(100.0, 36, OneRmFormula.BRZYCKI)).isNotNull()
    }

    @Test
    fun `중량이 0 이하이면 null`() {
        assertThat(OneRepMaxCalculator.estimate(0.0, 10, OneRmFormula.EPLEY)).isNull()
        assertThat(OneRepMaxCalculator.estimate(-10.0, 10, OneRmFormula.EPLEY)).isNull()
    }

    @Test
    fun `횟수가 0 이하이면 null`() {
        assertThat(OneRepMaxCalculator.estimate(100.0, 0, OneRmFormula.EPLEY)).isNull()
        assertThat(OneRepMaxCalculator.estimate(100.0, -1, OneRmFormula.EPLEY)).isNull()
    }

    @Test
    fun `신뢰도는 반복 횟수 구간으로 판정한다`() {
        assertThat(OneRepMaxCalculator.confidence(1)).isEqualTo(Confidence.HIGH)
        assertThat(OneRepMaxCalculator.confidence(5)).isEqualTo(Confidence.HIGH)
        assertThat(OneRepMaxCalculator.confidence(6)).isEqualTo(Confidence.NORMAL)
        assertThat(OneRepMaxCalculator.confidence(12)).isEqualTo(Confidence.NORMAL)
        assertThat(OneRepMaxCalculator.confidence(13)).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `대표 1RM 산출에는 12회 이하 세트만 사용한다`() {
        assertThat(OneRepMaxCalculator.isEligibleForRepresentative(12)).isTrue()
        assertThat(OneRepMaxCalculator.isEligibleForRepresentative(13)).isFalse()
        assertThat(OneRepMaxCalculator.isEligibleForRepresentative(0)).isFalse()
    }
}
