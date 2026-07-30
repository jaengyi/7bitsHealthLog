package com.sevenbits.myfit.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/**
 * 일지 내 종목 카드. (06_화면설계서 §3.11)
 *
 * 세트를 펼치지 않아도 수행 내역을 한눈에 파악할 수 있도록
 * 세트 수·볼륨과 함께 **세트별 요약(60×10 80×8 …)** 을 한 줄로 노출한다.
 */
@Composable
fun ExerciseCard(
    orderNo: Int,
    exerciseName: String,
    setSummary: String,
    setCountDisplay: String,
    volumeDisplay: String,
    modifier: Modifier = Modifier,
    hasMemo: Boolean = false,
    hasPtNote: Boolean = false,
    supersetLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.SpaceMd)
                .heightIn(min = Dimens.MinTouchTarget),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                Text(
                    text = "$orderNo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (hasMemo) Text("💬", style = MaterialTheme.typography.labelSmall)
                // PT 학습 노트 보유 표시 — 수행 중 즉시 열람 진입점 (FN-EXR-014)
                if (hasPtNote) Text("📝", style = MaterialTheme.typography.labelSmall)
            }

            if (supersetLabel != null) {
                Text(
                    text = supersetLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Text(
                text = "$setCountDisplay · $volumeDisplay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (setSummary.isNotBlank()) {
                Text(
                    text = setSummary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun ExerciseCardDarkPreview() {
    MyFitTheme(darkTheme = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            ExerciseCard(
                orderNo = 1,
                exerciseName = "바벨 벤치프레스",
                setSummary = "60×10  80×8  80×8  70×10",
                setCountDisplay = "4세트",
                volumeDisplay = "2,240 kg",
                hasMemo = true,
                hasPtNote = true,
            )
            ExerciseCard(
                orderNo = 2,
                exerciseName = "케이블 크로스오버",
                setSummary = "20×12  22.5×10",
                setCountDisplay = "2세트",
                volumeDisplay = "465 kg",
                supersetLabel = "슈퍼세트 A",
            )
        }
    }
}

@Preview(name = "light - 빈 세트", showBackground = true)
@Composable
private fun ExerciseCardLightPreview() {
    MyFitTheme(darkTheme = false) {
        ExerciseCard(
            orderNo = 1,
            exerciseName = "랫풀다운",
            setSummary = "",
            setCountDisplay = "0세트",
            volumeDisplay = "0 kg",
        )
    }
}
