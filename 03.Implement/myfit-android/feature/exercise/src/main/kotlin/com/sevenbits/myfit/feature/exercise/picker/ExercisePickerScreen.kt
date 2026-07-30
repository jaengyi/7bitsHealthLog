package com.sevenbits.myfit.feature.exercise.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sevenbits.myfit.core.designsystem.component.EmptyState
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.RecordType

/**
 * SCR-EXR-001 종목 선택 (stateless). (06_화면설계서 §3.7)
 *
 * ViewModel 을 받지 않는다 — Preview 가능하고 테스트하기 쉽다. (C1)
 */
@Composable
fun ExercisePickerScreen(
    uiState: ExercisePickerUiState,
    onAction: (ExercisePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onAction(ExercisePickerAction.OnQueryChange(it)) },
                placeholder = { Text("종목명 또는 초성 검색") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimens.ScreenPadding),
            )
            // 기본 라이브러리에 없는 종목 직접 등록 (FN-EXR-005)
            TextButton(
                onClick = { onAction(ExercisePickerAction.OnCreateExercise) },
                modifier = Modifier.padding(end = Dimens.SpaceSm),
            ) {
                Text("＋ 등록")
            }
        }

        FilterChipRow(
            uiState = uiState,
            onAction = onAction,
        )

        HorizontalDivider()

        if (uiState.showEmptyState) {
            EmptyState(
                message = "'${uiState.query}' 와 일치하는 종목이 없습니다",
                actionLabel = "필터 초기화",
                onAction = { onAction(ExercisePickerAction.OnClearFilters) },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (!uiState.isFiltering) {
                    // 종목 추가의 대부분은 최근·즐겨찾기에서 끝난다 (FN-EXR-011/010)
                    section("최근 사용", uiState.recent, uiState, onAction)
                    section("즐겨찾기", uiState.favorites, uiState, onAction)
                    section("전체", uiState.results, uiState, onAction)
                } else {
                    items(uiState.results, key = { it.id }) { exercise ->
                        ExerciseRow(exercise, exercise.id in uiState.selectedIds, onAction)
                    }
                }
            }
        }

        // 추가 전 미리보기 시트 (FN-EXR-018)
        uiState.preview?.let { preview ->
            ExerciseGuideSheet(
                preview = preview,
                isSelected = preview.exercise.id in uiState.selectedIds,
                onAction = onAction,
            )
        }

        // 하단 고정 — 한손 조작 영역 (U1)
        Surface(tonalElevation = 3.dp) {
            Button(
                onClick = { onAction(ExercisePickerAction.OnConfirmSelection) },
                enabled = uiState.selectedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(
                    if (uiState.selectedCount > 0) {
                        "선택한 ${uiState.selectedCount}개 종목 추가"
                    } else {
                        "종목을 선택하세요"
                    },
                )
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    exercises: List<Exercise>,
    uiState: ExercisePickerUiState,
    onAction: (ExercisePickerAction) -> Unit,
) {
    if (exercises.isEmpty()) return
    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = Dimens.ScreenPadding,
                vertical = Dimens.SpaceSm,
            ),
        )
    }
    items(exercises, key = { "$title-${it.id}" }) { exercise ->
        ExerciseRow(exercise, exercise.id in uiState.selectedIds, onAction)
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    isSelected: Boolean,
    onAction: (ExercisePickerAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .padding(horizontal = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onAction(ExercisePickerAction.OnSelectionToggle(exercise.id)) },
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = exercise.equipmentLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = { onAction(ExercisePickerAction.OnFavoriteToggle(exercise.id)) }) {
            Icon(
                imageVector = if (exercise.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = if (exercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기",
            )
        }

        // ⓘ — 추가 전 미리보기 (FN-EXR-018)
        IconButton(onClick = { onAction(ExercisePickerAction.OnPreviewOpen(exercise.id)) }) {
            Icon(Icons.Default.Info, contentDescription = "상세 미리보기")
        }
    }
}

@Composable
private fun FilterChipRow(
    uiState: ExercisePickerUiState,
    onAction: (ExercisePickerAction) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(uiState.availableBodyParts, key = { it.code }) { part ->
            FilterChip(
                selected = part.code in uiState.bodyPartFilters,
                onClick = { onAction(ExercisePickerAction.OnBodyPartToggle(part.code)) },
                label = { Text(part.name) },
            )
        }
        items(Equipment.entries.toList(), key = { it.name }) { equipment ->
            FilterChip(
                selected = equipment in uiState.equipmentFilters,
                onClick = { onAction(ExercisePickerAction.OnEquipmentToggle(equipment)) },
                label = { Text(equipment.label()) },
            )
        }
    }
}

private fun Exercise.equipmentLabel(): String {
    val parts = primaryBodyParts.joinToString("·")
    return listOfNotNull(equipment.label(), parts.takeIf { it.isNotBlank() }).joinToString(" · ")
}

private fun Equipment.label(): String = when (this) {
    Equipment.BARBELL -> "바벨"
    Equipment.DUMBBELL -> "덤벨"
    Equipment.MACHINE -> "머신"
    Equipment.CABLE -> "케이블"
    Equipment.BODYWEIGHT -> "맨몸"
    Equipment.ETC -> "기타"
}

// ── Preview ──────────────────────────────────────────────

private fun sampleExercise(id: String, name: String, favorite: Boolean = false) = Exercise(
    id = id,
    name = name,
    recordType = RecordType.WEIGHT_REPS,
    exerciseType = ExerciseType.WEIGHT,
    equipment = Equipment.BARBELL,
    isFavorite = favorite,
    primaryBodyParts = listOf("CHEST"),
)

@Preview(name = "dark", showBackground = true)
@Composable
private fun ExercisePickerDarkPreview() {
    MyFitTheme(darkTheme = true) {
        ExercisePickerScreen(
            uiState = ExercisePickerUiState(
                isLoading = false,
                recent = listOf(sampleExercise("1", "바벨 벤치프레스", favorite = true)),
                results = listOf(
                    sampleExercise("2", "인클라인 바벨 벤치프레스"),
                    sampleExercise("3", "덤벨 플라이"),
                ),
                selectedIds = setOf("1"),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 검색 결과 없음", showBackground = true)
@Composable
private fun ExercisePickerEmptyPreview() {
    MyFitTheme(darkTheme = false) {
        ExercisePickerScreen(
            uiState = ExercisePickerUiState(isLoading = false, query = "없는종목"),
            onAction = {},
        )
    }
}
