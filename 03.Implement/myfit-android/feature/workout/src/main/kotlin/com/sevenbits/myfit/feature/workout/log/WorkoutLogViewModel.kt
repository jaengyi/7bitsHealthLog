package com.sevenbits.myfit.feature.workout.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutStatus
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** SCR-WRK-001 운동일지 상세 상태. (06_화면설계서 §3.11) */
data class WorkoutLogUiState(
    val isLoading: Boolean = true,
    val date: String = LocalDate.now().toString(),
    val sessionNo: Int = 1,
    val sessionCount: Int = 1,
    val log: WorkoutLog? = null,
    val summary: SessionSummary = SessionSummary(),
    val memo: String = "",
    val pendingDeleteExerciseId: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && log?.exercises.isNullOrEmpty()
    val canStart: Boolean get() = !(log?.exercises.isNullOrEmpty())
}

sealed interface WorkoutLogAction {
    data class OnMemoChange(val value: String) : WorkoutLogAction
    data class OnExerciseClick(val logExerciseId: String) : WorkoutLogAction
    data class OnExerciseDeleteRequest(val logExerciseId: String) : WorkoutLogAction
    data object OnExerciseDeleteConfirm : WorkoutLogAction
    data object OnExerciseDeleteCancel : WorkoutLogAction
    data class OnExerciseReorder(val orderedIds: List<String>) : WorkoutLogAction
    data object OnAddExerciseClick : WorkoutLogAction
    data object OnStartWorkout : WorkoutLogAction
    data class OnSessionChange(val sessionNo: Int) : WorkoutLogAction
    data object OnBack : WorkoutLogAction
}

sealed interface WorkoutLogEvent {
    data class NavigateToSetInput(val logExerciseId: String) : WorkoutLogEvent
    data class NavigateToExercisePicker(val logId: String) : WorkoutLogEvent
    data class NavigateToSession(val logId: String) : WorkoutLogEvent
    data object NavigateBack : WorkoutLogEvent
}

/**
 * 운동일지 상세.
 *
 * 메모는 **저장 버튼 없이 즉시 영속화**한다. 입력마다 DB 를 때리면 부하가 커지므로
 * 300ms 디바운스를 걸되, UI 는 낙관적으로 즉시 갱신한다. (P5 / FN-WRK-031)
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class WorkoutLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val initialDate: String =
        savedStateHandle.get<String>(ARG_DATE) ?: LocalDate.now().toString()

    private val _uiState = MutableStateFlow(WorkoutLogUiState(date = initialDate))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<WorkoutLogEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var logId: String = ""

    init {
        viewModelScope.launch {
            logId = repository.getOrCreateLog(initialDate, _uiState.value.sessionNo)
            observeLog()
            _uiState.update {
                it.copy(sessionCount = repository.countSessionsOn(initialDate).coerceAtLeast(1))
            }
        }
        observeMemoInput()
    }

    private fun observeLog() {
        repository.observeLog(logId)
            .onEach { log ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        log = log,
                        // 사용자가 입력 중이면 DB 값으로 덮어쓰지 않는다
                        memo = if (it.memo.isBlank()) log?.memo.orEmpty() else it.memo,
                    )
                }
                refreshSummary()
            }
            .launchIn(viewModelScope)
    }

    private fun refreshSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(summary = repository.calcSummary(logId)) }
        }
    }

    private fun observeMemoInput() {
        _uiState
            .map { it.memo }
            .distinctUntilChanged()
            .drop(1) // 초기값은 저장하지 않는다
            .debounce(MEMO_DEBOUNCE_MS)
            .onEach { memo ->
                if (logId.isNotBlank()) repository.updateMemo(logId, memo)
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: WorkoutLogAction) {
        when (action) {
            is WorkoutLogAction.OnMemoChange ->
                _uiState.update { it.copy(memo = action.value) }

            is WorkoutLogAction.OnExerciseClick ->
                emit(WorkoutLogEvent.NavigateToSetInput(action.logExerciseId))

            is WorkoutLogAction.OnExerciseDeleteRequest ->
                _uiState.update { it.copy(pendingDeleteExerciseId = action.logExerciseId) }

            WorkoutLogAction.OnExerciseDeleteConfirm -> {
                val target = _uiState.value.pendingDeleteExerciseId ?: return
                viewModelScope.launch {
                    repository.removeLogExercise(target)
                    repository.refreshSummaryCache(logId)
                    _uiState.update { it.copy(pendingDeleteExerciseId = null) }
                    refreshSummary()
                }
            }

            WorkoutLogAction.OnExerciseDeleteCancel ->
                _uiState.update { it.copy(pendingDeleteExerciseId = null) }

            is WorkoutLogAction.OnExerciseReorder -> viewModelScope.launch {
                repository.reorderExercises(logId, action.orderedIds)
            }

            WorkoutLogAction.OnAddExerciseClick ->
                emit(WorkoutLogEvent.NavigateToExercisePicker(logId))

            WorkoutLogAction.OnStartWorkout -> viewModelScope.launch {
                repository.updateStatus(
                    logId,
                    WorkoutStatus.IN_PROGRESS.name,
                    System.currentTimeMillis(),
                )
                _events.send(WorkoutLogEvent.NavigateToSession(logId))
            }

            is WorkoutLogAction.OnSessionChange -> viewModelScope.launch {
                logId = repository.getOrCreateLog(_uiState.value.date, action.sessionNo)
                _uiState.update { it.copy(sessionNo = action.sessionNo, isLoading = true) }
                observeLog()
            }

            WorkoutLogAction.OnBack -> emit(WorkoutLogEvent.NavigateBack)
        }
    }

    /** 종목 선택 화면에서 돌아왔을 때 호출된다. */
    fun onExercisesSelected(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            repository.addExercises(logId, exerciseIds)
            refreshSummary()
        }
    }

    private fun emit(event: WorkoutLogEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        const val ARG_DATE = "date"
        private const val MEMO_DEBOUNCE_MS = 300L
    }
}
