package com.sevenbits.myfit.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sevenbits.myfit.core.common.NavResult
import com.sevenbits.myfit.feature.workout.log.WorkoutLogEvent
import com.sevenbits.myfit.feature.workout.log.WorkoutLogScreen
import com.sevenbits.myfit.feature.workout.log.WorkoutLogViewModel
import com.sevenbits.myfit.feature.workout.setinput.SetInputEvent
import com.sevenbits.myfit.feature.workout.setinput.SetInputScreen
import com.sevenbits.myfit.feature.workout.setinput.SetInputViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * 운동기록 네비게이션 그래프.
 *
 * feature 간 직접 참조 없이(R-3) :app 의 NavHost 가 조립한다.
 * 종목 선택 화면으로의 이동은 콜백으로 위임한다.
 */
@Serializable
data class WorkoutRoute(val date: String = "")

/** SCR-WRK-002 세트 입력 */
@Serializable
data class SetInputRoute(val logExerciseId: String)

fun NavGraphBuilder.workoutScreen(
    navController: NavController,
    onAddExercise: (logId: String) -> Unit = {},
) {
    composable<WorkoutRoute> { entry ->
        WorkoutLogRouteContent(
            savedStateHandle = entry.savedStateHandle,
            onOpenSetInput = { navController.navigate(SetInputRoute(it)) },
            onAddExercise = onAddExercise,
            onBack = { navController.popBackStack() },
        )
    }
    composable<SetInputRoute> {
        SetInputRouteContent(onBack = { navController.popBackStack() })
    }
}

@Composable
private fun WorkoutLogRouteContent(
    savedStateHandle: SavedStateHandle,
    onOpenSetInput: (String) -> Unit,
    onAddExercise: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 종목 선택 화면이 남긴 결과를 수신한다.
    // 소비 후 즉시 비워야 화면 복귀 때마다 같은 종목이 다시 추가되지 않는다.
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
            is WorkoutLogEvent.NavigateToSetInput -> onOpenSetInput(event.logExerciseId)
            is WorkoutLogEvent.NavigateToExercisePicker -> onAddExercise(event.logId)
            // 운동 수행 화면(SCR-WRK-003)은 후속 구현
            is WorkoutLogEvent.NavigateToSession -> Unit
            WorkoutLogEvent.NavigateBack -> onBack()
        }
    }

    WorkoutLogScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun SetInputRouteContent(
    onBack: () -> Unit,
    viewModel: SetInputViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SetInputEvent.NavigateBack -> onBack()
            // 휴식 타이머 서비스는 후속 구현 (FN-TOL-001)
            is SetInputEvent.StartRestTimer -> Unit
        }
    }

    SetInputScreen(uiState = uiState, onAction = viewModel::onAction)
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
