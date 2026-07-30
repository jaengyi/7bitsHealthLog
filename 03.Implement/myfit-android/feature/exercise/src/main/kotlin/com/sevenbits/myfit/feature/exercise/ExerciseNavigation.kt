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
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailEvent
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailScreen
import com.sevenbits.myfit.feature.exercise.detail.ExerciseDetailViewModel
import com.sevenbits.myfit.feature.exercise.editor.ExerciseEditorAction
import com.sevenbits.myfit.feature.exercise.editor.ExerciseEditorEvent
import com.sevenbits.myfit.feature.exercise.editor.ExerciseEditorScreen
import com.sevenbits.myfit.feature.exercise.editor.ExerciseEditorViewModel
import com.sevenbits.myfit.feature.exercise.note.ExerciseNoteAction
import com.sevenbits.myfit.feature.exercise.note.ExerciseNoteScreen
import com.sevenbits.myfit.feature.exercise.note.ExerciseNoteViewModel
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

/** SCR-EXR-003 종목 등록/편집. exerciseId 가 비면 신규 등록 */
@Serializable
data class ExerciseEditorRoute(val exerciseId: String = "")

/** SCR-EXR-004 PT 학습 노트 */
@Serializable
data class ExerciseNoteRoute(val exerciseId: String)

fun NavGraphBuilder.exerciseScreen(navController: NavController) {
    composable<ExerciseRoute> {
        ExercisePickerRoute(
            onOpenDetail = { navController.navigate(ExerciseDetailRoute(it)) },
            onCreateExercise = { navController.navigate(ExerciseEditorRoute()) },
        )
    }
    composable<ExerciseDetailRoute> {
        ExerciseDetailRouteContent(
            onBack = { navController.popBackStack() },
            onOpenNotes = { navController.navigate(ExerciseNoteRoute(it)) },
        )
    }
    composable<ExerciseEditorRoute> {
        ExerciseEditorRouteContent(onBack = { navController.popBackStack() })
    }
    composable<ExerciseNoteRoute> {
        ExerciseNoteRouteContent(onBack = { navController.popBackStack() })
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
    onCreateExercise: () -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreen(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is ExercisePickerAction.OnOpenDetail -> onOpenDetail(action.exerciseId)
                ExercisePickerAction.OnCreateExercise -> onCreateExercise()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun ExerciseDetailRouteContent(
    onBack: () -> Unit,
    onOpenNotes: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ExerciseDetailEvent.NavigateBack -> onBack()
            is ExerciseDetailEvent.OpenPtNotes -> onOpenNotes(event.exerciseId)
            // 링크 추가 다이얼로그는 화면 내부에서 처리한다
            ExerciseDetailEvent.RequestAddReference -> Unit
        }
    }

    ExerciseDetailScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun ExerciseEditorRouteContent(
    onBack: () -> Unit,
    viewModel: ExerciseEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ExerciseEditorEvent.Saved,
            is ExerciseEditorEvent.Deleted,
            ExerciseEditorEvent.NavigateBack,
            -> onBack()
        }
    }

    ExerciseEditorScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is ExerciseEditorAction.OnBack) onBack() else viewModel.onAction(action)
        },
    )
}

@Composable
private fun ExerciseNoteRouteContent(
    onBack: () -> Unit,
    viewModel: ExerciseNoteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseNoteScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is ExerciseNoteAction.OnBack) onBack() else viewModel.onAction(action)
        },
    )
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
