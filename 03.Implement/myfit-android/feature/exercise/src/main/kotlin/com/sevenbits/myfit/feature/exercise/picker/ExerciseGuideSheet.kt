package com.sevenbits.myfit.feature.exercise.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.ui.ExerciseGuideContent

/**
 * 종목 상세 미리보기 시트. (FN-EXR-018 / 06_화면설계서 §3.7)
 *
 * **전체 화면이 아니라 시트인 이유**: 종목 선택 중에 화면이 전환되면 선택 상태와
 * 스크롤 위치가 끊긴다. 시트는 뒤에 목록이 그대로 남아 "확인 → 닫기 → 계속 선택"
 * 흐름이 유지된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseGuideSheet(
    preview: ExercisePreview,
    isSelected: Boolean,
    onAction: (ExercisePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(ExercisePickerAction.OnPreviewClose) },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            ExerciseGuideContent(
                exercise = preview.exercise,
                isOnline = preview.isOnline,
                videoId = preview.videoId,
                videoStartSec = preview.videoStartSec,
                videoUrl = preview.videoUrl,
                ptNoteCount = preview.ptNoteCount,
                modifier = Modifier.weight(1f, fill = false),
            )

            // 하단 고정 — 시트를 닫지 않고도 바로 선택할 수 있다 (U1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                TextButton(
                    onClick = { onAction(ExercisePickerAction.OnPreviewClose) },
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text("닫기")
                }
                Button(
                    onClick = {
                        onAction(ExercisePickerAction.OnSelectionToggle(preview.exercise.id))
                        onAction(ExercisePickerAction.OnPreviewClose)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(if (isSelected) "선택 해제" else "＋ 종목 추가")
                }
            }
        }
    }
}
