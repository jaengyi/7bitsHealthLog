package com.sevenbits.myfit.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/** 키패드 입력 이벤트 */
sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey
    data object Decimal : KeypadKey
    data object Backspace : KeypadKey
    data object Confirm : KeypadKey
}

/**
 * 세트 입력 전용 숫자 키패드. (06_화면설계서 §3.12)
 *
 * **시스템 키보드를 쓰지 않는 이유**: 시스템 키보드는 화면의 절반을 덮어 세트 표가 가려지고,
 * 숫자 전환 탭이 한 번 더 필요하다. 전용 키패드는 3터치 이내 입력(U3)과
 * 세트 표 상시 노출을 동시에 만족시킨다.
 *
 * @param showDecimal 중량 입력은 소수점을 허용하고, 횟수 입력은 허용하지 않는다.
 */
@Composable
fun NumericKeypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    showDecimal: Boolean = true,
    confirmEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        listOf(1..3, 4..6, 7..9).forEach { range ->
            KeypadRow {
                range.forEach { digit ->
                    KeypadButton(
                        label = digit.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(KeypadKey.Digit(digit)) },
                    )
                }
                if (range.first == 1) {
                    KeypadIconButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(KeypadKey.Backspace) },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "지우기")
                    }
                } else {
                    // 확인 버튼이 세로로 걸치는 자리를 비워 둔다
                    KeypadSpacer(Modifier.weight(1f))
                }
            }
        }

        KeypadRow {
            if (showDecimal) {
                KeypadButton(".", Modifier.weight(1f)) { onKey(KeypadKey.Decimal) }
            } else {
                KeypadSpacer(Modifier.weight(1f))
            }
            KeypadButton("0", Modifier.weight(1f)) { onKey(KeypadKey.Digit(0)) }
            KeypadSpacer(Modifier.weight(1f))
            Button(
                onClick = { onKey(KeypadKey.Confirm) },
                enabled = confirmEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.StepperButton),
            ) {
                Icon(Icons.Default.Check, contentDescription = "완료")
            }
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        content = content,
    )
}

@Composable
private fun KeypadButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.StepperButton),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun KeypadIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.StepperButton),
        content = { content() },
    )
}

@Composable
private fun KeypadSpacer(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier.height(Dimens.StepperButton))
}

@Preview(name = "dark - 중량", showBackground = true)
@Composable
private fun NumericKeypadDarkPreview() {
    MyFitTheme(darkTheme = true) { NumericKeypad(onKey = {}) }
}

@Preview(name = "light - 횟수", showBackground = true)
@Composable
private fun NumericKeypadLightPreview() {
    MyFitTheme(darkTheme = false) { NumericKeypad(onKey = {}, showDecimal = false) }
}
