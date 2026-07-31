package com.sevenbits.myfit.feature.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.component.NumberStepper
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.validation.ProfileValidator
import com.sevenbits.myfit.feature.settings.component.SegmentedChoice
import com.sevenbits.myfit.feature.settings.component.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 키 미입력 상태에서 처음 ± 를 눌렀을 때 시작점 */
private const val DEFAULT_HEIGHT_CM = 170.0
private const val HEIGHT_STEP_CM = 0.5

/**
 * SCR-CMN-004 프로필 편집 (stateless). (06_화면설계서 §3.4)
 *
 * 저장 버튼이 없다 — 값을 바꾸는 즉시 저장된다(P5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    uiState: ProfileEditUiState,
    onAction: (ProfileEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                navigationIcon = {
                    TextButton(onClick = { onAction(ProfileEditAction.OnBack) }) { Text("←") }
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
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            OutlinedTextField(
                value = uiState.profile.displayName.orEmpty(),
                onValueChange = { onAction(ProfileEditAction.OnNameChange(it)) },
                label = { Text("이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SegmentedChoice(
                label = "성별",
                options = Gender.entries,
                selected = uiState.profile.gender,
                labelOf = { it.label() },
                onSelect = { onAction(ProfileEditAction.OnGenderChange(it)) },
            )

            // 생년월일 — 직접 타이핑보다 DatePicker 가 오입력이 적다
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text("생년월일", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(uiState.profile.birthDate ?: "선택하기")
                }
                val support = uiState.birthDateError
                    ?: uiState.age?.let { "만 $it 세" }
                if (support != null) {
                    Text(
                        text = support,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.birthDateError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // 키 — 0.5cm 단위, 범위를 벗어나지 않도록 버튼 자체를 잠근다
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text("키", style = MaterialTheme.typography.bodyMedium)
                val height = uiState.profile.heightCm
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberStepper(
                        value = height?.let { "%.1f cm".format(it) } ?: "- cm",
                        onDecrease = {
                            onAction(
                                ProfileEditAction.OnHeightChange(
                                    (height ?: DEFAULT_HEIGHT_CM) - HEIGHT_STEP_CM,
                                ),
                            )
                        },
                        onIncrease = {
                            onAction(
                                ProfileEditAction.OnHeightChange(
                                    (height ?: DEFAULT_HEIGHT_CM) + HEIGHT_STEP_CM,
                                ),
                            )
                        },
                        canDecrease = height == null ||
                            height - HEIGHT_STEP_CM >= ProfileValidator.MIN_HEIGHT_CM,
                        canIncrease = height == null ||
                            height + HEIGHT_STEP_CM <= ProfileValidator.MAX_HEIGHT_CM,
                        decreaseDescription = "키 감소",
                        increaseDescription = "키 증가",
                    )
                }
                if (uiState.heightError != null) {
                    Text(
                        text = uiState.heightError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SegmentedChoice(
                label = "운동 경력",
                options = CareerLevel.entries,
                selected = uiState.profile.careerLevel,
                labelOf = { it.label() },
                onSelect = { onAction(ProfileEditAction.OnCareerChange(it)) },
            )

            SegmentedChoice(
                label = "운동 목표",
                options = GoalType.entries,
                selected = uiState.profile.goalType,
                labelOf = { it.label() },
                onSelect = { onAction(ProfileEditAction.OnGoalChange(it)) },
            )
        }
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initial = uiState.profile.birthDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                onAction(ProfileEditAction.OnBirthDateChange(date))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialMillis = remember(initial) {
        runCatching {
            LocalDate.parse(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    // DatePicker 는 UTC 자정을 돌려준다. 로컬 타임존으로 변환하면
                    // 시차만큼 하루가 밀리므로 UTC 로 그대로 해석한다.
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(date.toString())
                },
            ) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = state)
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun ProfileEditDarkPreview() {
    MyFitTheme(darkTheme = true) {
        ProfileEditScreen(uiState = ProfileEditUiState(isLoading = false), onAction = {})
    }
}
