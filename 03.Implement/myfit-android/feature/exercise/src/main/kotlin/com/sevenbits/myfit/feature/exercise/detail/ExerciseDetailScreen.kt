package com.sevenbits.myfit.feature.exercise.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sevenbits.myfit.core.designsystem.theme.Dimens
import com.sevenbits.myfit.core.designsystem.theme.MyFitTheme
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseReference
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.RefProvider
import com.sevenbits.myfit.core.domain.model.RefType
import com.sevenbits.myfit.core.ui.ExerciseGuideContent

/**
 * SCR-EXR-002 종목 상세 · 자세 가이드 (stateless). (06_화면설계서 §3.8)
 *
 * 미리보기 시트와 **본문을 공유**하고(ExerciseGuideContent),
 * 상세 화면에만 있는 것은 사용자 참고 링크 관리와 PT 노트 진입점이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    uiState: ExerciseDetailUiState,
    onAction: (ExerciseDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name.orEmpty()) },
                navigationIcon = {
                    TextButton(onClick = { onAction(ExerciseDetailAction.OnBack) }) { Text("←") }
                },
                actions = {
                    val exercise = uiState.exercise ?: return@TopAppBar
                    IconButton(onClick = { onAction(ExerciseDetailAction.OnFavoriteToggle) }) {
                        Icon(
                            imageVector = if (exercise.isFavorite) {
                                Icons.Default.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = if (exercise.isFavorite) "즐겨찾기 해제" else "즐겨찾기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val exercise = uiState.exercise
        if (uiState.isLoading || exercise == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ExerciseGuideContent(
                exercise = exercise,
                isOnline = uiState.isOnline,
                videoId = uiState.defaultVideo?.videoId,
                videoStartSec = uiState.defaultVideo?.startSec,
                videoUrl = uiState.defaultVideo?.url,
                ptNoteCount = uiState.ptNoteCount,
                bodyPartLabels = uiState.bodyPartLabels,
                modifier = Modifier.weight(1f),
            )

            HorizontalDivider()

            UserReferenceSection(
                references = uiState.userReferences,
                onAdd = { onAction(ExerciseDetailAction.OnAddReferenceClick) },
                onRemove = { onAction(ExerciseDetailAction.OnRemoveReference(it)) },
            )

            TextButton(
                onClick = { onAction(ExerciseDetailAction.OnOpenPtNotes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text("📝 내 PT 학습 노트 (${uiState.ptNoteCount}건)")
            }
        }
    }
}

@Composable
private fun UserReferenceSection(
    references: List<ExerciseReference>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "참고 영상 / 링크",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onAdd) { Text("＋ 추가") }
        }

        // 기본 제공 링크는 목록에 포함되지 않는다 — 사용자가 관리하는 것만 노출한다
        references.forEach { reference ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reference.title ?: reference.url,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(reference.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "링크 삭제")
                }
            }
        }
    }
}

// ── Preview ──────────────────────────────────────────────

private val sampleExercise = Exercise(
    id = "ex-0001",
    name = "바벨 벤치프레스",
    recordType = RecordType.WEIGHT_REPS,
    exerciseType = ExerciseType.WEIGHT,
    equipment = Equipment.BARBELL,
    description = "견갑을 후인·하강하고 가슴을 연 상태로 벤치에 눕는다.",
    caution = "손목이 꺾이지 않도록 유지한다.",
    isFavorite = true,
    primaryBodyParts = listOf("CHEST"),
)

@Preview(name = "dark", showBackground = true)
@Composable
private fun ExerciseDetailDarkPreview() {
    MyFitTheme(darkTheme = true) {
        ExerciseDetailScreen(
            uiState = ExerciseDetailUiState(
                isLoading = false,
                exercise = sampleExercise,
                bodyPartLabels = listOf("가슴"),
                ptNoteCount = 2,
                userReferences = listOf(
                    ExerciseReference(
                        id = "r1",
                        exerciseId = "ex-0001",
                        refType = RefType.VIDEO,
                        provider = RefProvider.YOUTUBE,
                        url = "https://youtu.be/xxxx",
                        title = "김트레이너 벤치 교정 영상",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "light - 오프라인", showBackground = true)
@Composable
private fun ExerciseDetailOfflinePreview() {
    MyFitTheme(darkTheme = false) {
        ExerciseDetailScreen(
            uiState = ExerciseDetailUiState(
                isLoading = false,
                exercise = sampleExercise,
                isOnline = false,
            ),
            onAction = {},
        )
    }
}
