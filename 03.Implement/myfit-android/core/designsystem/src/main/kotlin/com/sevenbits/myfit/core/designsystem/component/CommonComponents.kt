package com.sevenbits.myfit.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme

/**
 * 파괴적 동작 확인 다이얼로그. (U4 / REQ-NFR-002)
 *
 * 땀·장갑 착용 상태의 오조작을 고려해 삭제·초기화에는 반드시 확인 절차를 둔다.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "취소",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/**
 * 빈 상태 표시. (06_화면설계서 §1.5)
 *
 * 단순히 "데이터 없음"을 알리는 데 그치지 않고 **다음 행동을 유도하는 버튼**을 함께 둔다.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Preview(name = "dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    MyFitTheme(darkTheme = true) {
        EmptyState(
            message = "오늘의 첫 종목을 추가해 보세요",
            actionLabel = "종목 추가",
            onAction = {},
        )
    }
}

@Preview(name = "light", showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    MyFitTheme(darkTheme = false) {
        ConfirmDialog(
            title = "일지를 삭제할까요?",
            message = "이 일지의 종목과 세트 기록이 모두 삭제됩니다.",
            confirmLabel = "삭제",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
