package com.sevenbits.myfit.feature.routine.list

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.component.ConfirmDialog
import com.sevenbits.myfit.core.designsystem.component.EmptyState
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.Routine
import com.sevenbits.myfit.core.domain.model.RoutineExercise

/**
 * SCR-RTN-001 루틴 목록 (stateless). (06_화면설계서 §3.15)
 *
 * "실행"을 누르면 루틴이 오늘 일지로 인스턴스화된다. (FN-RTN-007)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    uiState: RoutineListUiState,
    onAction: (RoutineListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("루틴") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(RoutineListAction.OnCreate) }) {
                Icon(Icons.Default.Add, contentDescription = "루틴 만들기")
            }
        },
    ) { innerPadding ->
        if (uiState.isEmpty) {
            EmptyState(
                message = "자주 하는 운동 구성을 루틴으로 저장해 두면\n다음부터 한 번에 불러올 수 있습니다.",
                actionLabel = "첫 루틴 만들기",
                onAction = { onAction(RoutineListAction.OnCreate) },
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                items(uiState.routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onClick = { onAction(RoutineListAction.OnRoutineClick(routine.id)) },
                        onExecute = { onAction(RoutineListAction.OnExecute(routine.id)) },
                        onDelete = { onAction(RoutineListAction.OnDeleteRequest(routine.id)) },
                    )
                }
            }
        }

        if (uiState.pendingDeleteId != null) {
            ConfirmDialog(
                title = "루틴을 삭제할까요?",
                // 논리 삭제이므로 과거 기록은 그대로 남는다 (FN-RTN-004)
                message = "이 루틴으로 수행한 과거 기록은 그대로 남습니다.",
                confirmLabel = "삭제",
                onConfirm = { onAction(RoutineListAction.OnDeleteConfirm) },
                onDismiss = { onAction(RoutineListAction.OnDeleteCancel) },
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    onClick: () -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit,
) {
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
                    text = routine.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "루틴 삭제")
                }
            }

            Text(
                text = routine.summaryLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = listOfNotNull(
                    routine.lastPerformedDate?.let { "최근 $it" },
                    "${routine.useCount}회 수행",
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(
                    onClick = onExecute,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text("실행")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text("편집")
                }
            }
        }
    }
}

// ── Preview ──────────────────────────────────────────────

private fun sampleRoutine(id: String, name: String, first: String, count: Int) = Routine(
    id = id,
    name = name,
    useCount = count,
    lastPerformedDate = "2026-07-28",
    exercises = List(3) { index ->
        RoutineExercise(
            id = "$id-$index",
            exerciseId = "e$index",
            exerciseName = if (index == 0) first else "종목$index",
            orderNo = index + 1,
        )
    },
)

@Preview(name = "dark", showBackground = true)
@Composable
private fun RoutineListDarkPreview() {
    MyFitTheme(darkTheme = true) {
        RoutineListScreen(
            uiState = RoutineListUiState(
                isLoading = false,
                routines = listOf(
                    sampleRoutine("1", "가슴/삼두", "바벨 벤치프레스", 12),
                    sampleRoutine("2", "등/이두", "랫풀다운", 11),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 빈 상태", showBackground = true)
@Composable
private fun RoutineListEmptyPreview() {
    MyFitTheme(darkTheme = false) {
        RoutineListScreen(uiState = RoutineListUiState(isLoading = false), onAction = {})
    }
}
