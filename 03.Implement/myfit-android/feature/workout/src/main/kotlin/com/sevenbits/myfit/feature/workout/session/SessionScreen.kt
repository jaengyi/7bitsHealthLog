package com.sevenbits.myfit.feature.workout.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.designsystem.component.ConfirmDialog
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.calculator.RestTimerCalculator
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutLogExercise
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.ui.RestTimerPanel

/**
 * SCR-WRK-003 운동 수행 (stateless). (06_화면설계서 §3.13)
 *
 * 운동 중에는 화면을 대충 보고 누른다. 그래서 이 화면은 **체크와 타이머만** 다루고
 * 값 편집은 세트 입력 화면으로 넘긴다.
 */
@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onAction: (SessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn()

    Scaffold(
        modifier = modifier,
        bottomBar = { SessionBottomBar(uiState, onAction) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SessionHeader(uiState, onAction)

            // 타이머가 돌 때만 자리를 차지한다. 항상 띄우면 세트 목록이 밀린다.
            if (uiState.restTimer.isRunning) {
                Surface(tonalElevation = Dimens.SpaceXs) {
                    RestTimerPanel(
                        remainingDisplay = RestTimerCalculator.formatRemaining(
                            uiState.restTimer.remainingMillis,
                        ),
                        progress = uiState.restTimer.progress,
                        isPaused = uiState.restTimer.isPaused,
                        onAdjust = { onAction(SessionAction.OnTimerAdjust(it)) },
                        onTogglePause = { onAction(SessionAction.OnTimerTogglePause) },
                        onSkip = { onAction(SessionAction.OnTimerSkip) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                val current = uiState.current
                if (current == null) {
                    Text(
                        text = "수행할 종목이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = Dimens.SpaceXl),
                    )
                } else {
                    CurrentExerciseSection(
                        exercise = current,
                        nextSetId = uiState.nextSetId,
                        weightUnit = uiState.weightUnit,
                        onToggleSet = { onAction(SessionAction.OnToggleSet(it)) },
                        onEdit = { onAction(SessionAction.OnEditCurrentExercise) },
                    )
                    uiState.next?.let { NextExerciseSection(it, uiState.weightUnit) }
                }
            }
        }
    }

    if (uiState.showFinishConfirm) {
        val remaining = uiState.totalSetCount - uiState.completedSetCount
        ConfirmDialog(
            title = "운동을 종료할까요?",
            message = if (remaining > 0) {
                "$remaining 세트가 남아 있습니다. 종료하면 기록은 그대로 저장됩니다."
            } else {
                "모든 세트를 마쳤습니다."
            },
            confirmLabel = "종료",
            onConfirm = { onAction(SessionAction.OnFinishConfirm) },
            onDismiss = { onAction(SessionAction.OnFinishCancel) },
        )
    }
}

/**
 * 수행 중에는 화면이 꺼지지 않게 한다. (FN-WRK-022)
 *
 * 화면을 벗어나면 반드시 해제한다 — 켜 둔 채로 나가면 배터리를 계속 먹는다.
 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun SessionHeader(uiState: SessionUiState, onAction: (SessionAction) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { onAction(SessionAction.OnClose) }) { Text("✕") }
            Text(
                text = "${uiState.completedSetCount} / ${uiState.totalSetCount}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${(uiState.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
    }
}

@Composable
private fun CurrentExerciseSection(
    exercise: WorkoutLogExercise,
    nextSetId: String?,
    weightUnit: WeightUnit,
    onToggleSet: (String) -> Unit,
    onEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "현재",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(exercise.exerciseName, style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = onEdit) { Text("값 수정") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                exercise.sets.forEachIndexed { index, set ->
                    if (index > 0) HorizontalDivider()
                    SessionSetRow(
                        set = set,
                        isNext = set.id == nextSetId,
                        weightUnit = weightUnit,
                        onToggle = { onToggleSet(set.id) },
                    )
                }
                if (exercise.sets.isEmpty()) {
                    Text(
                        text = "세트가 없습니다. 값 수정에서 추가하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(Dimens.ScreenPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionSetRow(
    set: WorkoutSet,
    isNext: Boolean,
    weightUnit: WeightUnit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            // 터치 타겟을 크게 잡는다. 운동 중 오조작이 가장 잦은 조작이다 (U2)
            .heightIn(min = Dimens.StepperButton)
            .padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(
            text = "${set.setNo}세트",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = set.summaryDisplay(weightUnit),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (!set.isWorkingSet) {
            Text(
                text = "웜업",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = set.isCompleted, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun NextExerciseSection(exercise: WorkoutLogExercise, weightUnit: WeightUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = "다음",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
            val first = exercise.sets.firstOrNull()
            Text(
                text = listOfNotNull(
                    first?.summaryDisplay(weightUnit)?.takeIf { it != "-" },
                    "${exercise.sets.size}세트",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionBottomBar(uiState: SessionUiState, onAction: (SessionAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            OutlinedButton(
                onClick = { onAction(SessionAction.OnPreviousExercise) },
                enabled = uiState.hasPrevious,
                modifier = Modifier.weight(1f),
            ) { Text("이전 종목") }
            OutlinedButton(
                onClick = { onAction(SessionAction.OnNextExercise) },
                enabled = uiState.hasNext,
                modifier = Modifier.weight(1f),
            ) { Text("다음 종목") }
        }
        Button(
            onClick = { onAction(SessionAction.OnFinishRequest) },
            enabled = !uiState.isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) { Text("운동 종료") }
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun SessionPreview() {
    val sets = listOf(
        WorkoutSet(id = "1", setNo = 1, weightKg = 40.0, reps = 12, isCompleted = true),
        WorkoutSet(id = "2", setNo = 2, weightKg = 60.0, reps = 10, isCompleted = true),
        WorkoutSet(id = "3", setNo = 3, weightKg = 80.0, reps = 8),
    )
    MyFitTheme(darkTheme = true) {
        SessionScreen(
            uiState = SessionUiState(
                isLoading = false,
                log = WorkoutLog(
                    workoutDate = "2026-07-30",
                    exercises = listOf(
                        WorkoutLogExercise(
                            id = "e1",
                            exerciseId = "x",
                            exerciseName = "벤치프레스",
                            sets = sets,
                        ),
                        WorkoutLogExercise(
                            id = "e2",
                            exerciseId = "y",
                            exerciseName = "인클라인 덤벨프레스",
                            sets = listOf(WorkoutSet(id = "4", weightKg = 22.5, reps = 10)),
                        ),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
