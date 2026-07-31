package com.sevenbits.myfit.feature.workout.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.common.UnitConverter
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.RestTimerState
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.model.WorkoutLogExercise
import com.sevenbits.myfit.core.domain.model.WorkoutSet
import com.sevenbits.myfit.core.domain.model.WorkoutStatus
import com.sevenbits.myfit.core.domain.repository.RestTimerController
import com.sevenbits.myfit.core.domain.repository.UserRepository
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
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

/** SCR-WRK-003 운동 수행 상태. (06_화면설계서 §3.13) */
data class SessionUiState(
    val isLoading: Boolean = true,
    val log: WorkoutLog? = null,
    /** 현재 종목 위치 (0-based) */
    val currentIndex: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restTimer: RestTimerState = RestTimerState(),
    val isFinishing: Boolean = false,
    val showFinishConfirm: Boolean = false,
) {
    val exercises: List<WorkoutLogExercise> get() = log?.exercises.orEmpty()
    val current: WorkoutLogExercise? get() = exercises.getOrNull(currentIndex)
    val next: WorkoutLogExercise? get() = exercises.getOrNull(currentIndex + 1)
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex < exercises.lastIndex

    /** 진행률은 세션 전체 세트 기준이다 (FN-WRK-021) */
    val totalSetCount: Int get() = exercises.sumOf { it.sets.size }
    val completedSetCount: Int get() = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val progress: Float
        get() = if (totalSetCount == 0) 0f else completedSetCount.toFloat() / totalSetCount

    /** 현재 종목에서 다음에 수행할 세트 — 강조 표시 대상 */
    val nextSetId: String? get() = current?.sets?.firstOrNull { !it.isCompleted }?.id
}

sealed interface SessionAction {
    data class OnToggleSet(val setId: String) : SessionAction
    data object OnPreviousExercise : SessionAction
    data object OnNextExercise : SessionAction
    data class OnExerciseSelect(val index: Int) : SessionAction
    /** 값 수정은 이 화면에서 하지 않는다 — 세트 입력 화면으로 보낸다 */
    data object OnEditCurrentExercise : SessionAction
    data object OnFinishRequest : SessionAction
    data object OnFinishConfirm : SessionAction
    data object OnFinishCancel : SessionAction
    data object OnClose : SessionAction

    data object OnTimerTogglePause : SessionAction
    data object OnTimerSkip : SessionAction
    data class OnTimerAdjust(val deltaSec: Int) : SessionAction
}

sealed interface SessionEvent {
    data class NavigateToSummary(val logId: String) : SessionEvent
    data class NavigateToSetInput(val logExerciseId: String) : SessionEvent
    /** 백그라운드 유지를 위한 Foreground Service 기동 (FN-TOL-004) */
    data class StartRestTimer(val seconds: Int) : SessionEvent
    data object NavigateBack : SessionEvent
}

