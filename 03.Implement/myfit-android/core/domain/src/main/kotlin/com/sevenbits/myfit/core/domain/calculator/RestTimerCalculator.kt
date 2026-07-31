package com.sevenbits.myfit.core.domain.calculator

/**
 * 휴식 타이머 시간 계산. (FN-TOL-001/003 / 07_핵심로직설계서 §7)
 *
 * **코루틴 delay 를 누적해 잔여 시간을 세지 않는다.** 백그라운드·Doze 상태에서
 * delay 는 지연되어 실제 경과 시간과 어긋난다(드리프트). 대신 기준 시각과의
 * **델타로 매번 재계산**한다. UI tick 은 표시 갱신에만 쓴다.
 *
 * 시각 인자는 `SystemClock.elapsedRealtime()` 값을 받는다 — 사용자의 시계 변경이나
 * 타임존 변경에 영향받지 않는다.
 */
object RestTimerCalculator {

    /**
     * 잔여 시간(ms).
     *
     * @param targetMillis 목표 휴식 시간
     * @param startElapsed 마지막 시작·재개 시점의 elapsedRealtime
     * @param accumulatedMillis 일시정지 이전까지 누적된 경과 시간
     * @param nowElapsed 현재 elapsedRealtime
     * @param isPaused 일시정지 중이면 현재 구간 경과를 세지 않는다
     */
    fun remainingMillis(
        targetMillis: Long,
        startElapsed: Long,
        accumulatedMillis: Long,
        nowElapsed: Long,
        isPaused: Boolean,
    ): Long {
        val currentSegment = if (isPaused) 0L else (nowElapsed - startElapsed).coerceAtLeast(0L)
        val elapsed = accumulatedMillis + currentSegment
        return (targetMillis - elapsed).coerceAtLeast(0L)
    }

    /**
     * 일시정지 시점의 누적 경과 시간.
     *
     * 재개할 때 기준 시각을 새로 잡으므로, 그 전까지의 경과를 적립해 두어야 한다.
     */
    fun accumulateOnPause(
        accumulatedMillis: Long,
        startElapsed: Long,
        nowElapsed: Long,
    ): Long = accumulatedMillis + (nowElapsed - startElapsed).coerceAtLeast(0L)

    /**
     * ±15초 조정. (FN-TOL-003)
     *
     * 목표 시간을 늘리거나 줄인다. 0 미만으로는 내려가지 않는다 —
     * 음수 목표는 즉시 종료와 구분되지 않아 사용자가 의도를 잃는다.
     */
    fun adjustTarget(targetMillis: Long, deltaSec: Int): Long =
        (targetMillis + deltaSec * 1000L).coerceAtLeast(0L)

    /** `01:23` 형태로 표시한다. 1시간을 넘기는 휴식은 상정하지 않는다. */
    fun formatRemaining(remainingMillis: Long): String {
        val totalSec = ((remainingMillis + 999L) / 1000L).toInt().coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
