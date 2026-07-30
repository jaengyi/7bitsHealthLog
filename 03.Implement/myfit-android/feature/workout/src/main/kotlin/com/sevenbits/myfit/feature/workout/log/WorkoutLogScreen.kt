package com.sevenbits.myfit.feature.workout.log

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sevenbits.myfit.core.designsystem.component.ConfirmDialog
import com.sevenbits.myfit.core.designsystem.component.EmptyState
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutLogExercise
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.ui.ExerciseCard

/**
 * SCR-WRK-001 운동일지 상세 (stateless). (06_화면설계서 §3.11)
 *
 * 앱의 중심 화면이다. 종목 카드는 세트를 펼치지 않아도 수행 내역이 보이고,
 * 하단에는 실시간 요약과 운동 시작 버튼이 고정된다. (U1)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    uiState: WorkoutLogUiState,
    onAction: (WorkoutLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.date) },
                navigationIcon = {
                    TextButton(onClick = { onAction(WorkoutLogAction.OnBack) }) { Text("←") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            uiState.log?.routineName?.let { routine ->
                Text(
                    text = "루틴: $routine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                )
            }

            OutlinedTextField(
                value = uiState.memo,
                onValueChange = { onAction(WorkoutLogAction.OnMemoChange(it)) },
                label = { Text("오늘 컨디션 · 특이사항") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
            )

            if (uiState.isEmpty) {
                EmptyState(
                    message = "오늘의 첫 종목을 추가해 보세요",
                    actionLabel = "종목 추가",
                    onAction = { onAction(WorkoutLogAction.OnAddExerciseClick) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Dimens.ScreenPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    items(uiState.log?.exercises.orEmpty(), key = { it.id }) { exercise ->
                        ExerciseCard(
                            orderNo = exercise.orderNo,
                            exerciseName = exercise.exerciseName,
                            setSummary = exercise.setSummary(),
                            setCountDisplay = "${exercise.sets.size}세트",
                            volumeDisplay = exercise.volumeDisplay(),
                            hasMemo = !exercise.memo.isNullOrBlank(),
                            supersetLabel = exercise.supersetGroup?.let { "슈퍼세트 $it" },
                            onClick = { onAction(WorkoutLogAction.OnExerciseClick(exercise.id)) },
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { onAction(WorkoutLogAction.OnAddExerciseClick) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.MinTouchTarget),
                        ) {
                            Text("＋ 종목 추가")
                        }
                    }
                }
            }

            HorizontalDivider()

            // 하단 고정 — 실시간 요약 + 시작 (U1 / FN-WRK-027)
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier.padding(Dimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    Text(
                        text = uiState.summary.headline(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onAction(WorkoutLogAction.OnStartWorkout) },
                        enabled = uiState.canStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Text("▶  운동 시작")
                    }
                }
            }
        }

        // 파괴적 동작 확인 (U4)
        if (uiState.pendingDeleteExerciseId != null) {
            ConfirmDialog(
                title = "종목을 삭제할까요?",
                message = "이 종목의 세트 기록이 함께 삭제됩니다.",
                confirmLabel = "삭제",
                onConfirm = { onAction(WorkoutLogAction.OnExerciseDeleteConfirm) },
                onDismiss = { onAction(WorkoutLogAction.OnExerciseDeleteCancel) },
            )
        }
    }
}

/** `60×10  80×8  80×8` 형태의 한 줄 요약 */
private fun WorkoutLogExercise.setSummary(): String =
    sets.filter { it.weightKg != null || it.reps != null }
        .joinToString("  ") { set ->
            val w = set.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
            listOfNotNull(w, set.reps?.toString()).joinToString("×")
        }

/** 웜업을 제외한 볼륨 (FN-WRK-013) */
private fun WorkoutLogExercise.volumeDisplay(): String {
    val volume = sets.filter { it.setType != SetType.WARMUP }
        .sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
    return "%,.0f kg".format(volume)
}

private fun SessionSummary.headline(): String =
    "총 ${totalSetCount}세트 · %,.0f kg".format(totalVolumeKg)

// ── Preview ──────────────────────────────────────────────

private fun previewExercise(order: Int, name: String, sets: List<WorkoutSet>) =
    WorkoutLogExercise(
        id = "le$order",
        exerciseId = "e$order",
        exerciseName = name,
        orderNo = order,
        sets = sets,
    )

@Preview(name = "dark", showBackground = true)
@Composable
private fun WorkoutLogDarkPreview() {
    MyFitTheme(darkTheme = true) {
        WorkoutLogScreen(
            uiState = WorkoutLogUiState(
                isLoading = false,
                date = "2026-07-30",
                memo = "오늘 컨디션 좋음",
                log = WorkoutLog(
                    id = "log1",
                    workoutDate = "2026-07-30",
                    routineName = "가슴/삼두",
                    exercises = listOf(
                        previewExercise(
                            1,
                            "바벨 벤치프레스",
                            listOf(
                                WorkoutSet(id = "1", setNo = 1, weightKg = 60.0, reps = 10),
                                WorkoutSet(id = "2", setNo = 2, weightKg = 80.0, reps = 8),
                            ),
                        ),
                        previewExercise(
                            2,
                            "인클라인 덤벨 프레스",
                            listOf(WorkoutSet(id = "3", setNo = 1, weightKg = 22.5, reps = 10)),
                        ),
                    ),
                ),
                summary = SessionSummary(totalVolumeKg = 1465.0, totalSetCount = 3),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 빈 일지", showBackground = true)
@Composable
private fun WorkoutLogEmptyPreview() {
    MyFitTheme(darkTheme = false) {
        WorkoutLogScreen(
            uiState = WorkoutLogUiState(
                isLoading = false,
                date = "2026-07-31",
                log = WorkoutLog(id = "log2", workoutDate = "2026-07-31"),
            ),
            onAction = {},
        )
    }
}
