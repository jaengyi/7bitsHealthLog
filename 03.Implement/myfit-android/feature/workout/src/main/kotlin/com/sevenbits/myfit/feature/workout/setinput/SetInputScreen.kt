package com.sevenbits.myfit.feature.workout.setinput

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.designsystem.component.NumberStepper
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.ui.SetRow

/**
 * SCR-WRK-002 세트 입력 (stateless). (06_화면설계서 §3.12)
 *
 * 화면 하단 1/3 에 입력기를 배치해 한손 조작을 지원한다. (U1)
 * 세트 표는 입력 중에도 계속 보인다 — 시스템 키보드를 쓰지 않는 이유다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputScreen(
    uiState: SetInputUiState,
    onAction: (SetInputAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.exerciseName, style = MaterialTheme.typography.titleLarge)
                        uiState.prefillHint?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = { onAction(SetInputAction.OnBack) }) { Text("←") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SetTableHeader()

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.sets, key = { it.id }) { set ->
                    SetRow(
                        setNo = set.setNo,
                        setType = set.setType,
                        weightDisplay = set.weightDisplay(uiState.weightUnit),
                        repsDisplay = set.repsDisplay(),
                        isCompleted = set.isCompleted,
                        isSelected = set.id == uiState.selectedSetId,
                        hasMemo = !set.memo.isNullOrBlank(),
                        onToggleComplete = { onAction(SetInputAction.OnToggleComplete(set.id)) },
                        onClick = { onAction(SetInputAction.OnSetSelect(set.id)) },
                        onTypeClick = { onAction(SetInputAction.OnSetTypeCycle(set.id)) },
                    )
                }
            }

            HorizontalDivider()

            // 하단 입력 영역 — 한손 조작 (U1)
            SetInputPanel(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        listOf("세트", "타입", "기록", "완료").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetInputPanel(
    uiState: SetInputUiState,
    onAction: (SetInputAction) -> Unit,
) {
    val selected = uiState.selectedSet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        if (selected != null) {
            // 기록 유형에 따라 표시할 입력 필드가 달라진다 (FN-EXR-012 / REQ-EXR-007)
            when (uiState.recordType) {
                RecordType.WEIGHT_REPS, RecordType.WEIGHT_TIME -> {
                    LabeledStepper(
                        label = "중량",
                        value = selected.weightKg?.toString() ?: "-",
                        onDecrease = { onAction(SetInputAction.OnWeightDecrease) },
                        onIncrease = { onAction(SetInputAction.OnWeightIncrease) },
                    )
                    LabeledStepper(
                        label = if (uiState.recordType == RecordType.WEIGHT_REPS) "횟수" else "시간(초)",
                        value = selected.reps?.toString() ?: "-",
                        onDecrease = { onAction(SetInputAction.OnRepsDecrease) },
                        onIncrease = { onAction(SetInputAction.OnRepsIncrease) },
                    )
                }

                RecordType.REPS_ONLY -> LabeledStepper(
                    label = "횟수",
                    value = selected.reps?.toString() ?: "-",
                    onDecrease = { onAction(SetInputAction.OnRepsDecrease) },
                    onIncrease = { onAction(SetInputAction.OnRepsIncrease) },
                )

                RecordType.TIME, RecordType.DISTANCE_TIME -> LabeledStepper(
                    label = "시간(초)",
                    value = selected.durationSec?.toString() ?: "-",
                    onDecrease = { onAction(SetInputAction.OnRepsDecrease) },
                    onIncrease = { onAction(SetInputAction.OnRepsIncrease) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Button(
                onClick = { onAction(SetInputAction.OnAddSet) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text("＋ 세트 추가")
            }
            OutlinedButton(
                onClick = { onAction(SetInputAction.OnCopyLastSet) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text("⧉ 직전 복사")
            }
        }
    }
}

@Composable
private fun LabeledStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumberStepper(value = value, onDecrease = onDecrease, onIncrease = onIncrease)
    }
}

// ── Preview ──────────────────────────────────────────────

private fun previewSet(no: Int, weight: Double, reps: Int, type: SetType, done: Boolean) =
    WorkoutSet(
        id = "s$no",
        setNo = no,
        setType = type,
        weightKg = weight,
        reps = reps,
        isCompleted = done,
    )

@Preview(name = "dark", showBackground = true)
@Composable
private fun SetInputDarkPreview() {
    MyFitTheme(darkTheme = true) {
        SetInputScreen(
            uiState = SetInputUiState(
                isLoading = false,
                exerciseName = "바벨 벤치프레스",
                prefillHint = "직전: 7/28  80×8 (3세트)",
                weightUnit = WeightUnit.KG,
                sets = listOf(
                    previewSet(1, 40.0, 12, SetType.WARMUP, true),
                    previewSet(2, 60.0, 10, SetType.NORMAL, true),
                    previewSet(3, 80.0, 8, SetType.NORMAL, false),
                ),
                selectedSetId = "s3",
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 맨몸", showBackground = true)
@Composable
private fun SetInputBodyweightPreview() {
    MyFitTheme(darkTheme = false) {
        SetInputScreen(
            uiState = SetInputUiState(
                isLoading = false,
                exerciseName = "풀업",
                recordType = RecordType.REPS_ONLY,
                sets = listOf(WorkoutSet(id = "s1", setNo = 1, reps = 10)),
                selectedSetId = "s1",
            ),
            onAction = {},
        )
    }
}
