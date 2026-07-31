package com.sevenbits.myfit.feature.routine.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.Routine
import com.sevenbits.myfit.core.domain.model.RoutineExercise
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import com.sevenbits.myfit.core.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** SCR-RTN-002 루틴 편집 상태. (06_화면설계서 §3.16) */
data class RoutineEditorUiState(
    val isLoading: Boolean = true,
    val routineId: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<RoutineExercise> = emptyList(),
) {
    val isEditMode: Boolean get() = routineId.isNotBlank()
    val canSave: Boolean get() = name.isNotBlank() && exercises.isNotEmpty()
}

sealed interface RoutineEditorAction {
    data class OnNameChange(val value: String) : RoutineEditorAction
    data class OnDescriptionChange(val value: String) : RoutineEditorAction
    data object OnAddExercise : RoutineEditorAction
    data class OnRemoveExercise(val routineExerciseId: String) : RoutineEditorAction
    data class OnTargetSetsChange(val id: String, val value: Int?) : RoutineEditorAction
    data class OnTargetWeightChange(val id: String, val value: Double?) : RoutineEditorAction
    data class OnTargetPercentChange(val id: String, val value: Double?) : RoutineEditorAction
    data class OnTargetRepsChange(val id: String, val value: Int?) : RoutineEditorAction
    data object OnSave : RoutineEditorAction
    data object OnBack : RoutineEditorAction
}

sealed interface RoutineEditorEvent {
    data object Saved : RoutineEditorEvent
    data object NavigateBack : RoutineEditorEvent
    data object RequestExercisePicker : RoutineEditorEvent
}

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val editingId: String = savedStateHandle.get<String>(ARG_ROUTINE_ID).orEmpty()

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<RoutineEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (editingId.isBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val routine = routineRepository.findRoutine(editingId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    routineId = routine?.id.orEmpty(),
                    name = routine?.name.orEmpty(),
                    description = routine?.description.orEmpty(),
                    exercises = routine?.exercises.orEmpty(),
                )
            }
        }
    }

    fun onAction(action: RoutineEditorAction) {
        when (action) {
            is RoutineEditorAction.OnNameChange ->
                _uiState.update { it.copy(name = action.value) }

            is RoutineEditorAction.OnDescriptionChange ->
                _uiState.update { it.copy(description = action.value) }

            RoutineEditorAction.OnAddExercise ->
                emit(RoutineEditorEvent.RequestExercisePicker)

            is RoutineEditorAction.OnRemoveExercise ->
                _uiState.update { state ->
                    state.copy(exercises = state.exercises.filterNot { it.id == action.routineExerciseId })
                }

            is RoutineEditorAction.OnTargetSetsChange ->
                editExercise(action.id) { it.copy(targetSetCount = action.value) }

            is RoutineEditorAction.OnTargetWeightChange ->
                // 절대 중량을 지정하면 %1RM 은 해제한다 — 둘은 택일이다 (FN-RTN-006)
                editExercise(action.id) {
                    it.copy(targetWeightKg = action.value, targetPercentOneRm = null)
                }

            is RoutineEditorAction.OnTargetPercentChange ->
                editExercise(action.id) {
                    it.copy(targetPercentOneRm = action.value, targetWeightKg = null)
                }

            is RoutineEditorAction.OnTargetRepsChange ->
                editExercise(action.id) { it.copy(targetReps = action.value) }

            RoutineEditorAction.OnSave -> save()

            RoutineEditorAction.OnBack -> emit(RoutineEditorEvent.NavigateBack)
        }
    }

    /** 종목 선택 화면에서 돌아왔을 때 호출된다. */
    fun onExercisesSelected(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            val added = exerciseIds.mapIndexedNotNull { index, exerciseId ->
                val exercise = exerciseRepository.findById(exerciseId) ?: return@mapIndexedNotNull null
                RoutineExercise(
                    // 저장 전까지는 임시 id — 저장 시 실제 id 가 부여된다
                    id = "temp-$exerciseId-$index",
                    exerciseId = exerciseId,
                    exerciseName = exercise.name,
                    recordType = exercise.recordType,
                    orderNo = _uiState.value.exercises.size + index + 1,
                    restSec = exercise.defaultRestSec,
                )
            }
            _uiState.update { it.copy(exercises = it.exercises + added) }
        }
    }

    private fun editExercise(id: String, transform: (RoutineExercise) -> RoutineExercise) {
        _uiState.update { state ->
            state.copy(exercises = state.exercises.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            routineRepository.upsertRoutine(
                Routine(
                    id = state.routineId,
                    name = state.name.trim(),
                    description = state.description.trim().takeIf { it.isNotBlank() },
                    exercises = state.exercises.mapIndexed { index, item ->
                        // 임시 id 는 비워서 저장 계층이 새 id 를 발급하게 한다
                        item.copy(
                            id = item.id.takeUnless { it.startsWith(TEMP_ID_PREFIX) }.orEmpty(),
                            orderNo = index + 1,
                        )
                    },
                ),
            )
            _events.send(RoutineEditorEvent.Saved)
        }
    }

    private fun emit(event: RoutineEditorEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
        private const val TEMP_ID_PREFIX = "temp-"
    }
}
