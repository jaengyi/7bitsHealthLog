package com.sevenbits.myfit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 사용자 계정 및 프로필. (ENT-001 / 04_데이터베이스설계서 §3.1) */
@Entity(tableName = "user")
internal data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    /** `GUEST` / `GOOGLE` / `KAKAO` / `EMAIL` — 로그인 없이도 사용 가능 (REQ-CMN-001) */
    @ColumnInfo(name = "login_type") val loginType: String = "GUEST",
    @ColumnInfo(name = "server_user_id") val serverUserId: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    /** `MALE` / `FEMALE` / `UNSPECIFIED` */
    @ColumnInfo(name = "gender") val gender: String? = null,
    /** `yyyy-MM-dd`. 만 나이 산출 기준 (FN-CMN-007) */
    @ColumnInfo(name = "birth_date") val birthDate: String? = null,
    /** 100~250 검증. BMI 산출 기준 (FN-BDY-002) */
    @ColumnInfo(name = "height_cm") val heightCm: Double? = null,
    @ColumnInfo(name = "career_level") val careerLevel: String? = null,
    @ColumnInfo(name = "goal_type") val goalType: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/** 사용자 환경설정. 기기 이전 시 승계되어야 하므로 DataStore 가 아닌 테이블로 둔다. (ENT-002 / §3.2) */
@Entity(
    tableName = "user_setting",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class UserSettingEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "weight_unit") val weightUnit: String = "KG",
    @ColumnInfo(name = "length_unit") val lengthUnit: String = "CM",
    @ColumnInfo(name = "distance_unit") val distanceUnit: String = "KM",
    /** 헬스장 조명 환경을 고려해 기본값 다크 (REQ-CMN-005) */
    @ColumnInfo(name = "theme_mode") val themeMode: String = "DARK",
    @ColumnInfo(name = "language") val language: String = "ko",
    @ColumnInfo(name = "one_rm_formula") val oneRmFormula: String = "EPLEY",
    @ColumnInfo(name = "default_rest_sec") val defaultRestSec: Int = 90,
    @ColumnInfo(name = "timer_sound_enabled") val timerSoundEnabled: Boolean = true,
    @ColumnInfo(name = "timer_vibrate_enabled") val timerVibrateEnabled: Boolean = true,
    @ColumnInfo(name = "default_bar_weight_kg") val defaultBarWeightKg: Double = 20.0,
    /** 스트릭 판정 기준 (FN-CAL-008) */
    @ColumnInfo(name = "weekly_goal_count") val weeklyGoalCount: Int = 3,
    /** ± 버튼 증감 단위 (FN-WRK-010) */
    @ColumnInfo(name = "weight_step_kg") val weightStepKg: Double = 2.5,
    @ColumnInfo(name = "auto_backup_enabled") val autoBackupEnabled: Boolean = true,
    @ColumnInfo(name = "onboarding_completed") val onboardingCompleted: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/** 근육 부위 코드 마스터. 앱 내장 고정 데이터 — 동기화 대상 아님. (ENT-004 / §3.4) */
@Entity(tableName = "body_part")
internal data class BodyPartEntity(
    @PrimaryKey @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    /** `UPPER` / `LOWER` / `CORE` / `FULL` — 상하체 비율 분석 기준 (FN-RPT-015) */
    @ColumnInfo(name = "upper_group") val upperGroup: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

/** 종목-부위 매핑. 부위별 세트 비중 집계의 조인 축. (ENT-005 / §3.5) */
@Entity(
    tableName = "exercise_body_part",
    primaryKeys = ["exercise_id", "body_part_code", "role"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("body_part_code")],
)
internal data class ExerciseBodyPartEntity(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "body_part_code") val bodyPartCode: String,
    /** `PRIMARY`(주동근) / `SECONDARY`(보조근) */
    @ColumnInfo(name = "role") val role: String,
)

/** 종목 즐겨찾기. 동기화 대상이므로 복합키 대신 단일 PK 를 부여한다. (ENT-006 / §3.6) */
@Entity(
    tableName = "exercise_favorite",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["user_id", "exercise_id"], unique = true), Index("exercise_id")],
)
internal data class ExerciseFavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/** PT 학습 노트. 종목별로 누적하고 수행 화면에서 즉시 열람한다. (ENT-007 / REQ-EXR-008) */
@Entity(
    tableName = "exercise_note",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exercise_id"), Index("note_date")],
)
internal data class ExerciseNoteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "trainer_name") val trainerName: String? = null,
    @ColumnInfo(name = "note_date") val noteDate: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

/**
 * 종목 참고 자료 — 자세 영상·참고 링크. (v1.1 신설 / REQ-EXR-003, FN-EXR-004/017)
 *
 * 기본 제공(`is_default=1`, 시드)과 사용자 등록을 한 테이블로 통합한다.
 * **영상 파일은 저장하지 않고 URL 만 보관한다.** (REQ-NFR-009)
 */
@Entity(
    tableName = "exercise_reference",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exercise_id", "order_no"])],
)
internal data class ExerciseReferenceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    /** 사용자 등록분만 값 보유. 기본 제공은 NULL */
    @ColumnInfo(name = "user_id") val userId: String? = null,
    /** `VIDEO` / `ARTICLE` */
    @ColumnInfo(name = "ref_type") val refType: String = "VIDEO",
    /** `YOUTUBE` / `WEB` — 재생 방식 분기 */
    @ColumnInfo(name = "provider") val provider: String = "YOUTUBE",
    @ColumnInfo(name = "url") val url: String,
    /** 임베드 플레이어 입력값 */
    @ColumnInfo(name = "video_id") val videoId: String? = null,
    /** 긴 영상에서 해당 구간만 재생 */
    @ColumnInfo(name = "start_sec") val startSec: Int? = null,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "order_no") val orderNo: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)
