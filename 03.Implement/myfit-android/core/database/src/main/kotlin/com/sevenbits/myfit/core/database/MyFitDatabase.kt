package com.sevenbits.myfit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.dao.ExerciseNoteDao
import com.sevenbits.myfit.core.database.dao.ExerciseReferenceDao
import com.sevenbits.myfit.core.database.dao.RoutineDao
import com.sevenbits.myfit.core.database.dao.TombstoneDao
import com.sevenbits.myfit.core.database.dao.WorkoutDao
import com.sevenbits.myfit.core.database.entity.BodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseBodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.ExerciseFavoriteEntity
import com.sevenbits.myfit.core.database.entity.ExerciseNoteEntity
import com.sevenbits.myfit.core.database.entity.ExerciseReferenceEntity
import com.sevenbits.myfit.core.database.entity.RoutineEntity
import com.sevenbits.myfit.core.database.entity.RoutineExerciseEntity
import com.sevenbits.myfit.core.database.entity.TombstoneEntity
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.UserSettingEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity

/**
 * 로컬 단일 진실 공급원(SSoT). (설계 원칙 P1 / REQ-DAT-001)
 *
 * **주의**: `fallbackToDestructiveMigration` 은 절대 사용하지 않는다.
 * 기록 유실은 본 앱에서 치명적 결함이다. (04_데이터베이스설계서 §5)
 *
 * 스키마 JSON(`core/database/schemas`)은 VCS 에 커밋하며 마이그레이션 테스트의 입력이 된다.
 *
 * **Phase 1 대상 13개 테이블.** Phase 2/3 테이블(personal_record, body_*, diet_*,
 * program, periodization_rule, note_fts 등)은 해당 Phase 착수 시 마이그레이션과 함께 추가한다.
 */
@Database(
    entities = [
        // 사용자
        UserEntity::class,
        UserSettingEntity::class,
        // 종목
        ExerciseEntity::class,
        BodyPartEntity::class,
        ExerciseBodyPartEntity::class,
        ExerciseFavoriteEntity::class,
        ExerciseNoteEntity::class,
        ExerciseReferenceEntity::class,
        // 운동기록 3계층
        WorkoutLogEntity::class,
        WorkoutLogExerciseEntity::class,
        WorkoutSetEntity::class,
        // 루틴
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        // 동기화 메타
        TombstoneEntity::class,
    ],
    version = MyFitDatabase.VERSION,
    exportSchema = true,
)
internal abstract class MyFitDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseNoteDao(): ExerciseNoteDao
    abstract fun exerciseReferenceDao(): ExerciseReferenceDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun routineDao(): RoutineDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        const val VERSION = 1
        const val NAME = "myfit.db"
    }
}
