package com.sevenbits.myfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 첫 화면 결정. 온보딩을 마쳤는지 DB 를 읽어야 알 수 있으므로 잠시 [Loading] 이다.
 */
sealed interface StartState {
    data object Loading : StartState
    data object Onboarding : StartState
    data object Main : StartState
}

/**
 * 앱 진입 시점의 전역 상태.
 *
 * 온보딩 완료 여부와 테마를 결정한다. NavHost 의 startDestination 은 한 번 정해지면
 * 바꿀 수 없으므로, 판정이 끝나기 전에는 화면을 그리지 않는다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    private val _startState = MutableStateFlow<StartState>(StartState.Loading)
    val startState = _startState.asStateFlow()

    /** 온보딩에서 테마를 고르면 즉시 반영된다 */
    val themeMode = repository.observeSetting()
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.DARK,
        )

    init {
        viewModelScope.launch {
            // findSetting() 은 레코드가 없으면 만들어 준다 — 최초 실행도 여기서 처리된다
            val setting = repository.findSetting()
            _startState.value =
                if (setting.onboardingCompleted) StartState.Main else StartState.Onboarding
        }
    }

    /** 온보딩이 끝났을 때 호출. 이후 재진입에서는 다시 묻지 않는다. */
    fun onOnboardingFinished() {
        _startState.value = StartState.Main
    }
}
