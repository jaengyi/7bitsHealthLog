package com.sevenbits.myfit.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * 캘린더 네비게이션 그래프.
 *
 * 일지 이동은 :app 이 콜백으로 위임받는다. feature 간 참조는 없다. (R-3)
 */
@Serializable
data object CalendarRoute

fun NavGraphBuilder.calendarScreen(onOpenLog: (date: String) -> Unit = {}) {
    composable<CalendarRoute> {
        CalendarRouteContent(onOpenLog = onOpenLog)
    }
}

@Composable
private fun CalendarRouteContent(
    onOpenLog: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is CalendarEvent.OpenLog -> onOpenLog(event.date)
        }
    }

    CalendarScreen(uiState = uiState, onAction = viewModel::onAction)
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
