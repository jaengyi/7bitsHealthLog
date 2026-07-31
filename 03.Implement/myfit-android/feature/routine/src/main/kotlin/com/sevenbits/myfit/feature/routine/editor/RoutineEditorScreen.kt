package com.sevenbits.myfit.feature.routine.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.RoutineExercise

/**
 * SCR-RTN-002 루틴 편집 (stateless). (06_화면설계서 §3.16)
 *
 * 목표 중량은 **절대값 또는 %1RM 택일**이다. 한쪽을 입력하면 다른 쪽이 해제된다.
 * (FN-RTN-006)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    uiState: RoutineEditorUiState,
    onAction: (RoutineEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "루틴 편집" else "루틴 만들기") },
                navigationIcon = {
                    TextButton(onClick = { onAction(RoutineEditorAction.OnBack) }) { Text("←") }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(RoutineEditorAction.OnSave) },
                        enabled = uiState.canSave,
                    ) {
                        Text("저장")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { onAction(RoutineEditorAction.OnNameChange(it)) },
                    label = { Text("루틴명 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { onAction(RoutineEditorAction.OnDescriptionChange(it)) },
                    label = { Text("설명 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(uiState.exercises, key = { it.id }) { exercise ->
                RoutineExerciseCard(exercise = exercise, onAction = onAction)
            }

            item {
                OutlinedButton(
                    onClick = { onAction(RoutineEditorAction.OnAddExercise) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text("＋ 종목 추가")
                }
            }
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: RoutineExercise,
    onAction: (RoutineEditorAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onAction(RoutineEditorAction.OnRemoveExercise(exercise.id)) },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "종목 제거")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                NumberField(
                    label = "세트",
                    value = exercise.targetSetCount?.toString().orEmpty(),
                    onChange = {
                        onAction(RoutineEditorAction.OnTargetSetsChange(exercise.id, it.toIntOrNull()))
                    },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "횟수",
                    value = exercise.targetReps?.toString().orEmpty(),
                    onChange = {
                        onAction(RoutineEditorAction.OnTargetRepsChange(exercise.id, it.toIntOrNull()))
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // 절대 중량과 %1RM 은 택일 — 한쪽을 채우면 다른 쪽이 비워진다
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                NumberField(
                    label = "중량(kg)",
                    value = exercise.targetWeightKg?.toString().orEmpty(),
                    onChange = {
                        onAction(
                            RoutineEditorAction.OnTargetWeightChange(exercise.id, it.toDoubleOrNull()),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = "또는 1RM %",
                    value = exercise.targetPercentOneRm?.toString().orEmpty(),
                    onChange = {
                        onAction(
                            RoutineEditorAction.OnTargetPercentChange(exercise.id, it.toDoubleOrNull()),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = exercise.targetLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        modifier = modifier,
    )
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun RoutineEditorDarkPreview() {
    MyFitTheme(darkTheme = true) {
        RoutineEditorScreen(
            uiState = RoutineEditorUiState(
                isLoading = false,
                routineId = "r1",
                name = "가슴/삼두",
                exercises = listOf(
                    RoutineExercise(
                        id = "re1",
                        exerciseId = "e1",
                        exerciseName = "바벨 벤치프레스",
                        targetSetCount = 4,
                        targetWeightKg = 80.0,
                        targetReps = 8,
                    ),
                    RoutineExercise(
                        id = "re2",
                        exerciseId = "e2",
                        exerciseName = "인클라인 덤벨 프레스",
                        targetSetCount = 3,
                        targetPercentOneRm = 70.0,
                        targetReps = 10,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 신규", showBackground = true)
@Composable
private fun RoutineEditorNewPreview() {
    MyFitTheme(darkTheme = false) {
        RoutineEditorScreen(uiState = RoutineEditorUiState(isLoading = false), onAction = {})
    }
}
