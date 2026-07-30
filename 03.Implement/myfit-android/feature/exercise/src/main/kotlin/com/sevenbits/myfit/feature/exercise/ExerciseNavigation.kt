package com.sevenbits.myfit.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sevenbits.myfit.feature.exercise.picker.ExercisePickerScreen
import com.sevenbits.myfit.feature.exercise.picker.ExercisePickerViewModel
import kotlinx.serialization.Serializable

/**
 * 종목 네비게이션 그래프.
 *
 * 각 feature 가 자신의 NavGraphBuilder 확장을 노출하고 :app 의 NavHost 가 조립한다.
 * 이 방식으로 feature 간 직접 참조 없이(R-3) 화면 이동이 가능하다.
 */
@Serializable
data object ExerciseRoute

fun NavGraphBuilder.exerciseScreen() {
    composable<ExerciseRoute> { ExercisePickerRoute() }
}

/**
 * ViewModel 을 주입받아 stateless 화면을 감싸는 진입점.
 *
 * ViewModel 은 최상위에서만 주입하고 하위 Composable 에는 전달하지 않는다. (C1/C5)
 */
@Composable
private fun ExercisePickerRoute(
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreen(uiState = uiState, onAction = viewModel::onAction)
}
