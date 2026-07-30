package com.sevenbits.myfit.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import org.junit.Test

/** 07_핵심로직설계서 §2 */
class VolumeCalculatorTest {

    private fun set(
        weight: Double? = null,
        reps: Int? = null,
        type: SetType = SetType.NORMAL,
        completed: Boolean = false,
    ) = WorkoutSet(weightKg = weight, reps = reps, setType = type, isCompleted = completed)

    @Test
    fun `세트 볼륨은 중량 곱하기 횟수다`() {
        assertThat(VolumeCalculator.setVolume(set(80.0, 8))).isWithin(0.001).of(640.0)
    }

    @Test
    fun `웜업 세트는 볼륨에서 제외된다`() {
        val warmup = set(40.0, 12, SetType.WARMUP)
        assertThat(VolumeCalculator.setVolume(warmup)).isEqualTo(0.0)
    }

    @Test
    fun `드랍세트와 실패세트는 볼륨에 포함된다`() {
        assertThat(VolumeCalculator.setVolume(set(60.0, 5, SetType.DROP)))
            .isWithin(0.001).of(300.0)
        assertThat(VolumeCalculator.setVolume(set(60.0, 3, SetType.FAILURE)))
            .isWithin(0.001).of(180.0)
    }

    @Test
    fun `중량이 없는 종목은 볼륨 0`() {
        assertThat(VolumeCalculator.setVolume(set(weight = null, reps = 20))).isEqualTo(0.0)
    }

    @Test
    fun `총 볼륨은 웜업을 제외하고 합산한다`() {
        val sets = listOf(
            set(40.0, 12, SetType.WARMUP), // 제외
            set(60.0, 10),                 // 600
            set(80.0, 8),                  // 640
            set(80.0, 8),                  // 640
        )
        assertThat(VolumeCalculator.totalVolume(sets)).isWithin(0.001).of(1880.0)
    }

    @Test
    fun `빈 목록의 총 볼륨은 0`() {
        assertThat(VolumeCalculator.totalVolume(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `완료 볼륨은 체크된 세트만 합산한다`() {
        val sets = listOf(
            set(80.0, 8, completed = true),  // 640
            set(80.0, 8, completed = false), // 미완료
        )
        assertThat(VolumeCalculator.completedVolume(sets)).isWithin(0.001).of(640.0)
    }

    @Test
    fun `총 횟수와 실작업 세트 수는 웜업을 제외한다`() {
        val sets = listOf(
            set(40.0, 12, SetType.WARMUP),
            set(60.0, 10),
            set(80.0, 8),
        )
        assertThat(VolumeCalculator.totalReps(sets)).isEqualTo(18)
        assertThat(VolumeCalculator.workingSetCount(sets)).isEqualTo(2)
    }
}
