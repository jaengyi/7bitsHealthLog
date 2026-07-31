package com.sevenbits.myfit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.feature.settings.OnboardingHost
import com.sevenbits.myfit.navigation.MyFitNavHost
import com.sevenbits.myfit.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 알림 권한 요청 런처. (API 33+)
     *
     * 거부되어도 앱은 정상 동작한다 — 타이머 계산은 알림과 무관하다.
     * 다만 화면을 벗어나면 잔여 시간을 볼 수 없으므로 사용자에게 손해다.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 결과와 무관하게 진행 */ }

    internal fun launchNotificationPermission() {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val startState by viewModel.startState.collectAsStateWithLifecycle()

            // 기본값은 다크 — 헬스장 조명 환경을 고려한 선택이다 (REQ-CMN-005)
            MyFitTheme(darkTheme = themeMode.isDark()) {
                when (startState) {
                    // DB 판정 전에 화면을 그리면 홈이 잠깐 보였다가 온보딩으로 튄다
                    StartState.Loading -> Box(Modifier.fillMaxSize())
                    StartState.Onboarding ->
                        OnboardingHost(onFinished = viewModel::onOnboardingFinished)
                    StartState.Main -> MyFitApp()
                }
            }
        }
    }
}

private fun ComponentActivity.requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val granted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        (this as MainActivity).launchNotificationPermission()
    }
}

/** SYSTEM 은 기기 설정을 따른다 */
@Composable
private fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
private fun MyFitApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { MyFitBottomBar(navController) },
    ) { innerPadding ->
        MyFitNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun MyFitBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route

    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = currentRouteName?.contains(
                    destination.route::class.simpleName.orEmpty(),
                ) == true,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
