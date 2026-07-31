package com.sevenbits.myfit.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sevenbits.myfit.feature.settings.more.MoreScreen
import com.sevenbits.myfit.feature.settings.more.MoreViewModel
import com.sevenbits.myfit.feature.settings.onboarding.OnboardingEvent
import com.sevenbits.myfit.feature.settings.onboarding.OnboardingScreen
import com.sevenbits.myfit.feature.settings.onboarding.OnboardingViewModel
import com.sevenbits.myfit.feature.settings.profile.ProfileEditAction
import com.sevenbits.myfit.feature.settings.profile.ProfileEditScreen
import com.sevenbits.myfit.feature.settings.profile.ProfileEditViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** SCR-CMN-003 마이페이지 (더보기 탭 진입점) */
@Serializable
data object MoreRoute

/** SCR-CMN-005 환경설정 */
@Serializable
data object SettingsRoute

/** SCR-CMN-004 프로필 편집 */
@Serializable
data object ProfileEditRoute

/**
 * 설정 계열 네비게이션 그래프.
 *
 * @param appVersion 앱 정보 표시용. feature 모듈은 :app 의 BuildConfig 를 볼 수 없으므로
 *   호출부가 넘긴다.
 */
fun NavGraphBuilder.settingsScreen(
    navController: NavController,
    appVersion: String = "",
) {
    composable<MoreRoute> {
        val viewModel: MoreViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        MoreScreen(
            uiState = uiState,
            onOpenProfile = { navController.navigate(ProfileEditRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            appVersion = appVersion,
        )
    }

    composable<SettingsRoute> {
        SettingsRouteContent(
            onBack = { navController.popBackStack() },
            onOpenProfile = { navController.navigate(ProfileEditRoute) },
        )
    }

    composable<ProfileEditRoute> {
        val viewModel: ProfileEditViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProfileEditScreen(
            uiState = uiState,
            onAction = { action ->
                if (action is ProfileEditAction.OnBack) {
                    navController.popBackStack()
                } else {
                    viewModel.onAction(action)
                }
            },
        )
    }

}

/**
 * SCR-CMN-006 온보딩. NavHost 밖에서 단독으로 띄운다.
 *
 * NavHost 의 startDestination 은 한 번 정해지면 바꿀 수 없어, 온보딩을 그래프 안에 두면
 * 완료 후 홈으로 넘어갈 때 그래프를 통째로 재생성해야 한다. 온보딩은 앱 생애 한 번뿐이므로
 * 아예 그래프 밖에 두는 편이 단순하다.
 */
@Composable
fun OnboardingHost(onFinished: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.Finished -> onFinished()
        }
    }

    OnboardingScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun SettingsRouteContent(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onBack()
            SettingsEvent.OpenProfile -> onOpenProfile()
        }
    }

    SettingsScreen(uiState = uiState, onAction = viewModel::onAction)
}

/** 1회성 이벤트 수집 — 화면이 STARTED 일 때만 처리한다 */
@Composable
private fun <T> ObserveEvents(events: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect(onEvent)
        }
    }
}
