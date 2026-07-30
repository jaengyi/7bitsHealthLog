package com.sevenbits.myfit.core.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sevenbits.myfit.core.designsystem.theme.Dimens

/**
 * 자세 영상 플레이어. (FN-EXR-017 / 02_개발환경및빌드정의서 §5.3)
 *
 * **영상 파일을 앱에 내장하거나 서버에 저장하지 않는다.** 외부 영상 서비스의 공식
 * 임베드 플레이어만 사용한다. (REQ-NFR-009)
 *
 * 서드파티 플레이어 라이브러리를 도입하지 않고 Android 내장 WebView 로 처리한다 —
 * 의존성과 APK 크기가 늘지 않는다.
 *
 * **오프라인 처리가 이 컴포넌트의 핵심이다.** REQ-NFR-003(완전 오프라인)과 외부 영상은
 * 본질적으로 충돌하므로, 재생 불가 시 빈 플레이어가 아니라 명시적 안내로 대체한다.
 * 같은 화면의 설명·자세 이미지는 내장 asset 이라 정상 표시된다.
 */
@Composable
fun GuideVideoPlayer(
    videoId: String?,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    startSec: Int? = null,
    fallbackUrl: String? = null,
) {
    if (videoId == null && fallbackUrl == null) return

    if (!isOnline) {
        OfflineVideoNotice(modifier = modifier, fallbackUrl = fallbackUrl)
        return
    }

    if (videoId == null) {
        // 임베드 불가 링크는 외부 앱으로만 연결한다
        ExternalVideoLink(url = fallbackUrl!!, modifier = modifier)
        return
    }

    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT_RATIO),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.apply {
                    javaScriptEnabled = true // IFrame Player 필수
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = true // 자동 재생 차단
                    allowFileAccess = false // 로컬 파일 접근 차단
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewClient = object : WebViewClient() {
                    /** 임베드 도메인 외 링크는 외부 브라우저로 넘긴다 */
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val host = request.url.host.orEmpty()
                        return if (EMBED_HOSTS.any { host.endsWith(it) }) {
                            false
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            true
                        }
                    }
                }
            }
        },
        update = { webView ->
            webView.loadUrl(embedUrl(videoId, startSec))
        },
    )
}

/**
 * 추적 쿠키를 최소화하기 위해 `youtube-nocookie.com` 임베드 도메인을 사용한다.
 * 개인 사용 목적에 부합한다.
 */
private fun embedUrl(videoId: String, startSec: Int?): String {
    val start = startSec?.takeIf { it > 0 }?.let { "&start=$it" } ?: ""
    return "https://www.youtube-nocookie.com/embed/$videoId?rel=0&playsinline=1$start"
}

@Composable
private fun OfflineVideoNotice(modifier: Modifier = Modifier, fallbackUrl: String?) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(VIDEO_ASPECT_RATIO),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                modifier = Modifier.padding(Dimens.SpaceMd),
            ) {
                Text(
                    text = "오프라인에서는 영상을 재생할 수 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "설명과 자세 이미지는 그대로 확인할 수 있습니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExternalVideoLink(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        modifier = modifier,
    ) {
        Text("영상 보기 (외부 앱)")
    }
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private val EMBED_HOSTS = listOf("youtube.com", "youtube-nocookie.com", "youtu.be")
