package com.sevenbits.myfit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.SetType

/**
 * 세트 표의 한 행. (06_화면설계서 §1.4 / §3.12)
 *
 * 표시 전용이다. 값 변환(kg↔lb 등)은 이미 끝난 문자열을 받는다. (P7)
 */
@Composable
fun SetRow(
    setNo: Int,
    setType: SetType,
    weightDisplay: String,
    repsDisplay: String,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    hasMemo: Boolean = false,
    intensityDisplay: String? = null,
    onClick: (() -> Unit)? = null,
    onTypeClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.SpaceSm))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // 터치 타겟 최소 48dp 확보 (U2)
            .heightIn(min = Dimens.MinTouchTarget)
            .padding(horizontal = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = setNo.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )

        SetTypeBadge(
            setType = setType,
            modifier = Modifier
                .width(44.dp)
                .then(if (onTypeClick != null) Modifier.clickable(onClick = onTypeClick) else Modifier),
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                Text(weightDisplay, style = MaterialTheme.typography.titleLarge)
                Text(repsDisplay, style = MaterialTheme.typography.titleLarge)
            }
            if (intensityDisplay != null) {
                Text(
                    text = intensityDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (hasMemo) {
            Text(
                text = "💬",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = Dimens.SpaceXs),
            )
        }

        Checkbox(checked = isCompleted, onCheckedChange = { onToggleComplete() })
    }
}

/**
 * 세트 타입 배지.
 *
 * 웜업은 볼륨·강도 집계에서 제외되므로(FN-WRK-013) 시각적으로도 약하게 표시해
 * 실작업 세트와 구분되도록 한다.
 */
@Composable
private fun SetTypeBadge(setType: SetType, modifier: Modifier = Modifier) {
    val (label, color) = when (setType) {
        SetType.NORMAL -> "정상" to MaterialTheme.colorScheme.onSurfaceVariant
        SetType.WARMUP -> "웜업" to MaterialTheme.colorScheme.onSurfaceVariant
        SetType.DROP -> "드랍" to MaterialTheme.colorScheme.tertiary
        SetType.FAILURE -> "실패" to MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun SetRowDarkPreview() {
    MyFitTheme(darkTheme = true) {
        Column {
            SetRow(1, SetType.WARMUP, "40.0 kg", "12회", true, {})
            SetRow(2, SetType.NORMAL, "60.0 kg", "10회", true, {})
            SetRow(3, SetType.NORMAL, "80.0 kg", "8회", false, {}, isSelected = true, intensityDisplay = "89% · 근력")
            SetRow(4, SetType.DROP, "60.0 kg", "6회", false, {}, hasMemo = true)
            SetRow(5, SetType.FAILURE, "80.0 kg", "3회", false, {})
        }
    }
}

@Preview(name = "light", showBackground = true)
@Composable
private fun SetRowLightPreview() {
    MyFitTheme(darkTheme = false) {
        SetRow(1, SetType.NORMAL, "80.0 kg", "8회", false, {})
    }
}
