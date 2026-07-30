package com.sevenbits.myfit.feature.exercise.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.MovementType
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-EXR-003 종목 등록/편집 상태. (FN-EXR-005 ~ 007)
 */
data class ExerciseEditorUiState(
    val isLoading: Boolean = true,
    /** 편집 모드면 기존 종목 id */
    val exerciseId: String = "",
    val name: String = "",
    val nameEn: String = "",
    val recordType: RecordType = RecordType.WEIGHT_REPS,
    val exerciseType: ExerciseType = ExerciseType.WEIGHT,
    val equipment: Equipment = Equipment.BARBELL,
    val movementType: MovementType = MovementType.COMPOUND,
    val primaryBodyParts: Set<String> = emptySet(),
    val secondaryBodyParts: Set<String> = emptySet(),
    val defaultRestSec: Int? = null,
    val description: String = "",
    val caution: String = "",
    val availableBodyParts: List<BodyPart> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEditMode: Boolean get() = exerciseId.isNotBlank()

    /**
     * 기록 유형은 **등록 후 변경할 수 없다.**
     * 이미 쌓인 세트의 필드 구성과 불일치가 생기기 때문이다. (REQ-EXR-007)
     */
    val isRecordTypeEditable: Boolean get() = !isEditMode

    val canSave: Boolean get() = name.isNotBlank() && primaryBodyParts.isNotEmpty()
}

sealed interface ExerciseEditorAction {
    data object OnBack : ExerciseEditorAction
    data class OnNameChange(val value: String) : ExerciseEditorAction
    data class OnNameEnChange(val value: String) : ExerciseEditorAction
    data class OnRecordTypeChange(val value: RecordType) : ExerciseEditorAction
    data class OnExerciseTypeChange(val value: ExerciseType) : ExerciseEditorAction
    data class OnEquipmentChange(val value: Equipment) : ExerciseEditorAction
    data class OnMovementTypeChange(val value: MovementType) : ExerciseEditorAction
    data class OnPrimaryToggle(val code: String) : ExerciseEditorAction
    data class OnSecondaryToggle(val code: String) : ExerciseEditorAction
    data class OnRestSecChange(val value: Int?) : ExerciseEditorAction
    data class OnDescriptionChange(val value: String) : ExerciseEditorAction
    data class OnCautionChange(val value: String) : ExerciseEditorAction
    data object OnSave : ExerciseEditorAction
    data object OnDelete : ExerciseEditorAction
}

sealed interface ExerciseEditorEvent {
    data object Saved : ExerciseEditorEvent
    /** 기록이 있어 논리 삭제된 경우 사용자에게 알린다 (FN-EXR-007) */
    data class Deleted(val wasDeactivated: Boolean) : ExerciseEditorEvent
    data object NavigateBack : ExerciseEditorEvent
}

@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val editingId: String = savedStateHandle.get<String>(ARG_EXERCISE_ID).orEmpty()

    private val _uiState = MutableStateFlow(ExerciseEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ExerciseEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        repository.observeBodyParts()
            .onEach { parts -> _uiState.update { it.copy(availableBodyParts = parts) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            if (editingId.isBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val exercise = repository.findById(editingId)
            _uiState.update { state ->
                if (exercise == null) {
                    state.copy(isLoading = false, errorMessage = "종목을 찾을 수 없습니다")
                } else {
                    state.copy(
                        isLoading = false,
                        exerciseId = exercise.id,
                        name = exercise.name,
                        nameEn = exercise.nameEn.orEmpty(),
                        recordType = exercise.recordType,
                        exerciseType = exercise.exerciseType,
                        equipment = exercise.equipment,
                        movementType = exercise.movementType,
                        primaryBodyParts = exercise.primaryBodyParts.toSet(),
                        secondaryBodyParts = exercise.secondaryBodyParts.toSet(),
                        defaultRestSec = exercise.defaultRestSec,
                        description = exercise.description.orEmpty(),
                        caution = exercise.caution.orEmpty(),
                    )
                }
            }
        }
    }

    fun onAction(action: ExerciseEditorAction) {
        when (action) {
            is ExerciseEditorAction.OnNameChange ->
                _uiState.update { it.copy(name = action.value, errorMessage = null) }

            is ExerciseEditorAction.OnNameEnChange ->
                _uiState.update { it.copy(nameEn = action.value) }

            is ExerciseEditorAction.OnRecordTypeChange ->
                _uiState.update { it.copy(recordType = action.value) }

            is ExerciseEditorAction.OnExerciseTypeChange ->
                _uiState.update { it.copy(exerciseType = action.value) }

            is ExerciseEditorAction.OnEquipmentChange ->
                _uiState.update { it.copy(equipment = action.value) }

            is ExerciseEditorAction.OnMovementTypeChange ->
                _uiState.update { it.copy(movementType = action.value) }

            is ExerciseEditorAction.OnPrimaryToggle ->
                _uiState.update {
                    it.copy(primaryBodyParts = it.primaryBodyParts.toggle(action.code))
                }

            is ExerciseEditorAction.OnSecondaryToggle ->
                _uiState.update {
                    it.copy(secondaryBodyParts = it.secondaryBodyParts.toggle(action.code))
                }

            is ExerciseEditorAction.OnRestSecChange ->
                _uiState.update { it.copy(defaultRestSec = action.value) }

            is ExerciseEditorAction.OnDescriptionChange ->
                _uiState.update { it.copy(description = action.value) }

            is ExerciseEditorAction.OnCautionChange ->
                _uiState.update { it.copy(caution = action.value) }

            ExerciseEditorAction.OnSave -> save()
            ExerciseEditorAction.OnDelete -> delete()
            ExerciseEditorAction.OnBack -> emit(ExerciseEditorEvent.NavigateBack)
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            runCatching {
                repository.upsertUserExercise(
                    Exercise(
                        id = state.exerciseId,
                        name = state.name.trim(),
                        nameEn = state.nameEn.trim().takeIf { it.isNotBlank() },
                        recordType = state.recordType,
                        exerciseType = state.exerciseType,
                        equipment = state.equipment,
                        movementType = state.movementType,
                        description = state.description.trim().takeIf { it.isNotBlank() },
                        caution = state.caution.trim().takeIf { it.isNotBlank() },
                        defaultRestSec = state.defaultRestSec,
                        isUserDefined = true,
                        primaryBodyParts = state.primaryBodyParts.toList(),
                        secondaryBodyParts = state.secondaryBodyParts.toList(),
                    ),
                )
            }.onSuccess {
                emit(ExerciseEditorEvent.Saved)
            }.onFailure { error ->
                // 중복명 등 도메인 규칙 위반은 화면에 인라인으로 표시한다
                _uiState.update { it.copy(errorMessage = error.message ?: "저장에 실패했습니다") }
            }
        }
    }

    private fun delete() {
        val id = _uiState.value.exerciseId
        if (id.isBlank()) return
        viewModelScope.launch {
            val deactivated = repository.deleteOrDeactivate(id)
            emit(ExerciseEditorEvent.Deleted(deactivated))
        }
    }

    private fun emit(event: ExerciseEditorEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun Set<String>.toggle(value: String): Set<String> =
        if (value in this) this - value else this + value

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}
