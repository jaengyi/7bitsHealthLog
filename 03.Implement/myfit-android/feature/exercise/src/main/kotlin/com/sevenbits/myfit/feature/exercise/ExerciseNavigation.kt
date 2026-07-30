package com.sevenbits.myfit.feature.exercise

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
import androidx.navigation.toRoute
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailAction
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailEvent
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailScreen
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailViewModel
import com.sevenbits.myfit.feature.exercise.picker.ExercisePickerAction
import com.sevenbits.myfit.feature.exercise.picker.ExercisePickerScreen
import com.sevenbits.myfit.feature.exercise.picker.ExercisePickerViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * 종목 네비게이션 그래프.
 *
 * 각 feature 가 자신의 NavGraphBuilder 확장을 노출하고 :app 의 NavHost 가 조립한다.
 * 이 방식으로 feature 간 직접 참조 없이(R-3) 화면 이동이 가능하다.
 */
@Serializable
data object ExerciseRoute

/** SCR-EXR-002 종목 상세 */
@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

fun NavGraphBuilder.exerciseScreen(navController: NavController) {
    composable<ExerciseRoute> {
        ExercisePickerRoute(
            onOpenDetail = { navController.navigate(ExerciseDetailRoute(it)) },
        )
    }
    composable<ExerciseDetailRoute> {
        ExerciseDetailRouteContent(onBack = { navController.popBackStack() })
    }
}

/**
 * ViewModel 을 주입받아 stateless 화면을 감싸는 진입점.
 *
 * ViewModel 은 최상위에서만 주입하고 하위 Composable 에는 전달하지 않는다. (C1/C5)
 */
@Composable
private fun ExercisePickerRoute(
    onOpenDetail: (String) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is ExercisePickerAction.OnOpenDetail) {
                onOpenDetail(action.exerciseId)
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

@Composable
private fun ExerciseDetailRouteContent(
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ExerciseDetailEvent.NavigateBack -> onBack()
            // PT 노트·링크 추가 화면은 후속 구현
            else -> Unit
        }
    }

    ExerciseDetailScreen(uiState = uiState, onAction = viewModel::onAction)
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