/**
 * 운동 수행.
 *
 * 세트 체크와 휴식 타이머만 다룬다. **값 편집은 제공하지 않는다** — 운동 중에는
 * 화면을 제대로 보지 않고 누르는 일이 잦아 오조작 위험이 크다. 수정이 필요하면
 * 세트 입력 화면으로 이동한다. (06_화면설계서 §3.13)
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val restTimer: RestTimerController,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val logId: String = checkNotNull(savedStateHandle[ARG_LOG_ID]) {
        "logId 인자가 없습니다"
    }

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SessionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var defaultRestSec: Int = FALLBACK_REST_SEC

    /** 사용자가 종목을 직접 넘긴 뒤에는 자동 이동을 하지 않는다 */
    private var indexPinned = false

    init {
        repository.observeLog(logId)
            .onEach { log ->
                _uiState.update { state ->
                    val exercises = log?.exercises.orEmpty()
                    state.copy(
                        isLoading = false,
                        log = log,
                        currentIndex = if (indexPinned) {
                            state.currentIndex.coerceIn(0, maxOf(exercises.lastIndex, 0))
                        } else {
                            firstUnfinishedIndex(exercises)
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        restTimer.state
            .onEach { timer -> _uiState.update { it.copy(restTimer = timer) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val setting = userRepository.findSetting()
            defaultRestSec = setting.defaultRestSec
            _uiState.update { it.copy(weightUnit = setting.weightUnit) }
        }
    }

    fun onAction(action: SessionAction) {
        when (action) {
            is SessionAction.OnToggleSet -> toggleSet(action.setId)

            SessionAction.OnPreviousExercise -> moveExercise(-1)
            SessionAction.OnNextExercise -> moveExercise(+1)
            is SessionAction.OnExerciseSelect -> {
                indexPinned = true
                _uiState.update {
                    it.copy(currentIndex = action.index.coerceIn(0, maxOf(it.exercises.lastIndex, 0)))
                }
            }

            SessionAction.OnEditCurrentExercise -> {
                val id = _uiState.value.current?.id ?: return
                emit(SessionEvent.NavigateToSetInput(id))
            }

            SessionAction.OnFinishRequest -> _uiState.update { it.copy(showFinishConfirm = true) }
            SessionAction.OnFinishCancel -> _uiState.update { it.copy(showFinishConfirm = false) }
            SessionAction.OnFinishConfirm -> finish()

            SessionAction.OnClose -> emit(SessionEvent.NavigateBack)

            SessionAction.OnTimerTogglePause ->
                if (_uiState.value.restTimer.isPaused) restTimer.resume() else restTimer.pause()
            SessionAction.OnTimerSkip -> restTimer.skip()
            is SessionAction.OnTimerAdjust -> restTimer.adjust(action.deltaSec)
        }
    }

    private fun toggleSet(setId: String) {
        val exercise = _uiState.value.exercises.firstOrNull { ex ->
            ex.sets.any { it.id == setId }
        } ?: return
        val set = exercise.sets.first { it.id == setId }
        val nowCompleted = !set.isCompleted

        viewModelScope.launch {
            repository.toggleSetComplete(set.id, nowCompleted, System.currentTimeMillis())
            if (!nowCompleted) return@launch

            // 종목별 휴식시간이 있으면 그것을 쓴다. 없을 때만 전역 설정으로 내려간다.
            // (FN-TOL-002)
            val seconds = exercise.defaultRestSec ?: defaultRestSec
            restTimer.start(seconds, sourceSetId = set.id)
            _events.send(SessionEvent.StartRestTimer(seconds))
        }
    }

    private fun moveExercise(delta: Int) {
        indexPinned = true
        _uiState.update {
            it.copy(currentIndex = (it.currentIndex + delta).coerceIn(0, maxOf(it.exercises.lastIndex, 0)))
        }
    }

    private fun finish() {
        if (_uiState.value.isFinishing) return
        _uiState.update { it.copy(isFinishing = true, showFinishConfirm = false) }

        viewModelScope.launch {
            // 운동이 끝나면 타이머도 끝나야 한다. 남겨 두면 요약 화면에서 알림이 계속 뜬다.
            restTimer.skip()
            repository.updateStatus(
                logId,
                WorkoutStatus.COMPLETED.name,
                System.currentTimeMillis(),
            )
            repository.refreshSummaryCache(logId)
            _events.send(SessionEvent.NavigateToSummary(logId))
        }
    }

    /**
     * 미완료 세트가 남은 첫 종목. 앱을 껐다 켜도 하던 자리로 돌아온다. (FN-WRK-023)
     * 전부 완료했으면 마지막 종목을 보여 준다.
     */
    private fun firstUnfinishedIndex(exercises: List<WorkoutLogExercise>): Int {
        if (exercises.isEmpty()) return 0
        val index = exercises.indexOfFirst { ex -> ex.sets.any { !it.isCompleted } }
        return if (index >= 0) index else exercises.lastIndex
    }

    private fun emit(event: SessionEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        const val ARG_LOG_ID = "logId"
        private const val FALLBACK_REST_SEC = 90
    }
}

/** 세트 한 줄 요약 — "80kg × 8회" */
fun WorkoutSet.summaryDisplay(unit: WeightUnit): String {
    val weight = weightKg?.let {
        "${UnitConverter.kgToDisplay(it, unit)}${if (unit == WeightUnit.KG) "kg" else "lb"}"
    }
    val repsText = reps?.let { "${it}회" }
    return listOfNotNull(weight, repsText).joinToString(" × ").ifBlank { "-" }
}
