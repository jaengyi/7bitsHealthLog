package com.sevenbits.myfit.timer

import android.os.SystemClock
import com.sevenbits.myfit.core.domain.calculator.RestTimerCalculator
import com.sevenbits.myfit.core.domain.model.RestTimerState
import com.sevenbits.myfit.core.domain.repository.RestTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 휴식 타이머 엔진. (FN-TOL-001 ~ 005)
 *
 * 잔여 시간은 tick 마다 **기준 시각과의 델타로 재계산**한다.
 * tick 자체는 표시 갱신 트리거일 뿐이며, tick 이 지연되어도 시간이 어긋나지 않는다.
 * (07_핵심로직설계서 §7.1)
 *
 * 프로세스가 살아 있는 동안 상태를 유지해야 하므로 Singleton 이다.
 * 백그라운드·화면잠금에서도 살아 있게 하는 것은 [RestTimerService] 의 책임이다.
 */
@Singleton
class RestTimerEngine @Inject constructor() : RestTimerController {

    private val scope = CoroutineScope(SupervisorJob())

    private val _state = MutableStateFlow(RestTimerState())
    override val state: StateFlow<RestTimerState> = _state.asStateFlow()

    /** 타이머 종료 알림(진동·소리)을 서비스가 받아 처리한다. (FN-TOL-005) */
    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    private var targetMillis = 0L
    private var startElapsed = 0L
    private var accumulatedMillis = 0L
    private var tickJob: Job? = null

    override fun start(seconds: Int, sourceSetId: String?) {
        // 연속 완료 시 이전 타이머가 남으면 잔여 시간이 뒤섞인다
        tickJob?.cancel()

        targetMillis = seconds * 1000L
        startElapsed = SystemClock.elapsedRealtime()
        accumulatedMillis = 0L

        _state.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            totalSec = seconds,
            remainingMillis = targetMillis,
            sourceSetId = sourceSetId,
        )
        startTicking()
    }

    override fun pause() {
        val current = _state.value
        if (!current.isRunning || current.isPaused) return

        accumulatedMillis = RestTimerCalculator.accumulateOnPause(
            accumulatedMillis = accumulatedMillis,
            startElapsed = startElapsed,
            nowElapsed = SystemClock.elapsedRealtime(),
        )
        tickJob?.cancel()
        _state.update { it.copy(isPaused = true) }
    }

    override fun resume() {
        val current = _state.value
        if (!current.isRunning || !current.isPaused) return

        startElapsed = SystemClock.elapsedRealtime()
        _state.update { it.copy(isPaused = false) }
        startTicking()
    }

    override fun skip() {
        tickJob?.cancel()
        _state.value = RestTimerState()
    }

    override fun adjust(deltaSec: Int) {
        if (!_state.value.isRunning) return

        targetMillis = RestTimerCalculator.adjustTarget(targetMillis, deltaSec)
        val remaining = currentRemaining()
        _state.update {
            it.copy(
                // 표시 진행률의 기준이 되므로 총 시간도 함께 갱신한다
                totalSec = (targetMillis / 1000L).toInt(),
                remainingMillis = remaining,
            )
        }
        if (remaining <= 0L) finish()
    }

    private fun startTicking() {
        tickJob = scope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                val remaining = currentRemaining()
                _state.update { it.copy(remainingMillis = remaining) }
                if (remaining <= 0L) {
                    finish()
                    break
                }
            }
        }
    }

    private fun currentRemaining(): Long = RestTimerCalculator.remainingMillis(
        targetMillis = targetMillis,
        startElapsed = startElapsed,
        accumulatedMillis = accumulatedMillis,
        nowElapsed = SystemClock.elapsedRealtime(),
        isPaused = _state.value.isPaused,
    )

    private fun finish() {
        tickJob?.cancel()
        _state.value = RestTimerState()
        _finished.tryEmit(Unit)
    }

    private companion object {
        /**
         * 표시 갱신 주기.
         *
         * 1초로 잡으면 표시가 한 박자 늦게 바뀌어 보인다. 200ms 는 체감상 즉각적이고
         * 전력 소모도 무시할 수준이다.
         */
        const val TICK_INTERVAL_MS = 200L
    }
}
