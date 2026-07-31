package com.sevenbits.myfit.feature.settings.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.designsystem.component.NumberStepper
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.core.domain.validation.ProfileValidator
import com.sevenbits.myfit.feature.settings.component.SegmentedChoice
import com.sevenbits.myfit.feature.settings.component.label

private const val DEFAULT_HEIGHT_CM = 170.0
private const val HEIGHT_STEP_CM = 0.5

/**
 * SCR-CMN-006 온보딩 (stateless). (06_화면설계서 §3.6)
 *
 * "건너뛰기"는 항상 보이는 자리에 둔다. 입력을 강요하지 않는 것이 요구사항이다.
 * (REQ-CMN-004)
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { onAction(OnboardingAction.OnSkip) },
                    enabled = !uiState.isFinishing,
                ) { Text("건너뛰기") }
            }

            StepIndicator(step = uiState.step)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            ) {
                when (uiState.step) {
                    0 -> ProfileStep(uiState, onAction)
                    1 -> GoalStep(uiState, onAction)
                    2 -> UnitStep(uiState, onAction)
                    else -> HabitStep(uiState, onAction)
                }
            }

            // 주요 동작은 하단에 — 한 손 조작 범위 (U1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.BottomActionHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                if (uiState.step > 0) {
                    TextButton(
                        onClick = { onAction(OnboardingAction.OnPrevious) },
                        enabled = !uiState.isFinishing,
                    ) { Text("이전") }
                }
                Button(
                    onClick = { onAction(OnboardingAction.OnNext) },
                    enabled = !uiState.isFinishing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(if (uiState.isLastStep) "시작하기" else "다음")
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        repeat(ONBOARDING_STEP_COUNT) { index ->
            val done = index <= step
            Box(
                modifier = Modifier
                    .size(Dimens.SpaceSm)
                    .clip(CircleShape)
                    .background(
                        if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
            if (index < ONBOARDING_STEP_COUNT - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.SpaceXs / 2)
                        .background(
                            if (index < step) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
        }
        Text(
            text = "${step + 1} / $ONBOARDING_STEP_COUNT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = Dimens.SpaceMd),
    )
}

@Composable
private fun ProfileStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepTitle("어떤 분이신가요?")
    SegmentedChoice(
        label = "성별",
        options = Gender.entries,
        selected = uiState.profile.gender,
        labelOf = { it.label() },
        onSelect = { onAction(OnboardingAction.OnGenderChange(it)) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text("키", style = MaterialTheme.typography.bodyMedium)
        val height = uiState.profile.heightCm
        NumberStepper(
            value = height?.let { "%.1f cm".format(it) } ?: "- cm",
            onDecrease = {
                onAction(
                    OnboardingAction.OnHeightChange((height ?: DEFAULT_HEIGHT_CM) - HEIGHT_STEP_CM),
                )
            },
            onIncrease = {
                onAction(
                    OnboardingAction.OnHeightChange((height ?: DEFAULT_HEIGHT_CM) + HEIGHT_STEP_CM),
                )
            },
            canDecrease = height == null ||
                height - HEIGHT_STEP_CM >= ProfileValidator.MIN_HEIGHT_CM,
            canIncrease = height == null ||
                height + HEIGHT_STEP_CM <= ProfileValidator.MAX_HEIGHT_CM,
            decreaseDescription = "키 감소",
            increaseDescription = "키 증가",
        )
        Text(
            text = "나중에 프로필에서 바꿀 수 있어요",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepTitle("운동 목표가 무엇인가요?")
    SegmentedChoice(
        label = "목표",
        options = GoalType.entries,
        selected = uiState.profile.goalType,
        labelOf = { it.label() },
        onSelect = { onAction(OnboardingAction.OnGoalChange(it)) },
    )
    SegmentedChoice(
        label = "운동 경력",
        options = CareerLevel.entries,
        selected = uiState.profile.careerLevel,
        labelOf = { it.label() },
        onSelect = { onAction(OnboardingAction.OnCareerChange(it)) },
    )
}

@Composable
private fun UnitStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepTitle("표시 방식을 정해요")
    SegmentedChoice(
        label = "중량 단위",
        options = WeightUnit.entries,
        selected = uiState.setting.weightUnit,
        labelOf = { it.label() },
        onSelect = { onAction(OnboardingAction.OnWeightUnitChange(it)) },
    )
    SegmentedChoice(
        label = "테마",
        options = ThemeMode.entries,
        selected = uiState.setting.themeMode,
        labelOf = { it.label() },
        onSelect = { onAction(OnboardingAction.OnThemeChange(it)) },
        supportingText = "헬스장 조명에서는 다크가 눈에 편해요",
    )
}

@Composable
private fun HabitStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepTitle("운동 습관을 알려 주세요")
    SegmentedChoice(
        label = "주 운동 횟수",
        options = WEEKLY_GOALS,
        selected = uiState.setting.weeklyGoalCount,
        labelOf = { "${it}회" },
        onSelect = { onAction(OnboardingAction.OnWeeklyGoalChange(it)) },
        supportingText = "스트릭 판정 기준이 돼요",
    )
    SegmentedChoice(
        label = "기본 휴식시간",
        options = REST_OPTIONS,
        selected = uiState.setting.defaultRestSec,
        labelOf = { "${it}초" },
        onSelect = { onAction(OnboardingAction.OnRestChange(it)) },
    )
}

private val WEEKLY_GOALS = listOf(2, 3, 4, 5)
private val REST_OPTIONS = listOf(60, 90, 120, 180)

@Preview(name = "step1", showBackground = true)
@Composable
private fun OnboardingStep1Preview() {
    MyFitTheme(darkTheme = true) {
        OnboardingScreen(uiState = OnboardingUiState(step = 0), onAction = {})
    }
}

@Preview(name = "step4", showBackground = true)
@Composable
private fun OnboardingStep4Preview() {
    MyFitTheme(darkTheme = true) {
        OnboardingScreen(uiState = OnboardingUiState(step = 3), onAction = {})
    }
}
