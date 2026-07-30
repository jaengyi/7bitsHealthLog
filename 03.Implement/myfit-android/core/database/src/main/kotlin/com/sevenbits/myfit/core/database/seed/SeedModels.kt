package com.sevenbits.myfit.core.database.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 시드 데이터 스키마. `core/database/src/main/assets/seed` 아래 JSON 파일과 1:1 대응한다.
 *
 * **prepackaged DB 대신 JSON 을 쓰는 이유** (04_데이터베이스설계서 §8 대비 변경):
 * prepackaged `.db` 는 Room 이 계산하는 스키마 해시와 정확히 일치해야 하고,
 * 스키마가 바뀔 때마다 재생성해야 한다. 스키마가 자주 바뀌는 개발 단계에서는 마찰이 크다.
 * JSON 은 git diff 로 리뷰 가능하고 스키마 변경에도 매핑 코드만 고치면 된다.
 * 300건 삽입은 단일 트랜잭션에서 100ms 내외로 끝나므로 최초 실행 성능에도 문제가 없다.
 * 시드가 수천 건 규모로 커지면 prepackaged DB 로 전환한다.
 */
@Serializable
internal data class SeedBodyPart(
    val code: String,
    val name: String,
    @SerialName("upperGroup") val upperGroup: String,
    @SerialName("sortOrder") val sortOrder: Int,
)

@Serializable
internal data class SeedExercise(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    /** WEIGHT_REPS / REPS_ONLY / TIME / DISTANCE_TIME / WEIGHT_TIME */
    val recordType: String,
    val exerciseType: String,
    val equipment: String,
    val movementType: String = "COMPOUND",
    val movementPattern: String? = null,
    val isUnilateral: Boolean = false,
    val defaultRestSec: Int? = null,
    /** 주동근 부위 코드 */
    val primary: List<String> = emptyList(),
    /** 보조근 부위 코드 */
    val secondary: List<String> = emptyList(),
    val description: String? = null,
    val caution: String? = null,
    /** 자세 가이드 이미지 asset 경로 (FN-EXR-016) — 콘텐츠 확보 후 채운다 */
    val guideImageAsset: String? = null,
)
