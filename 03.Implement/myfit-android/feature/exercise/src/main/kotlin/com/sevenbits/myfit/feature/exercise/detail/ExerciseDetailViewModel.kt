package com.sevenbits.myfit.feature.exercise.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseReference
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** SCR-EXR-002 종목 상세 상태 */
data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    val exercise: Exercise? = null,
    val bodyPartLabels: List<String> = emptyList(),
    /** 앱 기본 제공 자세 영상 (FN-EXR-017) */
    val defaultVideo: ExerciseReference? = null,
    /** 사용자가 등록한 링크만. 기본 제공 링크는 목록에 넣지 않는다. (FN-EXR-004) */
    val userReferences: List<ExerciseReference> = emptyList(),
    val ptNoteCount: Int = 0,
    val isOnline: Boolean = true,
)

sealed interface ExerciseDetailAction {
    data object OnBack : ExerciseDetailAction
    data object OnFavoriteToggle : ExerciseDetailAction
    data object OnAddReferenceClick : ExerciseDetailAction
    data class OnRemoveReference(val referenceId: String) : ExerciseDetailAction
    data object OnOpenPtNotes : ExerciseDetailAction
}

sealed interface ExerciseDetailEvent {
    data object NavigateBack : ExerciseDetailEvent
    data class OpenPtNotes(val exerciseId: String) : ExerciseDetailEvent
    data object RequestAddReference : ExerciseDetailEvent
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle[ARG_EXERCISE_ID]) {
        "exerciseId 인자가 없습니다"
    }

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ExerciseDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
        observeReferences()
    }

    private fun load() {
        viewModelScope.launch {
            val exercise = repository.findById(exerciseId)
            val bodyParts = repository.observeBodyParts().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    exercise = exercise,
                    // 코드가 아니라 표시명으로 바꿔서 전달한다 — 화면은 코드 체계를 몰라야 한다
                    bodyPartLabels = exercise?.primaryBodyParts.orEmpty().mapNotNull { code ->
                        bodyParts.firstOrNull { part -> part.code == code }?.name
                    },
                    defaultVideo = repository.findDefaultVideo(exerciseId),
                    ptNoteCount = repository.countNotes(exerciseId),
                )
            }
        }
    }

    private fun observeReferences() {
        repository.observeReferences(exerciseId)
            .onEach { references ->
                _uiState.update { state ->
                    state.copy(userReferences = references.filterNot { it.isDefault })
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ExerciseDetailAction) {
        when (action) {
            ExerciseDetailAction.OnBack -> emit(ExerciseDetailEvent.NavigateBack)

            ExerciseDetailAction.OnFavoriteToggle -> viewModelScope.launch {
                repository.toggleFavorite(exerciseId)
                _uiState.update { it.copy(exercise = repository.findById(exerciseId)) }
            }

            ExerciseDetailAction.OnAddReferenceClick ->
                emit(ExerciseDetailEvent.RequestAddReference)

            is ExerciseDetailAction.OnRemoveReference -> viewModelScope.launch {
                repository.removeUserReference(action.referenceId)
            }

            ExerciseDetailAction.OnOpenPtNotes ->
                emit(ExerciseDetailEvent.OpenPtNotes(exerciseId))
        }
    }

    private fun emit(event: ExerciseDetailEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}
