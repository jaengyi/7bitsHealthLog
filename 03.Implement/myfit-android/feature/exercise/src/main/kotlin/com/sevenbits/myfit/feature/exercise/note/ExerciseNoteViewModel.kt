package com.sevenbits.myfit.feature.exercise.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.ExerciseNote
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * SCR-EXR-004 PT 학습 노트 상태. (REQ-EXR-008)
 *
 * PT 세션에서 배운 자세 큐잉·주의점을 종목별로 누적하고,
 * 운동 수행 화면에서 즉시 열람하는 것이 목적이다.
 */
data class ExerciseNoteUiState(
    val isLoading: Boolean = true,
    val exerciseName: String = "",
    val notes: List<ExerciseNote> = emptyList(),
    /** 작성·수정 중인 노트. null 이면 편집기 닫힘 */
    val editing: NoteDraft? = null,
)

/** 작성 중인 노트. 저장 전까지 DB 에 반영하지 않는다. */
data class NoteDraft(
    val id: String = "",
    val content: String = "",
    val trainerName: String = "",
    val noteDate: String = LocalDate.now().toString(),
) {
    val isNew: Boolean get() = id.isBlank()
    val canSave: Boolean get() = content.isNotBlank()
}

sealed interface ExerciseNoteAction {
    data object OnBack : ExerciseNoteAction
    data object OnAddClick : ExerciseNoteAction
    data class OnEditClick(val noteId: String) : ExerciseNoteAction
    data class OnContentChange(val value: String) : ExerciseNoteAction
    data class OnTrainerChange(val value: String) : ExerciseNoteAction
    data class OnDateChange(val value: String) : ExerciseNoteAction
    data object OnSave : ExerciseNoteAction
    data object OnCancelEdit : ExerciseNoteAction
    data class OnDelete(val noteId: String) : ExerciseNoteAction
}

@HiltViewModel
class ExerciseNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle[ARG_EXERCISE_ID]) {
        "exerciseId 인자가 없습니다"
    }

    private val _uiState = MutableStateFlow(ExerciseNoteUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.findById(exerciseId)
            _uiState.update { it.copy(exerciseName = exercise?.name.orEmpty(), isLoading = false) }
        }
        repository.observeNotes(exerciseId)
            .onEach { notes -> _uiState.update { it.copy(notes = notes, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ExerciseNoteAction) {
        when (action) {
            ExerciseNoteAction.OnAddClick ->
                _uiState.update { it.copy(editing = NoteDraft()) }

            is ExerciseNoteAction.OnEditClick -> {
                val note = _uiState.value.notes.firstOrNull { it.id == action.noteId } ?: return
                _uiState.update {
                    it.copy(
                        editing = NoteDraft(
                            id = note.id,
                            content = note.content,
                            trainerName = note.trainerName.orEmpty(),
                            noteDate = note.noteDate,
                        ),
                    )
                }
            }

            is ExerciseNoteAction.OnContentChange ->
                _uiState.update { it.copy(editing = it.editing?.copy(content = action.value)) }

            is ExerciseNoteAction.OnTrainerChange ->
                _uiState.update { it.copy(editing = it.editing?.copy(trainerName = action.value)) }

            is ExerciseNoteAction.OnDateChange ->
                _uiState.update { it.copy(editing = it.editing?.copy(noteDate = action.value)) }

            ExerciseNoteAction.OnSave -> {
                val draft = _uiState.value.editing ?: return
                if (!draft.canSave) return
                viewModelScope.launch {
                    repository.upsertNote(
                        ExerciseNote(
                            id = draft.id,
                            exerciseId = exerciseId,
                            content = draft.content.trim(),
                            trainerName = draft.trainerName.trim().takeIf { it.isNotBlank() },
                            noteDate = draft.noteDate,
                        ),
                    )
                    _uiState.update { it.copy(editing = null) }
                }
            }

            ExerciseNoteAction.OnCancelEdit ->
                _uiState.update { it.copy(editing = null) }

            is ExerciseNoteAction.OnDelete -> viewModelScope.launch {
                repository.deleteNote(action.noteId)
            }

            ExerciseNoteAction.OnBack -> Unit // 네비게이션은 Route 계층이 처리한다
        }
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}
