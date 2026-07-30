package com.sevenbits.myfit.feature.workout.setinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.common.UnitConverter
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.SetType
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** SCR-WRK-002 세트 입력 상태. (06_화면설계서 §3.12) */
data class SetInputUiState(
    val isLoading: Boolean = true,
    val exerciseName: String = "",
    val recordType: RecordType = RecordType.WEIGHT_REPS,
    val sets: List<WorkoutSet> = emptyList(),
    val selectedSetId: String? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val weightStep: Double = DEFAULT_WEIGHT_STEP,
    /** 프리필 근거 표시 — 값의 출처를 사용자가 알 수 있게 한다 (FN-WRK-014) */
    val prefillHint: String? = null,
) {
    val selectedSet: WorkoutSet? get() = sets.firstOrNull { it.id == selectedSetId }

    companion object {
        const val DEFAULT_WEIGHT_STEP = 2.5
    }
}

sealed interface SetInputAction {
    data class OnSetSelect(val setId: String) : SetInputAction
    data class OnWeightChange(val value: Double?) : SetInputAction
    data class OnRepsChange(val value: Int?) : SetInputAction
    data object OnWeightIncrease : SetInputAction
    data object OnWeightDecrease : SetInputAction
    data object OnRepsIncrease : SetInputAction
    data object OnRepsDecrease : SetInputAction
    data class OnSetTypeCycle(val setId: String) : SetInputAction
    data class OnToggleComplete(val setId: String) : SetInputAction
    data object OnAddSet : SetInputAction
    data object OnCopyLastSet : SetInputAction
    data class OnDeleteSet(val setId: String) : SetInputAction
    data object OnBack : SetInputAction
}

sealed interface SetInputEvent {
    data object NavigateBack : SetInputEvent
    /** 세트 완료 시 휴식 타이머 시작 (FN-WRK-020 → FN-TOL-001) */
    data class StartRestTimer(val seconds: Int) : SetInputEvent
}

