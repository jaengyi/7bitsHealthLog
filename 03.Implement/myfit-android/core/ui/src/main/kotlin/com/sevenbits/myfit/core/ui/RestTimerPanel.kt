package com.sevenbits.myfit.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/**
 * 휴식 타이머 패널. (FN-TOL-001~003 / 06_화면설계서 §3.13)
 *
 * 표시 전용이다. 잔여 시간 계산은 Foreground Service 가 `elapsedRealtime()` 델타로
 * 산출한 값을 받는다 — 코루틴 delay 누적은 백그라운드에서 드리프트가 생긴다. (07 §7.1)
 *
 * @param remainingDisplay `01:23` 형태로 이미 포맷된 문자열
 * @param progress 0f~1f. 남은 비율
 */
@Composable
fun RestTimerPanel(
    remainingDisplay: String,
    progress: Float,
    isPaused: Boolean,
    onAdjust: (seconds: Int) -> Unit,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = "휴식",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = remainingDisplay,
            // 팔 길이 거리에서 읽혀야 한다 (U6)
            style = MaterialTheme.typography.displayLarge,
        )

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onAdjust(-ADJUST_SECONDS) }) { Text("−15초") }

            TextButton(onClick = onTogglePause) {
                if (isPaused) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "재개")
                } else {
                    Text("일시정지")
                }
            }

            TextButton(onClick = onSkip) {
                Icon(Icons.Default.SkipNext, contentDescription = "건너뛰기")
            }

            TextButton(onClick = { onAdjust(ADJUST_SECONDS) }) { Text("+15초") }
        }
    }
}

private const val ADJUST_SECONDS = 15

@Preview(name = "dark", showBackground = true)
@Composable
private fun RestTimerPanelDarkPreview() {
    MyFitTheme(darkTheme = true) {
        RestTimerPanel(
            remainingDisplay = "01:23",
            progress = 0.68f,
            isPaused = false,
            onAdjust = {},
            onTogglePause = {},
            onSkip = {},
        )
    }
}

@Preview(name = "light - 일시정지", showBackground = true)
@Composable
private fun RestTimerPanelPausedPreview() {
    MyFitTheme(darkTheme = false) {
        RestTimerPanel(
            remainingDisplay = "00:42",
            progress = 0.35f,
            isPaused = true,
            onAdjust = {},
            onTogglePause = {},
            onSkip = {},
        )
    }
}
