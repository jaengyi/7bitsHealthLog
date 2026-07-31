package com.sevenbits.myfit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import com.sevenbits.myfit.core.domain.repository.UserRepository
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

/** SCR-CMN-005 환경설정 상태. (06_화면설계서 §3.5) */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val setting: UserSetting = UserSetting(),
    val profile: UserProfile? = null,
)

sealed interface SettingsAction {
    data class OnThemeChange(val value: ThemeMode) : SettingsAction
    data class OnWeightUnitChange(val value: WeightUnit) : SettingsAction
    data class OnWeightStepChange(val value: Double) : SettingsAction
    data class OnDefaultRestChange(val value: Int) : SettingsAction
    data class OnOneRmFormulaChange(val value: OneRmFormula) : SettingsAction
    data class OnWeeklyGoalChange(val value: Int) : SettingsAction
    data class OnTimerSoundToggle(val value: Boolean) : SettingsAction
    data class OnTimerVibrateToggle(val value: Boolean) : SettingsAction
    data class OnBarWeightChange(val value: Double) : SettingsAction
    data object OnOpenProfile : SettingsAction
    data object OnBack : SettingsAction
}

sealed interface SettingsEvent {
    data object OpenProfile : SettingsEvent
    data object NavigateBack : SettingsEvent
}

/**
 * 환경설정.
 *
 * 변경은 **즉시 저장**한다. 저장 버튼이 없는 것이 앱 전체 방침이다. (P5)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        repository.observeSetting()
            .onEach { setting -> _uiState.update { it.copy(isLoading = false, setting = setting) } }
            .launchIn(viewModelScope)
        repository.observeProfile()
            .onEach { profile -> _uiState.update { it.copy(profile = profile) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnThemeChange -> save { it.copy(themeMode = action.value) }
            is SettingsAction.OnWeightUnitChange -> save { it.copy(weightUnit = action.value) }
            is SettingsAction.OnWeightStepChange -> save { it.copy(weightStepKg = action.value) }
            is SettingsAction.OnDefaultRestChange -> save { it.copy(defaultRestSec = action.value) }
            is SettingsAction.OnOneRmFormulaChange -> save { it.copy(oneRmFormula = action.value) }
            is SettingsAction.OnWeeklyGoalChange -> save { it.copy(weeklyGoalCount = action.value) }
            is SettingsAction.OnTimerSoundToggle -> save { it.copy(timerSoundEnabled = action.value) }
            is SettingsAction.OnTimerVibrateToggle ->
                save { it.copy(timerVibrateEnabled = action.value) }
            is SettingsAction.OnBarWeightChange -> save { it.copy(defaultBarWeightKg = action.value) }

            SettingsAction.OnOpenProfile -> emit(SettingsEvent.OpenProfile)
            SettingsAction.OnBack -> emit(SettingsEvent.NavigateBack)
        }
    }

    private fun save(transform: (UserSetting) -> UserSetting) {
        val updated = transform(_uiState.value.setting)
        // 낙관적 갱신 — Flow 가 돌아오기 전에 화면이 먼저 반응한다
        _uiState.update { it.copy(setting = updated) }
        viewModelScope.launch { repository.upsertSetting(updated) }
    }

    private fun emit(event: SettingsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
