package com.sevenbits.myfit.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * 치수 토큰. (06_화면설계서 §1.1)
 *
 * Composable 에서 dp 리터럴을 직접 쓰지 않고 이 토큰을 참조한다.
 */
object Dimens {
    /** 모든 탭 가능 요소의 최소 크기 (U2 / REQ-NFR-002) */
    val MinTouchTarget = 48.dp

    /**
     * 세트 입력의 ± 버튼 크기.
     *
     * 땀·장갑 착용 상태의 오조작을 고려해 최소 기준(48dp)보다 크게 잡는다.
     */
    val StepperButton = 56.dp

    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 16.dp
    val SpaceLg = 24.dp
    val SpaceXl = 32.dp

    val CardCorner = 12.dp
    val ScreenPadding = 16.dp

    /** 하단 고정 액션 영역 높이 — 한손 조작 영역(U1) */
    val BottomActionHeight = 72.dp
}
