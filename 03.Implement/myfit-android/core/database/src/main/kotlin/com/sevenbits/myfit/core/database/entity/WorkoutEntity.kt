package com.sevenbits.myfit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 운동일지 — 일자·회차 단위 세션 헤더. (ENT-008 / 04_데이터베이스설계서 §3.8)
 *
 * `total_*` 4개 컬럼은 캘린더·리포트 조회 성능(REQ-NFR-001)을 위한 **집계 캐시**다.
 * 원본은 항상 `workout_set` 이며, 세트 변경 시 동일 트랜잭션에서 재계산한다.
 */
@Entity(
    tableName = "workout_log",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // 회차 중복 방지 (FN-WRK-001) — 하루 최대 3회차 (REQ-WRK-002)
        Index(value = ["user_id", "workout_date", "session_no"], unique = true),
        // 캘린더 월간 조회·리포트 집계 축
        Index(value = ["user_id", "workout_date"]),
        // 진행 중 세션 복원 (FN-WRK-023)
        Index("status"),
        Index("routine_id"),
    ],
)
internal data class WorkoutLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    /** `yyyy-MM-dd` */
    @ColumnInfo(name = "workout_date") val workoutDate: String,
    /** 1~3 (REQ-WRK-002) */
    @ColumnInfo(name = "session_no") val sessionNo: Int = 1,
    /** 예: "오전 헬스", "저녁 홈트" */
    @ColumnInfo(name = "session_name") val sessionName: String? = null,
    /** `PLANNED` / `IN_PROGRESS` / `COMPLETED` */
    @ColumnInfo(name = "status") val status: String = "PLANNED",
    @ColumnInfo(name = "routine_id") val routineId: String? = null,
    /** 루틴이 삭제되어도 과거 일지에 이름을 표시하기 위한 스냅샷 (FN-RTN-004) */
    @ColumnInfo(name = "routine_name_snapshot") val routineNameSnapshot: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long? = null,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "duration_sec") val durationSec: Int? = null,
    @ColumnInfo(name = "total_volume_kg") val totalVolumeKg: Double? = null,
    @ColumnInfo(name = "total_set_count") val totalSetCount: Int? = null,
    @ColumnInfo(name = "total_reps") val totalReps: Int? = null,
    @ColumnInfo(name = "avg_intensity_pct") val avgIntensityPct: Double? = null,
    /** 일지 단위 메모 — 오늘 컨디션 (FN-WRK-024) */
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "condition_sleep") val conditionSleep: Int? = null,
    @ColumnInfo(name = "condition_overall") val conditionOverall: Int? = null,
    @ColumnInfo(name = "condition_soreness") val conditionSoreness: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/** 일지 내 수행 종목 항목. (ENT-009 / §3.9) */
@Entity(
    tableName = "workout_log_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_log_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
        ),
    ],
    indices = [
        Index(value = ["workout_log_id", "order_no"]),
        // 종목별 성장 추이 조회 (FN-RPT-007)
        Index("exercise_id"),
    ],
)
internal data class WorkoutLogExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "workout_log_id") val workoutLogId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    /** 드래그 정렬 순번 (FN-WRK-018) */
    @ColumnInfo(name = "order_no") val orderNo: Int,
    /** 동일 값끼리 슈퍼세트 (FN-WRK-012) */
    @ColumnInfo(name = "superset_group") val supersetGroup: Int? = null,
    /** 종목 단위 메모 — 자극점·통증 (FN-WRK-025) */
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/**
 * 세트 — 실제 기록의 최소 단위. (ENT-010 / §3.10) **핵심 테이블**
 *
 * 기록 유형 5종(REQ-EXR-007)을 수용하기 위해 유형별 컬럼을 모두 nullable 로 둔다.
 * 유형별 필수 필드 검증은 DB 제약이 아니라 Domain 계층에서 수행한다. (REQ-NFR-006)
 */
@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["log_exercise_id", "set_no"]),
        // 진행률 산출 (FN-WRK-021)
        Index("is_completed"),
    ],
)
internal data class WorkoutSetEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "log_exercise_id") val logExerciseId: String,
    @ColumnInfo(name = "set_no") val setNo: Int,
    /** `NORMAL` / `WARMUP` / `DROP` / `FAILURE` — WARMUP 은 집계 제외 (FN-WRK-013) */
    @ColumnInfo(name = "set_type") val setType: String = "NORMAL",
    /** 항상 kg 로 저장 (REQ-CMN-003) */
    @ColumnInfo(name = "weight_kg") val weightKg: Double? = null,
    @ColumnInfo(name = "reps") val reps: Int? = null,
    @ColumnInfo(name = "duration_sec") val durationSec: Int? = null,
    /** 항상 km 로 저장 */
    @ColumnInfo(name = "distance_km") val distanceKm: Double? = null,
    @ColumnInfo(name = "avg_heart_rate") val avgHeartRate: Int? = null,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double? = null,
    /** 1.0~10.0 (0.5 단위) */
    @ColumnInfo(name = "rpe") val rpe: Double? = null,
    /** 남은 반복 여유. rpe 와 택일 */
    @ColumnInfo(name = "rir") val rir: Int? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    /** 세트 단위 메모 — 폼 이슈 (FN-WRK-026) */
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)
