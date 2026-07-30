package com.sevenbits.myfit.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.RecordType

/**
 * 종목 자세 가이드 본문. (REQ-EXR-003 / 06_화면설계서 §3.8)
 *
 * 미리보기 바텀시트(SCR-EXR-001)와 상세 화면(SCR-EXR-002)이 **같은 본문을 공유**한다.
 * 두 곳에서 내용이 갈리면 유지보수가 어려워지므로 하나로 둔다.
 *
 * 표시 순서는 오프라인 가용성 순이다 — 이미지(항상) → 영상(온라인) → 텍스트(항상).
 */
@Composable
fun ExerciseGuideContent(
    exercise: Exercise,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    videoId: String? = null,
    videoStartSec: Int? = null,
    videoUrl: String? = null,
    ptNoteCount: Int = 0,
    bodyPartLabels: List<String> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(exercise.name, style = MaterialTheme.typography.headlineMedium)

        Text(
            text = buildSubtitle(exercise, bodyPartLabels),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 앱 내장 asset — 오프라인에서도 항상 표시된다 (FN-EXR-016)
        GuideImage(
            assetPath = exercise.guideImageAsset,
            contentDescription = "${exercise.name} 자세 가이드",
        )

        // 외부 링크 — 온라인에서만 재생된다 (FN-EXR-017)
        GuideVideoPlayer(
            videoId = videoId,
            isOnline = isOnline,
            startSec = videoStartSec,
            fallbackUrl = videoUrl,
        )

        if (!exercise.description.isNullOrBlank()) {
            GuideSection(title = "수행 방법", body = exercise.description!!)
        }
        if (!exercise.caution.isNullOrBlank()) {
            GuideSection(title = "⚠️ 주의사항", body = exercise.caution!!)
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            LabeledValue("기록 유형", exercise.recordType.label())
            if (ptNoteCount > 0) {
                LabeledValue("내 PT 노트", "${ptNoteCount}건")
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun buildSubtitle(exercise: Exercise, bodyPartLabels: List<String>): String {
    val equipment = when (exercise.equipment) {
        Equipment.BARBELL -> "바벨"
        Equipment.DUMBBELL -> "덤벨"
        Equipment.MACHINE -> "머신"
        Equipment.CABLE -> "케이블"
        Equipment.BODYWEIGHT -> "맨몸"
        Equipment.ETC -> "기타"
    }
    val parts = bodyPartLabels.ifEmpty { exercise.primaryBodyParts }
    return listOfNotNull(
        equipment,
        parts.joinToString("·").takeIf { it.isNotBlank() },
        if (exercise.isUnilateral) "좌우 분리" else null,
    ).joinToString(" · ")
}

private fun RecordType.label(): String = when (this) {
    RecordType.WEIGHT_REPS -> "중량 + 횟수"
    RecordType.REPS_ONLY -> "횟수"
    RecordType.TIME -> "시간"
    RecordType.DISTANCE_TIME -> "거리 + 시간"
    RecordType.WEIGHT_TIME -> "중량 + 시간"
}

// ── Preview ──────────────────────────────────────────────

private val sample = Exercise(
    id = "ex-0001",
    name = "바벨 벤치프레스",
    nameEn = "Barbell Bench Press",
    recordType = RecordType.WEIGHT_REPS,
    exerciseType = ExerciseType.WEIGHT,
    equipment = Equipment.BARBELL,
    description = "견갑을 후인·하강하고 가슴을 연 상태로 벤치에 눕는다. " +
        "바를 어깨너비보다 약간 넓게 잡고 명치 아래로 내렸다가 밀어 올린다.",
    caution = "손목이 꺾이지 않도록 유지하고, 바운싱으로 반동을 주지 않는다.",
    primaryBodyParts = listOf("가슴"),
)

@Preview(name = "dark - 온라인", showBackground = true)
@Composable
private fun GuideContentOnlinePreview() {
    MyFitTheme(darkTheme = true) {
        ExerciseGuideContent(exercise = sample, isOnline = true, videoId = "dummy", ptNoteCount = 2)
    }
}

@Preview(name = "light - 오프라인", showBackground = true)
@Composable
private fun GuideContentOfflinePreview() {
    MyFitTheme(darkTheme = false) {
        ExerciseGuideContent(exercise = sample, isOnline = false, videoId = "dummy")
    }
}
