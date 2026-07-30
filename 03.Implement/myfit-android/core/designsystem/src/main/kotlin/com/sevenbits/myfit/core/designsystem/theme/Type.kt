package com.sevenbits.myfit.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 타이포그래피. (06_화면설계서 §1.3)
 *
 * 전부 sp 를 사용한다. 시스템 글꼴 130% 확대에서도 잘림이 없어야 한다. (REQ-NFR-005)
 */
internal val MyFitTypography = Typography(
    // 타이머 잔여시간, 세션 총 볼륨
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    // 화면 제목
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    // 종목명, 세트 수치 — 팔 길이 거리에서 읽혀야 한다 (U6)
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 12.sp),
)
