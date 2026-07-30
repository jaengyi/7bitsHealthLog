package com.sevenbits.myfit.feature.exercise.picker

import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise

/**
 * SCR-EXR-001 종목 선택 화면 상태. (06_화면설계서 §3.7)
 *
 * 화면을 그리는 데 필요한 전부를 단일 data class 로 노출한다. (03_모듈설계서 §4.1)
 */
data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val bodyPartFilters: Set<String> = emptySet(),
    val equipmentFilters: Set<Equipment> = emptySet(),
    val availableBodyParts: List<BodyPart> = emptyList(),

    /** 최근 사용 — 종목 추가의 대부분이 여기서 끝난다 (FN-EXR-011) */
    val recent: List<Exercise> = emptyList(),
    val favorites: List<Exercise> = emptyList(),
    val results: List<Exercise> = emptyList(),

    val selectedIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,

    /** 추가 전 미리보기 시트 (FN-EXR-018, v1.1) */
    val preview: ExercisePreview? = null,
) {
    /** 검색·필터가 걸리면 섹션 구분 없이 결과만 보여준다. */
    val isFiltering: Boolean
        get() = query.isNotBlank() || bodyPartFilters.isNotEmpty() || equipmentFilters.isNotEmpty()

    val selectedCount: Int get() = selectedIds.size

    val showEmptyState: Boolean
        get() = !isLoading && !isSearching && isFiltering && results.isEmpty()
}

/**
 * 미리보기 시트 데이터.
 *
 * 설명·자세 이미지는 앱 내장이라 오프라인에서도 표시되고,
 * 영상만 네트워크가 필요하다. (REQ-NFR-003 예외, 06 §3.8)
 */
data class ExercisePreview(
    val exercise: Exercise,
    val videoUrl: String? = null,
    val videoId: String? = null,
    val videoStartSec: Int? = null,
    val ptNoteCount: Int = 0,
    val isOnline: Boolean = true,
)

/** 사용자 의도. Screen → ViewModel 단방향. */
sealed interface ExercisePickerAction {
    data class OnQueryChange(val query: String) : ExercisePickerAction
    data class OnBodyPartToggle(val code: String) : ExercisePickerAction
    data class OnEquipmentToggle(val equipment: Equipment) : ExercisePickerAction
    data class OnSelectionToggle(val exerciseId: String) : ExercisePickerAction
    data class OnFavoriteToggle(val exerciseId: String) : ExercisePickerAction
    data class OnPreviewOpen(val exerciseId: String) : ExercisePickerAction
    /** 시트에서 전체 화면(SCR-EXR-002)으로 확장 */
    data class OnOpenDetail(val exerciseId: String) : ExercisePickerAction
    data object OnPreviewClose : ExercisePickerAction
    data object OnConfirmSelection : ExercisePickerAction
    data object OnClearFilters : ExercisePickerAction
}

/** 1회성 효과. 상태에 남기지 않는다. */
sealed interface ExercisePickerEvent {
    data class SelectionConfirmed(val exerciseIds: List<String>) : ExercisePickerEvent
    data class OpenExerciseDetail(val exerciseId: String) : ExercisePickerEvent
    data class ShowMessage(val message: String) : ExercisePickerEvent
}
