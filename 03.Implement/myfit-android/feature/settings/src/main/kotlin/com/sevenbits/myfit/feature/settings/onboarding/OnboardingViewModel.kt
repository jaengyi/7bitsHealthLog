package com.sevenbits.myfit.feature.settings.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import com.sevenbits.myfit.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 위저드 총 단계 수. (06_화면설계서 §3.6) */
const val ONBOARDING_STEP_COUNT = 4

/** SCR-CMN-006 온보딩 상태. */
data class OnboardingUiState(
    /** 0-based */
    val step: Int = 0,
    val profile: UserProfile = UserProfile(),
    val setting: UserSetting = UserSetting(),
    val isFinishing: Boolean = false,
) {
    val isLastStep: Boolean get() = step == ONBOARDING_STEP_COUNT - 1
}

sealed interface OnboardingAction {
    data class OnGenderChange(val value: Gender) : OnboardingAction
    data class OnHeightChange(val value: Double) : OnboardingAction
    data class OnGoalChange(val value: GoalType) : OnboardingAction
    data class OnCareerChange(val value: CareerLevel) : OnboardingAction
    data class OnWeightUnitChange(val value: WeightUnit) : OnboardingAction
    data class OnThemeChange(val value: ThemeMode) : OnboardingAction
    data class OnWeeklyGoalChange(val value: Int) : OnboardingAction
    data class OnRestChange(val value: Int) : OnboardingAction
    data object OnNext : OnboardingAction
    data object OnPrevious : OnboardingAction
    data object OnSkip : OnboardingAction
}

sealed interface OnboardingEvent {
    data object Finished : OnboardingEvent
}

/**
 * 온보딩 4단계 위저드.
 *
 * **모든 단계를 건너뛸 수 있다**(REQ-CMN-004). 건너뛰어도 기본값이 이미 [UserSetting] 에
 * 들어 있으므로 앱은 정상 동작한다 — 입력을 강제하지 않는 것이 요구사항이다.
 *
 * 저장은 중간 단계마다 하지 않고 **완료·건너뛰기 시점에 한 번** 한다. 온보딩 도중
 * 앱을 죽이면 아무것도 남지 않아야 다음 실행에서 처음부터 다시 물을 수 있다.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.OnGenderChange ->
                _uiState.update { it.copy(profile = it.profile.copy(gender = action.value)) }
            is OnboardingAction.OnHeightChange ->
                _uiState.update { it.copy(profile = it.profile.copy(heightCm = action.value)) }
            is OnboardingAction.OnGoalChange ->
                _uiState.update { it.copy(profile = it.profile.copy(goalType = action.value)) }
            is OnboardingAction.OnCareerChange ->
                _uiState.update { it.copy(profile = it.profile.copy(careerLevel = action.value)) }

            is OnboardingAction.OnWeightUnitChange ->
                _uiState.update { it.copy(setting = it.setting.copy(weightUnit = action.value)) }
            is OnboardingAction.OnThemeChange ->
                _uiState.update { it.copy(setting = it.setting.copy(themeMode = action.value)) }
            is OnboardingAction.OnWeeklyGoalChange ->
                _uiState.update { it.copy(setting = it.setting.copy(weeklyGoalCount = action.value)) }
            is OnboardingAction.OnRestChange ->
                _uiState.update { it.copy(setting = it.setting.copy(defaultRestSec = action.value)) }

            OnboardingAction.OnNext ->
                if (_uiState.value.isLastStep) finish(saveInput = true) else move(+1)
            OnboardingAction.OnPrevious -> move(-1)
            // 건너뛰기는 지금까지 입력한 값도 버린다. 부분 입력이 남아 있으면
            // 사용자가 "건너뛰었는데 왜 값이 있지" 하고 혼란스러워한다.
            OnboardingAction.OnSkip -> finish(saveInput = false)
        }
    }

    private fun move(delta: Int) {
        _uiState.update { it.copy(step = (it.step + delta).coerceIn(0, ONBOARDING_STEP_COUNT - 1)) }
    }

    private fun finish(saveInput: Boolean) {
        if (_uiState.value.isFinishing) return
        _uiState.update { it.copy(isFinishing = true) }

        viewModelScope.launch {
            if (saveInput) {
                val state = _uiState.value
                // 기존 레코드의 id·loginType 을 살려 둔다. 새 UserProfile 로 덮으면
                // 게스트 사용자 식별자가 날아간다.
                val base = repository.findProfile() ?: UserProfile()
                repository.upsertProfile(
                    base.copy(
                        gender = state.profile.gender ?: base.gender,
                        heightCm = state.profile.heightCm ?: base.heightCm,
                        goalType = state.profile.goalType ?: base.goalType,
                        careerLevel = state.profile.careerLevel ?: base.careerLevel,
                    ),
                )
                val current = repository.findSetting()
                repository.upsertSetting(
                    current.copy(
                        weightUnit = state.setting.weightUnit,
                        themeMode = state.setting.themeMode,
                        weeklyGoalCount = state.setting.weeklyGoalCount,
                        defaultRestSec = state.setting.defaultRestSec,
                    ),
                )
            }
            // 저장 여부와 무관하게 완료로 표시한다 — 다시 묻지 않는다
            repository.markOnboardingCompleted()
            _events.send(OnboardingEvent.Finished)
        }
    }
}
