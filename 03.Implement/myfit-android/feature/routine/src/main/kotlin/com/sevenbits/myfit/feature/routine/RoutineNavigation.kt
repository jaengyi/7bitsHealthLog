package com.sevenbits.myfit.feature.routine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sevenbits.myfit.core.common.NavResult
import com.sevenbits.myfit.feature.routine.editor.RoutineEditorEvent
import com.sevenbits.myfit.feature.routine.editor.RoutineEditorScreen
import com.sevenbits.myfit.feature.routine.editor.RoutineEditorViewModel
import com.sevenbits.myfit.feature.routine.list.RoutineListEvent
import com.sevenbits.myfit.feature.routine.list.RoutineListScreen
import com.sevenbits.myfit.feature.routine.list.RoutineListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * 루틴 네비게이션 그래프.
 *
 * 종목 선택·일지 이동은 :app 이 콜백으로 위임받는다. feature 간 참조는 없다. (R-3)
 */
@Serializable
data object RoutineRoute

/** SCR-RTN-002 루틴 편집. routineId 가 비면 신규 생성 */
@Serializable
data class RoutineEditorRoute(val routineId: String = "")

fun NavGraphBuilder.routineScreen(
    navController: NavController,
    onAddExercise: () -> Unit = {},
    onOpenLog: (date: String) -> Unit = {},
) {
    composable<RoutineRoute> {
        RoutineListRouteContent(
            onOpenEditor = { navController.navigate(RoutineEditorRoute(it)) },
            onOpenLog = onOpenLog,
        )
    }
    composable<RoutineEditorRoute> { entry ->
        RoutineEditorRouteContent(
            savedStateHandle = entry.savedStateHandle,
            onBack = { navController.popBackStack() },
            onAddExercise = onAddExercise,
        )
    }
}

@Composable
private fun RoutineListRouteContent(
    onOpenEditor: (String) -> Unit,
    onOpenLog: (String) -> Unit,
    viewModel: RoutineListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is RoutineListEvent.OpenEditor -> onOpenEditor(event.routineId)
            is RoutineListEvent.OpenLog -> onOpenLog(event.date)
        }
    }

    RoutineListScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun RoutineEditorRouteContent(
    savedStateHandle: SavedStateHandle,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 종목 선택 결과 수신 — 소비 후 즉시 비운다
    val selectedIds by savedStateHandle
        .getStateFlow(NavResult.SELECTED_EXERCISE_IDS, "")
        .collectAsStateWithLifecycle()

    LaunchedEffect(selectedIds) {
        if (selectedIds.isNotBlank()) {
            viewModel.onExercisesSelected(NavResult.decodeIds(selectedIds))
            savedStateHandle[NavResult.SELECTED_EXERCISE_IDS] = ""
        }
    }

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            RoutineEditorEvent.Saved, RoutineEditorEvent.NavigateBack -> onBack()
            RoutineEditorEvent.RequestExercisePicker -> onAddExercise()
        }
    }

    RoutineEditorScreen(uiState = uiState, onAction = viewModel::onAction)
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
