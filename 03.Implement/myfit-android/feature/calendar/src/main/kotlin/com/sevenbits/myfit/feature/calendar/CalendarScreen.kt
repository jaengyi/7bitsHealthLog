package com.sevenbits.myfit.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/**
 * SCR-CAL-001 캘린더 — 골격 단계 플레이스홀더.
 *
 * 상태를 받지 않는 stateless Composable 로 분리해 두었다.
 * 후속 구현에서 XxxRoute(viewModel) 가 이 화면을 감싼다. (03_모듈설계서 §4.2 C1)
 */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "캘린더", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "SCR-CAL-001 · 구현 예정",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun CalendarScreenDarkPreview() {
    MyFitTheme(darkTheme = true) { CalendarScreen() }
}

@Preview(name = "light", showBackground = true)
@Composable
private fun CalendarScreenLightPreview() {
    MyFitTheme(darkTheme = false) { CalendarScreen() }
}
