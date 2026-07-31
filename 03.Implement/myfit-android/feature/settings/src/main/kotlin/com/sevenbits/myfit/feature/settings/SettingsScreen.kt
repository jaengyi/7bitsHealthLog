package com.sevenbits.myfit.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.feature.settings.component.label

/**
 * SCR-CMN-005 환경설정 (stateless). (06_화면설계서 §3.5)
 *
 * 여기서 바꾼 값은 **실제 동작에 즉시 반영된다** — 중량 증감 단위는 세트 입력의 ± 버튼에,
 * 기본 휴식시간은 세트 완료 시 시작되는 타이머에 쓰인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("환경설정") },
                navigationIcon = {
                    TextButton(onClick = { onAction(SettingsAction.OnBack) }) { Text("←") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            SectionTitle("표시")
            ChoiceRow(
                label = "테마",
                options = ThemeMode.entries,
                selected = uiState.setting.themeMode,
                labelOf = { it.label() },
                onSelect = { onAction(SettingsAction.OnThemeChange(it)) },
            )

            HorizontalDivider()
            SectionTitle("단위")
            ChoiceRow(
                label = "중량",
                options = WeightUnit.entries,
                selected = uiState.setting.weightUnit,
                labelOf = { it.label() },
                onSelect = { onAction(SettingsAction.OnWeightUnitChange(it)) },
            )
            ChoiceRow(
                label = "증감 단위",
                options = WEIGHT_STEPS,
                selected = uiState.setting.weightStepKg,
                labelOf = { "${it}kg" },
                onSelect = { onAction(SettingsAction.OnWeightStepChange(it)) },
            )

            HorizontalDivider()
            SectionTitle("운동")
            ChoiceRow(
                label = "기본 휴식시간",
                options = REST_OPTIONS,
                selected = uiState.setting.defaultRestSec,
                labelOf = { "${it}초" },
                onSelect = { onAction(SettingsAction.OnDefaultRestChange(it)) },
            )
            SwitchRow(
                label = "타이머 소리",
                checked = uiState.setting.timerSoundEnabled,
                onChange = { onAction(SettingsAction.OnTimerSoundToggle(it)) },
            )
            SwitchRow(
                label = "타이머 진동",
                checked = uiState.setting.timerVibrateEnabled,
                onChange = { onAction(SettingsAction.OnTimerVibrateToggle(it)) },
            )
            ChoiceRow(
                label = "주간 목표 횟수",
                options = WEEKLY_GOALS,
                selected = uiState.setting.weeklyGoalCount,
                labelOf = { "${it}회" },
                onSelect = { onAction(SettingsAction.OnWeeklyGoalChange(it)) },
            )
            ChoiceRow(
                label = "바 무게",
                options = BAR_WEIGHTS,
                selected = uiState.setting.defaultBarWeightKg,
                labelOf = { "${it}kg" },
                onSelect = { onAction(SettingsAction.OnBarWeightChange(it)) },
            )

            HorizontalDivider()
            SectionTitle("분석")
            ChoiceRow(
                label = "1RM 산출 공식",
                options = OneRmFormula.entries,
                selected = uiState.setting.oneRmFormula,
                labelOf = { it.label() },
                onSelect = { onAction(SettingsAction.OnOneRmFormulaChange(it)) },
            )

            HorizontalDivider()
            TextButton(
                onClick = { onAction(SettingsAction.OnOpenProfile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text("프로필 편집")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private val WEIGHT_STEPS = listOf(1.0, 2.5, 5.0)
private val REST_OPTIONS = listOf(60, 90, 120, 150, 180)
private val WEEKLY_GOALS = listOf(2, 3, 4, 5, 6)
private val BAR_WEIGHTS = listOf(15.0, 20.0)

@Preview(name = "dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    MyFitTheme(darkTheme = true) {
        SettingsScreen(uiState = SettingsUiState(isLoading = false), onAction = {})
    }
}

@Preview(name = "light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    MyFitTheme(darkTheme = false) {
        SettingsScreen(uiState = SettingsUiState(isLoading = false), onAction = {})
    }
}
