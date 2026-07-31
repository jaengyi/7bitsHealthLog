package com.sevenbits.myfit.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 07_핵심로직설계서 §7 검증.
 *
 * 타이머는 Foreground Service 로 동작해 실기기 없이는 전체 검증이 어렵다.
 * 시간 계산만이라도 순수 함수로 분리해 여기서 고정한다.
 */
class RestTimerCalculatorTest {

    @Test
    fun `경과한 만큼 잔여 시간이 줄어든다`() {
        val remaining = RestTimerCalculator.remainingMillis(
            targetMillis = 90_000,
            startElapsed = 1_000,
            accumulatedMillis = 0,
            nowElapsed = 31_000, // 30초 경과
            isPaused = false,
        )

        assertThat(remaining).isEqualTo(60_000)
    }

    @Test
    fun `일시정지 중에는 시간이 흐르지 않는다`() {
        val remaining = RestTimerCalculator.remainingMillis(
            targetMillis = 90_000,
            startElapsed = 1_000,
            accumulatedMillis = 30_000,
            nowElapsed = 999_999, // 아무리 시간이 지나도
            isPaused = true,
        )

        assertThat(remaining).isEqualTo(60_000)
    }

    @Test
    fun `재개하면 일시정지 이전 경과가 이어진다`() {
        // 30초 진행 후 일시정지
        val accumulated = RestTimerCalculator.accumulateOnPause(
            accumulatedMillis = 0,
            startElapsed = 1_000,
            nowElapsed = 31_000,
        )
        assertThat(accumulated).isEqualTo(30_000)

        // 한참 뒤 재개해서 10초 더 진행
        val remaining = RestTimerCalculator.remainingMillis(
            targetMillis = 90_000,
            startElapsed = 500_000,
            accumulatedMillis = accumulated,
            nowElapsed = 510_000,
            isPaused = false,
        )

        assertThat(remaining).isEqualTo(50_000)
    }

    @Test
    fun `잔여 시간은 0 미만으로 내려가지 않는다`() {
        val remaining = RestTimerCalculator.remainingMillis(
            targetMillis = 10_000,
            startElapsed = 0,
            accumulatedMillis = 0,
            nowElapsed = 999_999,
            isPaused = false,
        )

        assertThat(remaining).isEqualTo(0)
    }

    @Test
    fun `시스템 시각이 뒤로 흘러도 음수 경과로 처리하지 않는다`() {
        // elapsedRealtime 은 단조 증가하지만, 방어적으로 확인한다
        val remaining = RestTimerCalculator.remainingMillis(
            targetMillis = 90_000,
            startElapsed = 100_000,
            accumulatedMillis = 0,
            nowElapsed = 50_000,
            isPaused = false,
        )

        assertThat(remaining).isEqualTo(90_000)
    }

    @Test
    fun `플러스 15초로 목표를 늘린다`() {
        assertThat(RestTimerCalculator.adjustTarget(90_000, 15)).isEqualTo(105_000)
    }

    @Test
    fun `마이너스 15초로 목표를 줄인다`() {
        assertThat(RestTimerCalculator.adjustTarget(90_000, -15)).isEqualTo(75_000)
    }

    @Test
    fun `목표는 0 미만으로 내려가지 않는다`() {
        assertThat(RestTimerCalculator.adjustTarget(10_000, -15)).isEqualTo(0)
    }

    @Test
    fun `잔여 시간을 분초 형식으로 표시한다`() {
        assertThat(RestTimerCalculator.formatRemaining(83_000)).isEqualTo("01:23")
        assertThat(RestTimerCalculator.formatRemaining(90_000)).isEqualTo("01:30")
        assertThat(RestTimerCalculator.formatRemaining(5_000)).isEqualTo("00:05")
        assertThat(RestTimerCalculator.formatRemaining(0)).isEqualTo("00:00")
    }

    @Test
    fun `1초 미만 잔여는 1초로 올려 표시한다`() {
        // 999ms 가 00:00 으로 보이면 아직 안 끝났는데 끝난 것처럼 읽힌다
        assertThat(RestTimerCalculator.formatRemaining(1)).isEqualTo("00:01")
        assertThat(RestTimerCalculator.formatRemaining(999)).isEqualTo("00:01")
    }
}
