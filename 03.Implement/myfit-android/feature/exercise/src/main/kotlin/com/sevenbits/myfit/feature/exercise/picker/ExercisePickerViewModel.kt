package com.sevenbits.myfit.feature.exercise.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.ExerciseFilter
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import com.sevenbits.myfit.core.domain.usecase.exercise.SearchExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 검색·필터 조건 묶음 — 디바운스 대상 */
private data class SearchInput(
    val query: String,
    val bodyParts: Set<String>,
    val equipments: Set<Equipment>,
)

/**
 * SCR-EXR-001 종목 선택. (06_화면설계서 §3.7)
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    private val searchExercises: SearchExercisesUseCase,
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExercisePickerUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ExercisePickerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadInitialSections()
        observeSearchInput()
    }

    /**
     * 검색 입력 디바운스.
     *
     * 매 타이핑마다 조회하면 결과가 깜빡이고 DB 부하가 커진다.
     * 200ms 디바운스 + 결과 50건 상한으로 300ms 표시 기준(REQ-EXR-005)을 만족한다.
     */
    private fun observeSearchInput() {
        _uiState
            .map { SearchInput(it.query, it.bodyPartFilters, it.equipmentFilters) }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            .onEach { runSearch(it) }
            .launchIn(viewModelScope)
    }

    private fun loadInitialSections() {
        viewModelScope.launch {
            val recent = repository.findRecentlyUsed()
            _uiState.update { it.copy(recent = recent, isLoading = false) }
        }
        repository.observeBodyParts()
            .onEach { parts -> _uiState.update { it.copy(availableBodyParts = parts) } }
            .launchIn(viewModelScope)
        repository.observeFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favorites = favorites) } }
            .launchIn(viewModelScope)
    }

    private suspend fun runSearch(input: SearchInput) {
        _uiState.update { it.copy(isSearching = true) }
        val results = searchExercises(
            ExerciseFilter(
                query = input.query,
                bodyPartCodes = input.bodyParts,
                equipments = input.equipments,
            ),
        )
        _uiState.update { it.copy(results = results, isSearching = false, isLoading = false) }
    }

    fun onAction(action: ExercisePickerAction) {
        when (action) {
            is ExercisePickerAction.OnQueryChange ->
                _uiState.update { it.copy(query = action.query) }

            is ExercisePickerAction.OnBodyPartToggle ->
                _uiState.update { it.copy(bodyPartFilters = it.bodyPartFilters.toggle(action.code)) }

            is ExercisePickerAction.OnEquipmentToggle ->
                _uiState.update {
                    it.copy(equipmentFilters = it.equipmentFilters.toggle(action.equipment))
                }

            is ExercisePickerAction.OnSelectionToggle ->
                _uiState.update { it.copy(selectedIds = it.selectedIds.toggle(action.exerciseId)) }

            is ExercisePickerAction.OnFavoriteToggle -> viewModelScope.launch {
                repository.toggleFavorite(action.exerciseId)
                // 즐겨찾기 표시가 결과 목록에도 반영되도록 재조회한다
                with(_uiState.value) {
                    runSearch(SearchInput(query, bodyPartFilters, equipmentFilters))
                }
            }

            is ExercisePickerAction.OnPreviewOpen -> openPreview(action.exerciseId)

            // 네비게이션은 Route 계층에서 가로챈다. ViewModel 은 상태만 정리한다.
            is ExercisePickerAction.OnOpenDetail ->
                _uiState.update { it.copy(preview = null) }

            ExercisePickerAction.OnPreviewClose ->
                _uiState.update { it.copy(preview = null) }

            ExercisePickerAction.OnConfirmSelection -> viewModelScope.launch {
                _events.send(
                    ExercisePickerEvent.SelectionConfirmed(_uiState.value.selectedIds.toList()),
                )
            }

            ExercisePickerAction.OnClearFilters ->
                _uiState.update {
                    it.copy(query = "", bodyPartFilters = emptySet(), equipmentFilters = emptySet())
                }
        }
    }

    /**
     * 미리보기 시트를 연다. (FN-EXR-018)
     *
     * 전체 화면으로 전환하면 선택 상태와 스크롤 위치가 끊기므로 시트로 처리한다.
     */
    private fun openPreview(exerciseId: String) {
        viewModelScope.launch {
            val exercise = repository.findById(exerciseId) ?: return@launch
            val video = repository.findDefaultVideo(exerciseId)
            _uiState.update {
                it.copy(
                    preview = ExercisePreview(
                        exercise = exercise,
                        videoUrl = video?.url,
                        videoId = video?.videoId,
                        videoStartSec = video?.startSec,
                        ptNoteCount = repository.countNotes(exerciseId),
                    ),
                )
            }
        }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> =
        if (value in this) this - value else this + value

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
