package com.sevenbits.myfit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 루틴 — 반복 수행 종목 묶음 템플릿. (ENT-011 / 04_데이터베이스설계서 §3.11)
 *
 * 루틴 원본과 실제 일지는 완전히 분리된다. 일지 수정이 루틴 정의에 영향을 주지 않는다. (REQ-RTN-003)
 */
@Entity(
    tableName = "routine",
    indices = [Index("user_id"), Index("last_performed_date")],
)
internal data class RoutineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    /** 내장 프리셋은 NULL (FN-RTN-017) */
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    /** 대상 부위 코드 CSV — 예: `CHEST,SHOULDER` */
    @ColumnInfo(name = "target_body_parts") val targetBodyParts: String? = null,
    @ColumnInfo(name = "preset_category") val presetCategory: String? = null,
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    /** 목록 정렬 기준 (FN-RTN-002) */
    @ColumnInfo(name = "use_count") val useCount: Int = 0,
    /** 순환 배정의 다음 루틴 판정 기준 (FN-RTN-011) */
    @ColumnInfo(name = "last_performed_date") val lastPerformedDate: String? = null,
    @ColumnInfo(name = "version") val version: Int = 1,
    /** 논리 삭제 — 참조 일지의 루틴명은 스냅샷으로 보존된다 (FN-RTN-004) */
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/**
 * 루틴 내 종목과 목표값. (ENT-012 / §3.12)
 *
 * `target_weight_kg` 와 `target_percent_1rm` 은 **XOR 관계**다.
 * 비율이 지정되면 인스턴스화 시점에 `추정1RM × 비율` 로 환산한다. (FN-RTN-006)
 */
@Entity(
    tableName = "routine_exercise",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
        ),
    ],
    indices = [Index(value = ["routine_id", "order_no"]), Index("exercise_id")],
)
internal data class RoutineExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "order_no") val orderNo: Int,
    @ColumnInfo(name = "target_set_count") val targetSetCount: Int? = null,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double? = null,
    /** %1RM. `target_weight_kg` 와 택일 (REQ-RTN-002) */
    @ColumnInfo(name = "target_percent_1rm") val targetPercent1rm: Double? = null,
    @ColumnInfo(name = "target_reps") val targetReps: Int? = null,
    @ColumnInfo(name = "rest_sec") val restSec: Int? = null,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/**
 * 삭제 이력. 동기화 환경에서 삭제 사실을 서버로 전파한다. (ENT-024 / §3.27)
 *
 * 서버 전파 완료 후 90일 경과분은 정리한다(무한 증가 방지).
 */
@Entity(
    tableName = "tombstone",
    indices = [Index(value = ["entity_name", "target_id"]), Index("is_dirty")],
)
internal data class TombstoneEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "entity_name") val entityName: String,
    @ColumnInfo(name = "target_id") val targetId: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)
