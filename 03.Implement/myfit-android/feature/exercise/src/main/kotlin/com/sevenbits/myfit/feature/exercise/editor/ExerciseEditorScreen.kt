package com.sevenbits.myfit.feature.exercise.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.MovementType
import com.sevenbits.myfit.core.domain.model.RecordType

/**
 * SCR-EXR-003 종목 등록/편집 (stateless). (06_화면설계서 §3.9)
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(
    uiState: ExerciseEditorUiState,
    onAction: (ExerciseEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "종목 편집" else "종목 등록") },
                navigationIcon = {
                    TextButton(onClick = { onAction(ExerciseEditorAction.OnBack) }) { Text("←") }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(ExerciseEditorAction.OnSave) },
                        enabled = uiState.canSave,
                    ) {
                        Text("저장")
                    }
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
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onAction(ExerciseEditorAction.OnNameChange(it)) },
                label = { Text("종목명 *") },
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.nameEn,
                onValueChange = { onAction(ExerciseEditorAction.OnNameEnChange(it)) },
                label = { Text("영문명 (선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 기록 유형은 등록 후 변경 불가 — 기존 세트와 필드 구성이 어긋난다 (REQ-EXR-007)
            ChoiceSection(
                title = if (uiState.isRecordTypeEditable) {
                    "기록 유형 *"
                } else {
                    "기록 유형 (등록 후 변경 불가)"
                },
                options = RecordType.entries,
                selected = uiState.recordType,
                labelOf = { it.label() },
                enabled = uiState.isRecordTypeEditable,
                onSelect = { onAction(ExerciseEditorAction.OnRecordTypeChange(it)) },
            )

            ChoiceSection(
                title = "운동 유형 *",
                options = ExerciseType.entries,
                selected = uiState.exerciseType,
                labelOf = { it.label() },
                onSelect = { onAction(ExerciseEditorAction.OnExerciseTypeChange(it)) },
            )

            ChoiceSection(
                title = "장비 *",
                options = Equipment.entries,
                selected = uiState.equipment,
                labelOf = { it.label() },
                onSelect = { onAction(ExerciseEditorAction.OnEquipmentChange(it)) },
            )

            ChoiceSection(
                title = "동작 성격",
                options = MovementType.entries,
                selected = uiState.movementType,
                labelOf = { it.label() },
                onSelect = { onAction(ExerciseEditorAction.OnMovementTypeChange(it)) },
            )

            BodyPartSection(
                title = "주동근 * (부위별 세트 비중 집계 기준)",
                parts = uiState.availableBodyParts,
                selected = uiState.primaryBodyParts,
                onToggle = { onAction(ExerciseEditorAction.OnPrimaryToggle(it)) },
            )

            BodyPartSection(
                title = "보조근",
                parts = uiState.availableBodyParts,
                selected = uiState.secondaryBodyParts,
                onToggle = { onAction(ExerciseEditorAction.OnSecondaryToggle(it)) },
            )

            OutlinedTextField(
                value = uiState.defaultRestSec?.toString().orEmpty(),
                onValueChange = { input ->
                    onAction(ExerciseEditorAction.OnRestSecChange(input.toIntOrNull()))
                },
                label = { Text("기본 휴식시간 (초) — 미설정 시 전역값") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onAction(ExerciseEditorAction.OnDescriptionChange(it)) },
                label = { Text("수행 방법") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.caution,
                onValueChange = { onAction(ExerciseEditorAction.OnCautionChange(it)) },
                label = { Text("주의사항") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.isEditMode) {
                HorizontalDivider()
                TextButton(
                    onClick = { onAction(ExerciseEditorAction.OnDelete) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text("종목 삭제", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    text = "이 종목으로 남긴 기록이 있으면 삭제되지 않고 목록에서만 숨겨집니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BodyPartSection(
    title: String,
    parts: List<BodyPart>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            parts.forEach { part ->
                FilterChip(
                    selected = part.code in selected,
                    onClick = { onToggle(part.code) },
                    label = { Text(part.name) },
                )
            }
        }
    }
}

private fun RecordType.label(): String = when (this) {
    RecordType.WEIGHT_REPS -> "중량+횟수"
    RecordType.REPS_ONLY -> "횟수"
    RecordType.TIME -> "시간"
    RecordType.DISTANCE_TIME -> "거리+시간"
    RecordType.WEIGHT_TIME -> "중량+시간"
}

private fun ExerciseType.label(): String = when (this) {
    ExerciseType.WEIGHT -> "웨이트"
    ExerciseType.CARDIO -> "유산소"
    ExerciseType.BODYWEIGHT -> "맨몸"
    ExerciseType.STRETCHING -> "스트레칭"
}

private fun Equipment.label(): String = when (this) {
    Equipment.BARBELL -> "바벨"
    Equipment.DUMBBELL -> "덤벨"
    Equipment.MACHINE -> "머신"
    Equipment.CABLE -> "케이블"
    Equipment.BODYWEIGHT -> "맨몸"
    Equipment.ETC -> "기타"
}

private fun MovementType.label(): String = when (this) {
    MovementType.COMPOUND -> "다관절"
    MovementType.ISOLATION -> "단일관절"
}

@Preview(name = "dark - 등록", showBackground = true)
@Composable
private fun ExerciseEditorNewPreview() {
    MyFitTheme(darkTheme = true) {
        ExerciseEditorScreen(
            uiState = ExerciseEditorUiState(
                isLoading = false,
                availableBodyParts = listOf(
                    BodyPart("CHEST", "가슴", "UPPER", 1),
                    BodyPart("BACK", "등", "UPPER", 2),
                    BodyPart("ARM", "팔", "UPPER", 4),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 편집", showBackground = true)
@Composable
private fun ExerciseEditorEditPreview() {
    MyFitTheme(darkTheme = false) {
        ExerciseEditorScreen(
            uiState = ExerciseEditorUiState(
                isLoading = false,
                exerciseId = "user-1",
                name = "나만의 벤치 변형",
                primaryBodyParts = setOf("CHEST"),
                availableBodyParts = listOf(BodyPart("CHEST", "가슴", "UPPER", 1)),
            ),
            onAction = {},
        )
    }
}
