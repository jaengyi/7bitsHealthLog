package com.sevenbits.myfit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.entity.ExerciseEntity

/**
 * 로컬 단일 진실 공급원(SSoT). (설계 원칙 P1 / REQ-DAT-001)
 *
 * **주의**: `fallbackToDestructiveMigration` 은 절대 사용하지 않는다.
 * 기록 유실은 본 앱에서 치명적 결함이다. (04_데이터베이스설계서 §5)
 *
 * 스키마 JSON(`core/database/schemas`)은 VCS 에 커밋하며 마이그레이션 테스트의 입력이 된다.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        // Phase 1 잔여 엔티티는 후속 커밋에서 추가한다.
        // body_part, exercise_body_part, exercise_favorite, exercise_note, exercise_reference,
        // workout_log, workout_log_exercise, workout_set, routine, routine_exercise,
        // user, user_setting
    ],
    version = MyFitDatabase.VERSION,
    exportSchema = true,
)
internal abstract class MyFitDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val VERSION = 1
        const val NAME = "myfit.db"
    }
}
