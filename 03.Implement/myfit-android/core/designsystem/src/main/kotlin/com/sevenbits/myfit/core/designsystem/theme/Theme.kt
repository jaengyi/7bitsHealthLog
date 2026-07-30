package com.sevenbits.myfit.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 색상 토큰. (06_화면설계서 §1.2)
 * Composable 에서 Color 리터럴을 직접 쓰지 않고 반드시 이 토큰을 통해 참조한다.
 */
private val Primary = Color(0xFF7DD3FC)
private val PrimaryLight = Color(0xFF0369A1)
private val SurfaceDark = Color(0xFF0F172A)
private val SurfaceContainerDark = Color(0xFF1E293B)
private val OnSurfaceDark = Color(0xFFE2E8F0)
private val OnSurfaceVariantDark = Color(0xFF94A3B8)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceContainerLight = Color(0xFFF1F5F9)
private val OnSurfaceLight = Color(0xFF0F172A)
private val OnSurfaceVariantLight = Color(0xFF475569)
private val ErrorDark = Color(0xFFF87171)
private val ErrorLight = Color(0xFFDC2626)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = SurfaceDark,
    surface = SurfaceDark,
    background = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = SurfaceLight,
    surface = SurfaceLight,
    background = SurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorLight,
)

/**
 * 앱 테마. 기본값은 **다크** — 헬스장 조명 환경 고려. (REQ-CMN-005)
 */
@Composable
fun MyFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MyFitTypography,
        content = content,
    )
}
