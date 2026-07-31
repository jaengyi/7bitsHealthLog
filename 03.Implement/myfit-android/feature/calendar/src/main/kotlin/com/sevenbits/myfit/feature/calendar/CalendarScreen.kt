package com.sevenbits.myfit.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.DailyWorkoutSummary
import java.time.LocalDate
import java.time.YearMonth

/**
 * SCR-CAL-001 월간 캘린더 (stateless). (06_화면설계서 §3.17)
 *
 * 일자를 누르면 하단 시트로 요약이 뜨고 거기서 일지로 이동한다.
 * 기록이 없는 날도 선택할 수 있다 — 진입 시 일지가 생성된다. (FN-CAL-007)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.monthLabel) },
                navigationIcon = {
                    TextButton(onClick = { onAction(CalendarAction.OnPreviousMonth) }) { Text("‹") }
                },
                actions = {
                    TextButton(onClick = { onAction(CalendarAction.OnNextMonth) }) { Text("›") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.SpaceSm),
        ) {
            WeekdayHeader()

            uiState.gridDays.chunked(DAYS_IN_WEEK).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            summary = date?.let { uiState.summaries[it.toString()] },
                            isToday = date == uiState.today,
                            isSelected = date == uiState.selectedDate,
                            onClick = { date?.let { onAction(CalendarAction.OnDateSelect(it)) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpaceSm))

            Text(
                text = "이번 달  %d회 · %,.0f kg".format(
                    uiState.monthlyLogCount,
                    uiState.monthlyVolumeKg,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.SpaceSm),
            )
        }

        uiState.selectedDate?.let { selected ->
            DayDetailSheet(
                date = selected,
                summary = uiState.selectedSummary,
                onOpenLog = { onAction(CalendarAction.OnOpenSelectedLog) },
                onDismiss = { onAction(CalendarAction.OnDismissDetail) },
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAY_LABELS.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    summary: DailyWorkoutSummary?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (date == null) return@Box

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            // 수행 마커 — 기록이 있는 날만 채운다. 없는 날도 같은 크기를 차지해
            // 날짜 숫자가 위아래로 흔들리지 않는다. (FN-CAL-002)
            Box(
                modifier = Modifier
                    .size(MARKER_SIZE)
                    .then(
                        if (summary != null && summary.logCount > 0) {
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    date: LocalDate,
    summary: DailyWorkoutSummary?,
    onOpenLog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Text(date.toString(), style = MaterialTheme.typography.titleLarge)

            if (summary != null) {
                Text(
                    text = "%d회 · %,.0f kg".format(summary.logCount, summary.totalVolumeKg),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                // 기록이 없어도 막다른 화면을 만들지 않는다
                Text(
                    text = "기록이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onOpenLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(if (summary != null) "일지 열기" else "이 날짜에 기록하기")
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7
private val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")
private val MARKER_SIZE = 6.dp

// ── Preview ──────────────────────────────────────────────

@Preview(name = "dark", showBackground = true)
@Composable
private fun CalendarDarkPreview() {
    val month = YearMonth.of(2026, 7)
    MyFitTheme(darkTheme = true) {
        CalendarScreen(
            uiState = CalendarUiState(
                isLoading = false,
                yearMonth = month,
                today = month.atDay(30),
                summaries = listOf(2, 4, 7, 9, 11, 14, 16, 18, 21, 23, 25, 28)
                    .associate { day ->
                        val date = month.atDay(day).toString()
                        date to DailyWorkoutSummary(date, 1, 8000.0 + day * 100)
                    },
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 빈 달", showBackground = true)
@Composable
private fun CalendarEmptyPreview() {
    MyFitTheme(darkTheme = false) {
        CalendarScreen(
            uiState = CalendarUiState(isLoading = false, yearMonth = YearMonth.of(2026, 8)),
            onAction = {},
        )
    }
}
