package com.sevenbits.myfit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sevenbits.myfit.BuildConfig
import com.sevenbits.myfit.R
import com.sevenbits.myfit.feature.calendar.CalendarRoute
import com.sevenbits.myfit.feature.calendar.calendarScreen
import androidx.compose.ui.platform.LocalContext
import com.sevenbits.myfit.service.RestTimerService
import com.sevenbits.myfit.feature.exercise.ExerciseRoute
import com.sevenbits.myfit.feature.exercise.exerciseScreen
import com.sevenbits.myfit.feature.routine.RoutineRoute
import com.sevenbits.myfit.feature.routine.routineScreen
import com.sevenbits.myfit.feature.settings.MoreRoute
import com.sevenbits.myfit.feature.settings.settingsScreen
import com.sevenbits.myfit.feature.workout.WorkoutRoute
import com.sevenbits.myfit.feature.workout.workoutScreen

/**
 * 하단 탭. (06_화면설계서 §2)
 *
 * 설계상 5종이지만 Phase 1 에서는 **4종**이다. 홈(SCR-CMN-001)이 아직 없어
 * 첫 탭이 오늘 일지를 직접 연다 — 중앙 "기록" 탭과 목적지가 같아진다.
 * 같은 화면을 가리키는 탭 두 개를 두면 둘 다 선택 표시가 켜져 고장으로 보인다.
 * 홈 화면을 구현할 때 중앙 탭을 복원한다.
 */
enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(WorkoutRoute, Icons.Default.Home, R.string.nav_record),
    CALENDAR(CalendarRoute, Icons.Default.CalendarMonth, R.string.nav_calendar),
    ROUTINE(RoutineRoute, Icons.Default.ListAlt, R.string.nav_routine),
    MORE(MoreRoute, Icons.Default.Menu, R.string.nav_more),
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
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = WorkoutRoute,
        modifier = modifier,
    ) {
        // 일지에서 "종목 추가" -> 종목 선택 화면. 결과는 SavedStateHandle 로 돌아온다.
        workoutScreen(
            navController = navController,
            onAddExercise = { navController.navigate(ExerciseRoute) },
            onStartRestTimerService = { RestTimerService.start(context) },
        )
        exerciseScreen(navController)
        routineScreen(
            navController = navController,
            onAddExercise = { navController.navigate(ExerciseRoute) },
            onOpenLog = { date -> navController.navigate(WorkoutRoute(date)) },
        )
        calendarScreen(onOpenLog = { date -> navController.navigate(WorkoutRoute(date)) })
        settingsScreen(navController = navController, appVersion = BuildConfig.VERSION_NAME)
    }
}
