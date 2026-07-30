package com.sevenbits.myfit.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/**
 * 값 증감 입력기. (FN-WRK-010 / 06_화면설계서 §1.4)
 *
 * 버튼은 56dp — 최소 터치 타겟(48dp)보다 크게 잡아 땀·장갑 착용 상태의 오조작을 줄인다.
 * (U2 / REQ-NFR-002)
 *
 * @param value 표시할 값. 단위 변환은 호출부(Presentation)에서 이미 끝난 상태여야 한다. (P7)
 * @param onDecrease 감소. 하한에 도달하면 호출부가 [canDecrease] 를 false 로 준다.
 */
@Composable
fun NumberStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
    decreaseDescription: String = "감소",
    increaseDescription: String = "증가",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        FilledTonalIconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(Dimens.StepperButton),
        ) {
            Icon(Icons.Default.Remove, contentDescription = decreaseDescription)
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = Dimens.StepperButton),
        )

        FilledTonalIconButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.size(Dimens.StepperButton),
        ) {
            Icon(Icons.Default.Add, contentDescription = increaseDescription)
        }
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun NumberStepperDarkPreview() {
    MyFitTheme(darkTheme = true) {
        NumberStepper(value = "80.0", onDecrease = {}, onIncrease = {})
    }
}

@Preview(name = "light", showBackground = true)
@Composable
private fun NumberStepperLightPreview() {
    MyFitTheme(darkTheme = false) {
        NumberStepper(value = "8", onDecrease = {}, onIncrease = {}, canDecrease = false)
    }
}
