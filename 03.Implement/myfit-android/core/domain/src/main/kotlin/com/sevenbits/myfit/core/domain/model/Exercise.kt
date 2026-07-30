package com.sevenbits.myfit.core.domain.model

/**
 * 운동 종목. (ENT-003 / 04_데이터베이스설계서 §3.3)
 */
data class Exercise(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val recordType: RecordType,
    val exerciseType: ExerciseType,
    val equipment: Equipment,
    val movementType: MovementType = MovementType.COMPOUND,
    /** 밸런스 분석 기준 (FN-RPT-014) */
    val movementPattern: MovementPattern? = null,
    val isUnilateral: Boolean = false,
    val description: String? = null,
    val caution: String? = null,
    /** 앱 내장 asset 경로. 오프라인에서도 열람 가능하다. (FN-EXR-016) */
    val guideImageAsset: String? = null,
    val muscleMapAsset: String? = null,
    val defaultRestSec: Int? = null,
    val isUserDefined: Boolean = false,
    val isFavorite: Boolean = false,
    /** 주동근 부위 코드 */
    val primaryBodyParts: List<String> = emptyList(),
    /** 보조근 부위 코드 */
    val secondaryBodyParts: List<String> = emptyList(),
)

/** 근육 부위 코드 마스터 (ENT-004) */
data class BodyPart(
    val code: String,
    val name: String,
    /** `UPPER` / `LOWER` / `CORE` / `FULL` */
    val upperGroup: String,
    val sortOrder: Int,
)

/**
 * 종목 참고 자료 — 자세 영상·참고 링크. (FN-EXR-004/017)
 *
 * 영상 파일은 저장하지 않고 URL 만 보관한다. (REQ-NFR-009)
 */
data class ExerciseReference(
    val id: String,
    val exerciseId: String,
    val refType: RefType,
    val provider: RefProvider,
    val url: String,
    val videoId: String? = null,
    /** 긴 영상에서 해당 구간만 재생 */
    val startSec: Int? = null,
    val title: String? = null,
    /** 앱 기본 제공(시드) 여부. 사용자는 자기 링크만 삭제할 수 있다. */
    val isDefault: Boolean = false,
)

enum class RefType { VIDEO, ARTICLE }
enum class RefProvider { YOUTUBE, WEB }

/** PT 학습 노트 (ENT-007 / REQ-EXR-008) */
data class ExerciseNote(
    val id: String,
    val exerciseId: String,
    val content: String,
    val trainerName: String? = null,
    /** ISO-8601 `yyyy-MM-dd` */
    val noteDate: String,
)

/** 종목 검색·필터 조건 (FN-EXR-008/009) */
data class ExerciseFilter(
    val query: String = "",
    val bodyPartCodes: Set<String> = emptySet(),
    val equipments: Set<Equipment> = emptySet(),
    val favoriteOnly: Boolean = false,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && bodyPartCodes.isEmpty() && equipments.isEmpty() && !favoriteOnly
}
