package com.sevenbits.myfit.core.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 자세 가이드 이미지. (FN-EXR-016)
 *
 * 앱 내장 asset 이므로 **네트워크 없이 항상 표시된다.** 오프라인 원칙(REQ-NFR-003)에서
 * 영상과 달리 이미지가 예외가 아닌 이유다.
 *
 * Coil 을 쓰지 않고 직접 디코딩한다 — asset 경로는 URL 이 아니고,
 * 이미지 1장을 위해 이미지 로딩 라이브러리의 캐시 계층을 태울 이유가 없다.
 *
 * @param assetPath `guide/ex-0001.webp` 형태의 asset 상대 경로. null 이면 아무것도 그리지 않는다.
 */
@Composable
fun GuideImage(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (assetPath == null) return

    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // 디코딩 실패·로딩 중에도 레이아웃이 무너지지 않도록 자리를 유지한다
            Text(
                text = "",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
