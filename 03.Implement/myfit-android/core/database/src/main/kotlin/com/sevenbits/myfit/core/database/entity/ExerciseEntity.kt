package com.sevenbits.myfit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 운동종목 마스터. (ENT-003 / 04_데이터베이스설계서 §3.3)
 *
 * Entity 는 모듈 밖으로 노출하지 않는다(R-5). 경계는 항상 Domain Model 이다.
 */
@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["name"]),
        Index(value = ["name_chosung"]),
        Index(value = ["exercise_type", "equipment", "is_active"]),
    ],
)
internal data class ExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String? = null,

    /** 초성 인덱스 — 검색 300ms 요구 충족을 위해 사전 계산해 저장한다 (REQ-EXR-005) */
    @ColumnInfo(name = "name_chosung")
    val nameChosung: String,

    @ColumnInfo(name = "record_type")
    val recordType: String,

    @ColumnInfo(name = "exercise_type")
    val exerciseType: String,

    @ColumnInfo(name = "equipment")
    val equipment: String,

    @ColumnInfo(name = "movement_type")
    val movementType: String = "COMPOUND",

    /** 밸런스 분석 기준 (FN-RPT-014) */
    @ColumnInfo(name = "movement_pattern")
    val movementPattern: String? = null,

    @ColumnInfo(name = "is_unilateral")
    val isUnilateral: Boolean = false,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "caution")
    val caution: String? = null,

    /** 자세 가이드 이미지 asset 경로 — 오프라인 열람 보장 (FN-EXR-016) */
    @ColumnInfo(name = "guide_image_asset")
    val guideImageAsset: String? = null,

    @ColumnInfo(name = "muscle_map_asset")
    val muscleMapAsset: String? = null,

    @ColumnInfo(name = "default_rest_sec")
    val defaultRestSec: Int? = null,

    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** LWW 충돌 해소 기준 (REQ-DAT-002) */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    /** 로컬 전용 — 미동기화 변경분 표시. 서버로 전송하지 않는다. */
    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean = true,
)
