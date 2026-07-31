package com.sevenbits.myfit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sevenbits.myfit.R
import com.sevenbits.myfit.feature.calendar.CalendarRoute
import com.sevenbits.myfit.feature.calendar.calendarScreen
import com.sevenbits.myfit.feature.exercise.ExerciseRoute
import com.sevenbits.myfit.feature.exercise.exerciseScreen
import com.sevenbits.myfit.feature.routine.RoutineRoute
import com.sevenbits.myfit.feature.routine.routineScreen
import com.sevenbits.myfit.feature.settings.SettingsRoute
import com.sevenbits.myfit.feature.settings.settingsScreen
import com.sevenbits.myfit.feature.workout.WorkoutRoute
import com.sevenbits.myfit.feature.workout.workoutScreen

/**
 * 하단 탭 5종. (06_화면설계서 §2)
 *
 * 가장 빈번한 동선인 "오늘 기록"을 중앙 탭 1회 터치로 도달하게 배치한다.
 */
enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(WorkoutRoute, Icons.Default.Home, R.string.nav_home),
    CALENDAR(CalendarRoute, Icons.Default.CalendarMonth, R.string.nav_calendar),
    RECORD(WorkoutRoute, Icons.Default.Add, R.string.nav_record),
    ROUTINE(RoutineRoute, Icons.Default.ListAlt, R.string.nav_routine),
    MORE(SettingsRoute, Icons.Default.Menu, R.string.nav_more),
}

/**
 * 최상위 NavHost.
 *
 * 각 feature 모듈이 노출한 NavGraphBuilder 확장을 조립한다.
 * feature 간 직접 참조가 없으므로 의존 규칙 R-3 을 만족한다.
 */
@Composable
fun MyFitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = WorkoutRoute,
        modifier = modifier,
    ) {
        // 일지에서 "종목 추가" -> 종목 선택 화면. 결과는 SavedStateHandle 로 돌아온다.
        workoutScreen(
            navController = navController,
            onAddExercise = { navController.navigate(ExerciseRoute) },
        )
        exerciseScreen(navController)
        routineScreen()
        calendarScreen()
        settingsScreen()
    }
}
