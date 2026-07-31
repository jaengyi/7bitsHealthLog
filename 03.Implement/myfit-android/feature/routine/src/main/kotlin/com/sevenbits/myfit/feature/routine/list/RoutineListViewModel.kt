package com.sevenbits.myfit.feature.routine.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.Routine
import com.sevenbits.myfit.core.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** SCR-RTN-001 루틴 목록 상태. (06_화면설계서 §3.15) */
data class RoutineListUiState(
    val isLoading: Boolean = true,
    val routines: List<Routine> = emptyList(),
    val pendingDeleteId: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && routines.isEmpty()
}

sealed interface RoutineListAction {
    data class OnRoutineClick(val routineId: String) : RoutineListAction
    data class OnExecute(val routineId: String) : RoutineListAction
    data class OnDeleteRequest(val routineId: String) : RoutineListAction
    data object OnDeleteConfirm : RoutineListAction
    data object OnDeleteCancel : RoutineListAction
    data object OnCreate : RoutineListAction
}

sealed interface RoutineListEvent {
    data class OpenEditor(val routineId: String) : RoutineListEvent
    /** 인스턴스화 완료 — 생성된 일지로 이동한다 (FN-RTN-007) */
    data class OpenLog(val date: String) : RoutineListEvent
}

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val repository: RoutineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<RoutineListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        repository.observeRoutines()
            .onEach { routines ->
                _uiState.update { it.copy(isLoading = false, routines = routines) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: RoutineListAction) {
        when (action) {
            is RoutineListAction.OnRoutineClick ->
                emit(RoutineListEvent.OpenEditor(action.routineId))

            RoutineListAction.OnCreate -> emit(RoutineListEvent.OpenEditor(""))

            is RoutineListAction.OnExecute -> viewModelScope.launch {
                val today = LocalDate.now().toString()
                repository.instantiateToLog(action.routineId, today)
                _events.send(RoutineListEvent.OpenLog(today))
            }

            is RoutineListAction.OnDeleteRequest ->
                _uiState.update { it.copy(pendingDeleteId = action.routineId) }

            RoutineListAction.OnDeleteConfirm -> {
                val target = _uiState.value.pendingDeleteId ?: return
                viewModelScope.launch {
                    repository.deactivateRoutine(target)
                    _uiState.update { it.copy(pendingDeleteId = null) }
                }
            }

            RoutineListAction.OnDeleteCancel ->
                _uiState.update { it.copy(pendingDeleteId = null) }
        }
    }

    private fun emit(event: RoutineListEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
