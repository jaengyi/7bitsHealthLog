package com.sevenbits.myfit.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.domain.model.IntensityZone
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import org.junit.Test

/** 07_핵심로직설계서 §3.3 검증 케이스 */
class IntensityCalculatorTest {

    @Test
    fun `강도는 추정 1RM 대비 백분율이다`() {
        assertThat(IntensityCalculator.intensityPct(80.0, 100.0)).isWithin(0.05).of(80.0)
    }

    @Test
    fun `추정 1RM 이 0 이하이면 null`() {
        assertThat(IntensityCalculator.intensityPct(100.0, 0.0)).isNull()
        assertThat(IntensityCalculator.intensityPct(100.0, -1.0)).isNull()
    }

    @Test
    fun `강도 구간 경계값은 상위 구간에 포함된다`() {
        assertThat(IntensityCalculator.zone(100.0)).isEqualTo(IntensityZone.STRENGTH)
        assertThat(IntensityCalculator.zone(85.0)).isEqualTo(IntensityZone.STRENGTH)
        assertThat(IntensityCalculator.zone(84.9)).isEqualTo(IntensityZone.HYPERTROPHY)
        assertThat(IntensityCalculator.zone(65.0)).isEqualTo(IntensityZone.HYPERTROPHY)
        assertThat(IntensityCalculator.zone(64.9)).isEqualTo(IntensityZone.ENDURANCE)
    }

    @Test
    fun `세션 평균 강도는 볼륨 가중평균이다`() {
        // 100kg×1회(강도 100%, 볼륨 100) + 50kg×10회(강도 50%, 볼륨 500)
        // 단순평균이면 75%, 볼륨 가중평균이면 (100*100 + 50*500)/600 = 58.3%
        val sets = listOf(
            WorkoutSet(exerciseId = "e1", weightKg = 100.0, reps = 1),
            WorkoutSet(exerciseId = "e1", weightKg = 50.0, reps = 10),
        )
        val avg = IntensityCalculator.sessionAverageIntensity(sets) { 100.0 }
        assertThat(avg).isNotNull()
        assertThat(avg!!).isWithin(0.1).of(58.3)
    }

    @Test
    fun `세션 평균 강도에서 웜업은 제외된다`() {
        val sets = listOf(
            WorkoutSet(exerciseId = "e1", weightKg = 40.0, reps = 10, setType = SetType.WARMUP),
            WorkoutSet(exerciseId = "e1", weightKg = 80.0, reps = 10),
        )
        assertThat(IntensityCalculator.sessionAverageIntensity(sets) { 100.0 })
            .isWithin(0.05).of(80.0)
    }

    @Test
    fun `추정 1RM 을 알 수 없는 세트는 집계에서 빠진다`() {
        val sets = listOf(WorkoutSet(exerciseId = "unknown", weightKg = 80.0, reps = 10))
        assertThat(IntensityCalculator.sessionAverageIntensity(sets) { null }).isNull()
    }

    @Test
    fun `강도 구간별 세트 분포를 집계한다`() {
        val sets = listOf(
            WorkoutSet(exerciseId = "e1", weightKg = 90.0, reps = 3),  // 90% STRENGTH
            WorkoutSet(exerciseId = "e1", weightKg = 70.0, reps = 10), // 70% HYPERTROPHY
            WorkoutSet(exerciseId = "e1", weightKg = 50.0, reps = 20), // 50% ENDURANCE
            WorkoutSet(exerciseId = "e1", weightKg = 40.0, reps = 15, setType = SetType.WARMUP),
        )
        val dist = IntensityCalculator.zoneDistribution(sets) { 100.0 }
        assertThat(dist[IntensityZone.STRENGTH]).isEqualTo(1)
        assertThat(dist[IntensityZone.HYPERTROPHY]).isEqualTo(1)
        assertThat(dist[IntensityZone.ENDURANCE]).isEqualTo(1)
    }
}
