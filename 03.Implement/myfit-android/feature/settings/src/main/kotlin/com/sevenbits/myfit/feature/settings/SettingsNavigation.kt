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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * 설정 네비게이션 그래프. (SCR-CMN-005)
 */
@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<SettingsRoute> {
        SettingsRouteContent(onBack = { navController.popBackStack() })
    }
}

@Composable
private fun SettingsRouteContent(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onBack()
            // 프로필 편집 화면은 후속 구현
            SettingsEvent.OpenProfile -> Unit
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
