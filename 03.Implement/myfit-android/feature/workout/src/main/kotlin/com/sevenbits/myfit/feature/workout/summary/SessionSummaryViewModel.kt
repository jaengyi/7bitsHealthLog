package com.sevenbits.myfit.feature.workout.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.common.UnitConverter
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.SessionSummary
import com.sevenbits.myfit.core.domain.model.WorkoutLog
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import com.sevenbits.myfit.core.domain.repository.RoutineRepository
import com.sevenbits.myfit.core.domain.repository.UserRepository
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** 부위별 세트 비중 한 줄. (FN-WRK-028) */
data class BodyPartShare(
    val name: String,
    val ratio: Double,
    val setCount: Int,
)

/** SCR-WRK-004 세션 요약 상태. (06_화면설계서 §3.14) */
data class SessionSummaryUiState(
    val isLoading: Boolean = true,
    val log: WorkoutLog? = null,
    val summary: SessionSummary = SessionSummary(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val bodyPartShares: List<BodyPartShare> = emptyList(),
    val showSaveRoutineDialog: Boolean = false,
    val routineNameInput: String = "",
    val savedRoutineName: String? = null,
) {
    val durationText: String?
        get() {
            val started = log?.startedAtEpochMillis ?: return null
            val ended = log.endedAtEpochMillis ?: return null
            val minutes = ((ended - started) / 60_000L).toInt().coerceAtLeast(0)
            return when {
                minutes >= 60 -> "${minutes / 60}시간 ${minutes % 60}분"
                else -> "${minutes}분"
            }
        }

    val volumeDisplay: String
        get() = "${UnitConverter.kgToDisplay(summary.totalVolumeKg, weightUnit)} " +
            if (weightUnit == WeightUnit.KG) "kg" else "lb"
}

sealed interface SessionSummaryAction {
    data object OnSaveRoutineRequest : SessionSummaryAction
    data class OnRoutineNameChange(val value: String) : SessionSummaryAction
    data object OnSaveRoutineConfirm : SessionSummaryAction
    data object OnSaveRoutineCancel : SessionSummaryAction
    data object OnConfirm : SessionSummaryAction
}

sealed interface SessionSummaryEvent {
    data object NavigateHome : SessionSummaryEvent
    data class ShowMessage(val message: String) : SessionSummaryEvent
}

/**
 * 세션 요약.
 *
 * 집계는 Domain 이 이미 산출한 [SessionSummary] 를 쓴다. 화면에서 다시 계산하지 않는다. (P6)
 */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val logId: String = checkNotNull(savedStateHandle[ARG_LOG_ID]) {
        "logId 인자가 없습니다"
    }

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SessionSummaryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val log = repository.observeLog(logId).first()
            val summary = repository.calcSummary(logId)
            val unit = userRepository.findSetting().weightUnit
            // 부위 코드만으로는 읽을 수 없다. 이름을 붙여 준다.
            val partNames = exerciseRepository.observeBodyParts().first()
                .associate { it.code to it.name }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    log = log,
                    summary = summary,
                    weightUnit = unit,
                    bodyPartShares = summary.toShares(partNames),
                    routineNameInput = log?.routineName ?: defaultRoutineName(log),
                )
            }
        }
    }

    fun onAction(action: SessionSummaryAction) {
        when (action) {
            SessionSummaryAction.OnSaveRoutineRequest ->
                _uiState.update { it.copy(showSaveRoutineDialog = true) }

            is SessionSummaryAction.OnRoutineNameChange ->
                _uiState.update { it.copy(routineNameInput = action.value) }

            SessionSummaryAction.OnSaveRoutineCancel ->
                _uiState.update { it.copy(showSaveRoutineDialog = false) }

            SessionSummaryAction.OnSaveRoutineConfirm -> {
                val name = _uiState.value.routineNameInput.trim()
                if (name.isEmpty()) return
                viewModelScope.launch {
                    routineRepository.saveLogAsRoutine(logId, name)
                    _uiState.update {
                        it.copy(showSaveRoutineDialog = false, savedRoutineName = name)
                    }
                    _events.send(SessionSummaryEvent.ShowMessage("루틴 '$name' 으로 저장했습니다"))
                }
            }

            SessionSummaryAction.OnConfirm ->
                viewModelScope.launch { _events.send(SessionSummaryEvent.NavigateHome) }
        }
    }

    /** 비중이 큰 부위부터 보여 준다 */
    private fun SessionSummary.toShares(names: Map<String, String>): List<BodyPartShare> =
        bodyPartRatio.entries
            .sortedByDescending { it.value }
            .map { (code, ratio) ->
                BodyPartShare(
                    name = names[code] ?: code,
                    ratio = ratio,
                    setCount = (totalSetCount * ratio / 100.0).roundToInt(),
                )
            }

    /** 루틴 이름 초안 — 첫 종목 이름을 딴다. 빈 칸에서 시작하는 것보다 낫다. */
    private fun defaultRoutineName(log: WorkoutLog?): String {
        val first = log?.exercises?.firstOrNull()?.exerciseName ?: return "새 루틴"
        val count = log.exercises.size
        return if (count > 1) "$first 외 ${count - 1}종목" else first
    }

    companion object {
        const val ARG_LOG_ID = "logId"
    }
}
