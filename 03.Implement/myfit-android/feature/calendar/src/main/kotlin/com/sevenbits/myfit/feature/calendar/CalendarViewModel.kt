package com.sevenbits.myfit.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.DailyWorkoutSummary
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** SCR-CAL-001 월간 캘린더 상태. (06_화면설계서 §3.17) */
data class CalendarUiState(
    val isLoading: Boolean = true,
    val yearMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    /** 일자(`yyyy-MM-dd`) → 요약 */
    val summaries: Map<String, DailyWorkoutSummary> = emptyMap(),
    val selectedDate: LocalDate? = null,
) {
    val monthLabel: String get() = "${yearMonth.year}년 ${yearMonth.monthValue}월"

    val monthlyLogCount: Int get() = summaries.values.sumOf { it.logCount }

    val monthlyVolumeKg: Double get() = summaries.values.sumOf { it.totalVolumeKg }

    val selectedSummary: DailyWorkoutSummary?
        get() = selectedDate?.let { summaries[it.toString()] }

    /**
     * 월 그리드에 그릴 날짜 목록. 일요일 시작 기준으로 앞뒤를 null 로 채운다.
     * null 칸은 이전·다음 달이라 마커를 그리지 않는다.
     */
    val gridDays: List<LocalDate?>
        get() {
            val first = yearMonth.atDay(1)
            // DayOfWeek 는 월=1 … 일=7. 일요일 시작 그리드에 맞추려면 일요일을 0 으로 본다.
            val leadingBlanks = first.dayOfWeek.value % DAYS_IN_WEEK
            val days = (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }
            val trailing = (DAYS_IN_WEEK - (leadingBlanks + days.size) % DAYS_IN_WEEK) % DAYS_IN_WEEK
            return List(leadingBlanks) { null } + days + List(trailing) { null }
        }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}

sealed interface CalendarAction {
    data object OnPreviousMonth : CalendarAction
    data object OnNextMonth : CalendarAction
    data class OnDateSelect(val date: LocalDate) : CalendarAction
    data object OnDismissDetail : CalendarAction
    data object OnOpenSelectedLog : CalendarAction
}

sealed interface CalendarEvent {
    /** 일자 선택 후 일지로 이동. 일지가 없으면 진입 시 생성된다 (FN-CAL-007) */
    data class OpenLog(val date: String) : CalendarEvent
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 월 이동 시 이전 구독을 끊는다 — 누적되면 화면이 옛 데이터로 덮인다 */
    private var observeJob: Job? = null

    init {
        observeMonth(_uiState.value.yearMonth)
    }

    private fun observeMonth(yearMonth: YearMonth) {
        observeJob?.cancel()
        observeJob = repository
            .observeDailySummaries(
                from = yearMonth.atDay(1).toString(),
                to = yearMonth.atEndOfMonth().toString(),
            )
            .onEach { list ->
                _uiState.update { state ->
                    state.copy(isLoading = false, summaries = list.associateBy { it.date })
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.OnPreviousMonth -> moveMonth(-1)
            CalendarAction.OnNextMonth -> moveMonth(1)

            is CalendarAction.OnDateSelect ->
                _uiState.update { it.copy(selectedDate = action.date) }

            CalendarAction.OnDismissDetail ->
                _uiState.update { it.copy(selectedDate = null) }

            CalendarAction.OnOpenSelectedLog -> {
                val date = _uiState.value.selectedDate ?: return
                viewModelScope.launch { _events.send(CalendarEvent.OpenLog(date.toString())) }
            }
        }
    }

    private fun moveMonth(delta: Long) {
        val next = _uiState.value.yearMonth.plusMonths(delta)
        _uiState.update { it.copy(yearMonth = next, isLoading = true, selectedDate = null) }
        observeMonth(next)
    }
}
