package com.sevenbits.myfit.feature.exercise.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.component.EmptyState
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.ExerciseNote

/**
 * SCR-EXR-004 PT 학습 노트 (stateless). (06_화면설계서 §3.10)
 *
 * 항목별 작성일과 트레이너명을 함께 보관해, 나중에 어느 세션에서 배운 내용인지
 * 추적할 수 있게 한다. (REQ-EXR-008)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseNoteScreen(
    uiState: ExerciseNoteUiState,
    onAction: (ExerciseNoteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PT 학습 노트", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = uiState.exerciseName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = { onAction(ExerciseNoteAction.OnBack) }) { Text("←") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(ExerciseNoteAction.OnAddClick) }) {
                Icon(Icons.Default.Add, contentDescription = "노트 추가")
            }
        },
    ) { innerPadding ->
        if (uiState.notes.isEmpty() && !uiState.isLoading) {
            EmptyState(
                message = "PT에서 배운 자세 큐잉이나 주의점을 기록해 보세요.\n" +
                    "운동 중에 바로 꺼내 볼 수 있습니다.",
                actionLabel = "첫 노트 작성",
                onAction = { onAction(ExerciseNoteAction.OnAddClick) },
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    Dimens.ScreenPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(uiState.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { onAction(ExerciseNoteAction.OnEditClick(note.id)) },
                        onDelete = { onAction(ExerciseNoteAction.OnDelete(note.id)) },
                    )
                }
            }
        }

        uiState.editing?.let { draft ->
            NoteEditorDialog(draft = draft, onAction = onAction)
        }
    }
}

@Composable
private fun NoteCard(note: ExerciseNote, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listOfNotNull(note.noteDate, note.trainerName).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "노트 수정")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "노트 삭제")
                }
            }
            Text(note.content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NoteEditorDialog(draft: NoteDraft, onAction: (ExerciseNoteAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ExerciseNoteAction.OnCancelEdit) },
        title = { Text(if (draft.isNew) "노트 작성" else "노트 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                OutlinedTextField(
                    value = draft.content,
                    onValueChange = { onAction(ExerciseNoteAction.OnContentChange(it)) },
                    label = { Text("배운 내용") },
                    placeholder = { Text("예: 견갑 후인하고 가슴 열기") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.trainerName,
                    onValueChange = { onAction(ExerciseNoteAction.OnTrainerChange(it)) },
                    label = { Text("트레이너 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.noteDate,
                    onValueChange = { onAction(ExerciseNoteAction.OnDateChange(it)) },
                    label = { Text("작성일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ExerciseNoteAction.OnSave) },
                enabled = draft.canSave,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ExerciseNoteAction.OnCancelEdit) }) { Text("취소") }
        },
    )
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun ExerciseNoteDarkPreview() {
    MyFitTheme(darkTheme = true) {
        ExerciseNoteScreen(
            uiState = ExerciseNoteUiState(
                isLoading = false,
                exerciseName = "바벨 벤치프레스",
                notes = listOf(
                    ExerciseNote("1", "ex-0001", "견갑 후인하고 가슴 열기. 바는 명치 아래로.", "김트레이너", "2026-07-24"),
                    ExerciseNote("2", "ex-0001", "발 위치 고정, 아치 유지.", "김트레이너", "2026-07-10"),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 빈 상태", showBackground = true)
@Composable
private fun ExerciseNoteEmptyPreview() {
    MyFitTheme(darkTheme = false) {
        ExerciseNoteScreen(
            uiState = ExerciseNoteUiState(isLoading = false, exerciseName = "랫풀다운"),
            onAction = {},
        )
    }
}
