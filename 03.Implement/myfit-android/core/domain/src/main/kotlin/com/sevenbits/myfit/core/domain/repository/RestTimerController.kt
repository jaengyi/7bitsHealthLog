package com.sevenbits.myfit.core.domain.repository

import com.sevenbits.myfit.core.domain.model.RestTimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * 휴식 타이머 제어 계약. (FN-TOL-001 ~ 005)
 *
 * 구현은 Foreground Service 와 연동되지만, feature 모듈은 이 인터페이스만 안다.
 * 화면은 상태를 관찰하고 명령을 보낼 뿐 서비스 생명주기를 알지 못한다.
 */
interface RestTimerController {

    val state: StateFlow<RestTimerState>

    /**
     * 휴식 타이머를 시작한다. 이미 실행 중이면 **기존 타이머를 취소하고 새로 시작**한다 —
     * 세트를 연속으로 완료할 때 이전 타이머가 남아 있으면 잔여 시간이 뒤섞인다.
     */
    fun start(seconds: Int, sourceSetId: String? = null)

    fun pause()

    fun resume()

    /** 남은 시간을 버리고 즉시 종료한다. (FN-TOL-003) */
    fun skip()

    /** ±15초 조정 (FN-TOL-003) */
    fun adjust(deltaSec: Int)
}
