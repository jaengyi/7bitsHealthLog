package com.sevenbits.myfit.feature.workout.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * SCR-WRK-004 세션 요약 (stateless). (06_화면설계서 §3.14)
 *
 * 운동 직후에 보는 화면이다. 숫자를 나열하기보다 "오늘 뭘 했는지"가 한눈에 들어와야 한다.
 */
@Composable
fun SessionSummaryScreen(
    uiState: SessionSummaryUiState,
    onAction: (SessionSummaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                OutlinedButton(
                    onClick = { onAction(SessionSummaryAction.OnSaveRoutineRequest) },
                    enabled = uiState.savedRoutineName == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(
                        uiState.savedRoutineName?.let { "루틴 '$it' 저장됨" }
                            ?: "이 구성을 루틴으로 저장",
                    )
                }
                Button(
                    onClick = { onAction(SessionSummaryAction.OnConfirm) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) { Text("확인") }
            }
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
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text("운동 완료 🎉", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = listOfNotNull(
                        uiState.log?.workoutDate?.toDisplayDate(),
                        uiState.durationText,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MetricRow(uiState)

            if (uiState.bodyPartShares.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Text("부위별 세트 비중", style = MaterialTheme.typography.titleMedium)
                    uiState.bodyPartShares.forEach { share -> BodyPartBar(share) }
                }
            }

            if (!uiState.log?.exercises.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Text("수행 종목", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            uiState.log?.exercises?.forEachIndexed { index, exercise ->
                                if (index > 0) HorizontalDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.ScreenPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = exercise.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "${exercise.sets.count { it.isCompleted }}" +
                                            " / ${exercise.sets.size}세트",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSaveRoutineDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SessionSummaryAction.OnSaveRoutineCancel) },
            title = { Text("루틴으로 저장") },
            text = {
                OutlinedTextField(
                    value = uiState.routineNameInput,
                    onValueChange = { onAction(SessionSummaryAction.OnRoutineNameChange(it)) },
                    label = { Text("루틴 이름") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(SessionSummaryAction.OnSaveRoutineConfirm) },
                    enabled = uiState.routineNameInput.isNotBlank(),
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SessionSummaryAction.OnSaveRoutineCancel) }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun MetricRow(uiState: SessionSummaryUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpaceMd),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Metric("총 볼륨", uiState.volumeDisplay)
            Metric("총 세트", "${uiState.summary.totalSetCount}")
            Metric("총 횟수", "${uiState.summary.totalReps}")
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BodyPartBar(share: BodyPartShare) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(share.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${share.setCount}세트 (${share.ratio.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (share.ratio / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** `2026-07-30` → `7월 30일 (목)` */
private fun String.toDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
}.getOrDefault(this)

@Preview(name = "dark", showBackground = true)
@Composable
private fun SessionSummaryPreview() {
    MyFitTheme(darkTheme = true) {
        SessionSummaryScreen(
            uiState = SessionSummaryUiState(
                isLoading = false,
                log = WorkoutLog(
                    workoutDate = "2026-07-30",
                    startedAtEpochMillis = 0L,
                    endedAtEpochMillis = 72L * 60_000L,
                ),
                summary = SessionSummary(
                    totalVolumeKg = 8420.0,
                    totalSetCount = 21,
                    totalReps = 186,
                    bodyPartRatio = mapOf("CHEST" to 57.0, "TRICEPS" to 29.0, "SHOULDER" to 14.0),
                ),
                bodyPartShares = listOf(
                    BodyPartShare("가슴", 57.0, 12),
                    BodyPartShare("삼두", 29.0, 6),
                    BodyPartShare("어깨", 14.0, 3),
                ),
            ),
            onAction = {},
        )
    }
}
