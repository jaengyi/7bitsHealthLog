package com.sevenbits.myfit.feature.settings.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.feature.settings.component.label

/**
 * SCR-CMN-003 마이페이지 / 더보기 허브 (stateless). (06_화면설계서 §3.3)
 *
 * Phase 1 에 없는 항목은 **아예 표시하지 않는다** — 비활성 회색 처리도 하지 않는다.
 * 눌러도 아무 일이 없는 메뉴는 고장으로 읽힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    uiState: MoreUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String = "",
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("더보기") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            ProfileCard(
                profile = uiState.profile,
                onClick = onOpenProfile,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpaceSm))

            MenuRow(icon = "⚙️", title = "환경설정", onClick = onOpenSettings)

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpaceSm))

            Text(
                text = if (appVersion.isBlank()) "MyFit" else "MyFit $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )
            Text(
                text = "기록은 이 기기에만 저장됩니다. 로그인 없이 사용할 수 있어요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.SpaceXs,
                ),
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = profile?.displayName?.takeIf { it.isNotBlank() } ?: "이름 없음",
                style = MaterialTheme.typography.titleMedium,
            )
            val summary = profile.summary()
            Text(
                text = summary ?: "프로필을 채우면 분석이 정확해져요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "게스트 모드",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 프로필 한 줄 요약. 입력된 항목만 이어 붙인다. */
private fun UserProfile?.summary(): String? {
    if (this == null) return null
    val parts = buildList {
        gender?.takeIf { it != Gender.UNSPECIFIED }?.let { add(it.label()) }
        heightCm?.let { add("%.0fcm".format(it)) }
        goalType?.let { add(it.label()) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun MenuRow(icon: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Dimens.MinTouchTarget)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun MoreDarkPreview() {
    MyFitTheme(darkTheme = true) {
        MoreScreen(
            uiState = MoreUiState(
                profile = UserProfile(displayName = "Jaengyi", heightCm = 179.0),
            ),
            onOpenProfile = {},
            onOpenSettings = {},
            appVersion = "1.0.0",
        )
    }
}
