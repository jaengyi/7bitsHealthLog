package com.sevenbits.myfit.core.domain.model

/**
 * 휴식 타이머 상태. (FN-TOL-001 ~ 005 / 07_핵심로직설계서 §7)
 */
data class RestTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalSec: Int = 0,
    val remainingMillis: Long = 0L,
    /** 타이머를 시작시킨 세트 — 수행 화면에서 어느 세트의 휴식인지 표시한다 */
    val sourceSetId: String? = null,
) {
    val remainingSec: Int get() = ((remainingMillis + MILLIS_ROUND_UP) / 1000L).toInt()

    val progress: Float
        get() = if (totalSec <= 0) 0f else (remainingMillis / (totalSec * 1000f)).coerceIn(0f, 1f)

    val isFinished: Boolean get() = isRunning && remainingMillis <= 0L

    private companion object {
        /** 999ms 를 1초로 표시하기 위한 올림 보정 */
        const val MILLIS_ROUND_UP = 999L
    }
}
