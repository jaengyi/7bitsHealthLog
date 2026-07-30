package com.sevenbits.myfit.feature.calendar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * 캘린더 네비게이션 그래프.
 *
 * 각 feature 가 자신의 NavGraphBuilder 확장을 노출하고 :app 의 NavHost 가 조립한다.
 * 이 방식으로 feature 간 직접 참조 없이(R-3) 화면 이동이 가능하다.
 */
@Serializable
data object CalendarRoute

fun NavGraphBuilder.calendarScreen() {
    composable<CalendarRoute> { CalendarScreen() }
}