/**
 * 세트 입력.
 *
 * **입력 즉시 영속화**한다. 명시적 저장 버튼이 없다. (P5 / REQ-WRK-011)
 * UI 는 낙관적으로 즉시 갱신하고(100ms 기준, REQ-NFR-001), DB 쓰기는 300ms 디바운스로
 * 뒤따른다 — 연타 입력 시 쓰기 폭주를 막는다.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SetInputViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val logExerciseId: String = checkNotNull(savedStateHandle[ARG_LOG_EXERCISE_ID]) {
        "logExerciseId 인자가 없습니다"
    }

    private val _uiState = MutableStateFlow(SetInputUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SetInputEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 디바운스 대상 — 마지막 편집 세트 */
    private val pendingWrite = MutableStateFlow<WorkoutSet?>(null)

    private var restSeconds: Int = DEFAULT_REST_SEC

    init {
        repository.observeSets(logExerciseId)
            .onEach { sets ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        sets = sets,
                        // 선택된 세트가 없으면 첫 미완료 세트를 자동 선택한다
                        selectedSetId = state.selectedSetId
                            ?: sets.firstOrNull { !it.isCompleted }?.id
                            ?: sets.firstOrNull()?.id,
                    )
                }
            }
            .launchIn(viewModelScope)

        pendingWrite
            .filterNotNull()
            .debounce(WRITE_DEBOUNCE_MS)
            .onEach { repository.upsertSet(it) }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SetInputAction) {
        when (action) {
            is SetInputAction.OnSetSelect ->
                _uiState.update { it.copy(selectedSetId = action.setId) }

            is SetInputAction.OnWeightChange -> editSelected { it.copy(weightKg = action.value) }

            is SetInputAction.OnRepsChange -> editSelected { it.copy(reps = action.value) }

            SetInputAction.OnWeightIncrease -> editSelected {
                it.copy(weightKg = ((it.weightKg ?: 0.0) + _uiState.value.weightStep).coerceIn(0.0, MAX_WEIGHT))
            }

            SetInputAction.OnWeightDecrease -> editSelected {
                it.copy(weightKg = ((it.weightKg ?: 0.0) - _uiState.value.weightStep).coerceAtLeast(0.0))
            }

            SetInputAction.OnRepsIncrease -> editSelected {
                it.copy(reps = ((it.reps ?: 0) + 1).coerceAtMost(MAX_REPS))
            }

            SetInputAction.OnRepsDecrease -> editSelected {
                it.copy(reps = ((it.reps ?: 0) - 1).coerceAtLeast(0))
            }

            is SetInputAction.OnSetTypeCycle -> {
                val set = _uiState.value.sets.firstOrNull { it.id == action.setId } ?: return
                val next = set.setType.next()
                applyLocal(set.copy(setType = next))
                viewModelScope.launch { repository.upsertSet(set.copy(setType = next)) }
            }

            is SetInputAction.OnToggleComplete -> {
                val set = _uiState.value.sets.firstOrNull { it.id == action.setId } ?: return
                val nowCompleted = !set.isCompleted
                viewModelScope.launch {
                    repository.toggleSetComplete(
                        set.id,
                        nowCompleted,
                        System.currentTimeMillis(),
                    )
                    // 세트를 완료하면 휴식 타이머가 자동 시작된다 (FN-WRK-020)
                    if (nowCompleted) _events.send(SetInputEvent.StartRestTimer(restSeconds))
                }
            }

            SetInputAction.OnAddSet -> viewModelScope.launch {
                val newId = repository.addSet(logExerciseId)
                _uiState.update { it.copy(selectedSetId = newId) }
            }

            SetInputAction.OnCopyLastSet -> viewModelScope.launch {
                val newId = repository.addSet(logExerciseId)
                _uiState.update { it.copy(selectedSetId = newId) }
            }

            is SetInputAction.OnDeleteSet -> viewModelScope.launch {
                repository.deleteSet(action.setId)
                _uiState.update {
                    it.copy(selectedSetId = it.selectedSetId.takeIf { id -> id != action.setId })
                }
            }

            SetInputAction.OnBack -> viewModelScope.launch {
                flushPendingWrite()
                _events.send(SetInputEvent.NavigateBack)
            }
        }
    }

    /**
     * 낙관적 갱신 + 지연 쓰기.
     *
     * UI 는 즉시 반영하고 DB 쓰기는 디바운스한다. 앱이 강제 종료되어도
     * 마지막 디바운스 확정분까지는 보존된다.
     */
    private fun editSelected(transform: (WorkoutSet) -> WorkoutSet) {
        val current = _uiState.value.selectedSet ?: return
        val updated = transform(current)
        applyLocal(updated)
        pendingWrite.value = updated
    }

    private fun applyLocal(updated: WorkoutSet) {
        _uiState.update { state ->
            state.copy(sets = state.sets.map { if (it.id == updated.id) updated else it })
        }
    }

    /** 화면을 벗어나기 전에 대기 중인 쓰기를 확정한다 — 디바운스 유실 방지 */
    private suspend fun flushPendingWrite() {
        pendingWrite.value?.let { repository.upsertSet(it) }
        pendingWrite.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 소멸 시점에도 마지막 입력이 남아 있으면 저장한다
        pendingWrite.value?.let { pending ->
            kotlinx.coroutines.runBlocking { repository.upsertSet(pending) }
        }
    }

    /** 정상 → 웜업 → 드랍 → 실패 → 정상 순환 (FN-WRK-011) */
    private fun SetType.next(): SetType = when (this) {
        SetType.NORMAL -> SetType.WARMUP
        SetType.WARMUP -> SetType.DROP
        SetType.DROP -> SetType.FAILURE
        SetType.FAILURE -> SetType.NORMAL
    }

    companion object {
        const val ARG_LOG_EXERCISE_ID = "logExerciseId"
        private const val WRITE_DEBOUNCE_MS = 300L
        private const val DEFAULT_REST_SEC = 90
        private const val MAX_WEIGHT = 500.0
        private const val MAX_REPS = 200
    }
}

/** 표시용 문자열 변환 — 단위 변환은 이 경계에서만 일어난다 (P7) */
fun WorkoutSet.weightDisplay(unit: WeightUnit): String {
    val kg = weightKg ?: return "-"
    val value = UnitConverter.kgToDisplay(kg, unit)
    return "$value ${if (unit == WeightUnit.KG) "kg" else "lb"}"
}

fun WorkoutSet.repsDisplay(): String = reps?.let { "${it}회" } ?: "-"
