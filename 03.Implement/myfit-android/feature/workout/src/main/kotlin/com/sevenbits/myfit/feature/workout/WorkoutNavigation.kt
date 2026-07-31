package com.sevenbits.myfit.feature.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sevenbits.myfit.feature.workout.session.SessionEvent
import com.sevenbits.myfit.feature.workout.session.SessionScreen
import com.sevenbits.myfit.feature.workout.session.SessionViewModel
import com.sevenbits.myfit.feature.workout.setinput.SetInputEvent
import com.sevenbits.myfit.feature.workout.setinput.SetInputScreen
import com.sevenbits.myfit.feature.workout.setinput.SetInputViewModel
import com.sevenbits.myfit.feature.workout.summary.SessionSummaryEvent
import com.sevenbits.myfit.feature.workout.summary.SessionSummaryScreen
import com.sevenbits.myfit.feature.workout.summary.SessionSummaryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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

/** SCR-WRK-003 운동 수행 */
@Serializable
data class SessionRoute(val logId: String)

/** SCR-WRK-004 세션 요약 */
@Serializable
data class SessionSummaryRoute(val logId: String)

fun NavGraphBuilder.workoutScreen(
    navController: NavController,
    onAddExercise: (logId: String) -> Unit = {},
    /**
     * 휴식 타이머 Foreground Service 기동을 :app 에 위임한다.
     * feature 모듈은 서비스 클래스를 알지 못한다. (R-3)
     */
    onStartRestTimerService: () -> Unit = {},
) {
    composable<WorkoutRoute> { entry ->
        WorkoutLogRouteContent(
            savedStateHandle = entry.savedStateHandle,
            onOpenSetInput = { navController.navigate(SetInputRoute(it)) },
            onAddExercise = onAddExercise,
            onOpenSession = { navController.navigate(SessionRoute(it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable<SetInputRoute> {
        SetInputRouteContent(
            onBack = { navController.popBackStack() },
            onStartRestTimerService = onStartRestTimerService,
        )
    }
    composable<SessionRoute> {
        SessionRouteContent(
            onOpenSummary = { logId ->
                // 요약에서 뒤로 눌러 수행 화면으로 돌아가지 않게 한다.
                // 이미 종료된 세션이라 되돌아갈 자리가 아니다.
                navController.navigate(SessionSummaryRoute(logId)) {
                    popUpTo<SessionRoute> { inclusive = true }
                }
            },
            onOpenSetInput = { navController.navigate(SetInputRoute(it)) },
            onStartRestTimerService = onStartRestTimerService,
            onBack = { navController.popBackStack() },
        )
    }
    composable<SessionSummaryRoute> {
        SessionSummaryRouteContent(
            onDone = {
                // 요약을 닫으면 일지로 돌아간다
                navController.popBackStack()
            },
        )
    }
}

@Composable
private fun WorkoutLogRouteContent(
    savedStateHandle: SavedStateHandle,
    onOpenSetInput: (String) -> Unit,
    onAddExercise: (String) -> Unit,
    onOpenSession: (String) -> Unit,
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
            is WorkoutLogEvent.NavigateToSession -> onOpenSession(event.logId)
            WorkoutLogEvent.NavigateBack -> onBack()
        }
    }

    WorkoutLogScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun SetInputRouteContent(
    onBack: () -> Unit,
    onStartRestTimerService: () -> Unit,
    viewModel: SetInputViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SetInputEvent.NavigateBack -> onBack()
            // 타이머 계산은 이미 시작됐고, 여기서는 백그라운드 유지를 위한
            // Foreground Service 만 띄운다 (FN-TOL-004)
            is SetInputEvent.StartRestTimer -> onStartRestTimerService()
        }
    }

    SetInputScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun SessionRouteContent(
    onOpenSummary: (String) -> Unit,
    onOpenSetInput: (String) -> Unit,
    onStartRestTimerService: () -> Unit,
    onBack: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is SessionEvent.NavigateToSummary -> onOpenSummary(event.logId)
            is SessionEvent.NavigateToSetInput -> onOpenSetInput(event.logExerciseId)
            is SessionEvent.StartRestTimer -> onStartRestTimerService()
            SessionEvent.NavigateBack -> onBack()
        }
    }

    SessionScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun SessionSummaryRouteContent(
    onDone: () -> Unit,
    viewModel: SessionSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SessionSummaryEvent.NavigateHome -> onDone()
            // 별도 코루틴으로 띄운다. 여기서 직접 await 하면 스낵바가 닫힐 때까지
            // 다음 이벤트 수집이 막힌다.
            is SessionSummaryEvent.ShowMessage ->
                scope.launch { snackbarHostState.showSnackbar(event.message) }
        }
    }

    Box {
        SessionSummaryScreen(uiState = uiState, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
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
