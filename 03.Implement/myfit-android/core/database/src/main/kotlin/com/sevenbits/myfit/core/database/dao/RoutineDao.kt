package com.sevenbits.myfit.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.ExerciseNoteEntity
import com.sevenbits.myfit.core.database.entity.ExerciseReferenceEntity
import com.sevenbits.myfit.core.database.entity.RoutineEntity
import com.sevenbits.myfit.core.database.entity.RoutineExerciseEntity
import com.sevenbits.myfit.core.database.entity.TombstoneEntity
import kotlinx.coroutines.flow.Flow

internal data class RoutineExerciseWithExercise(
    @Embedded val routineExercise: RoutineExerciseEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
)

internal data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routine_id")
    val exercises: List<RoutineExerciseWithExercise>,
)

@Dao
internal interface RoutineDao {

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertRoutineExercises(items: List<RoutineExerciseEntity>)

    /** 최근 수행일·사용빈도순 정렬 (FN-RTN-002) */
    @Query(
        """
        SELECT * FROM routine
        WHERE is_active = 1 AND (user_id = :userId OR is_preset = 1)
        ORDER BY last_performed_date DESC, use_count DESC, name
        """,
    )
    fun observeRoutines(userId: String): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :routineId")
    suspend fun findRoutineWithExercises(routineId: String): RoutineWithExercises?

    /** 논리 삭제 — 과거 일지의 루틴명 스냅샷은 보존된다 (FN-RTN-004) */
    @Query("UPDATE routine SET is_active = 0, updated_at = :now, is_dirty = 1 WHERE id = :routineId")
    suspend fun deactivateRoutine(routineId: String, now: Long)

    /** 루틴 실행 시 사용 이력 갱신 (FN-RTN-007) */
    @Query(
        """
        UPDATE routine
        SET use_count = use_count + 1, last_performed_date = :date, updated_at = :now, is_dirty = 1
        WHERE id = :routineId
        """,
    )
    suspend fun markPerformed(routineId: String, date: String, now: Long)

    /** 순환 배정의 다음 루틴 제안 (FN-RTN-011) */
    @Query(
        """
        SELECT * FROM routine
        WHERE is_active = 1 AND user_id = :userId
        ORDER BY last_performed_date ASC NULLS FIRST
        LIMIT 1
        """,
    )
    suspend fun findLeastRecentlyPerformed(userId: String): RoutineEntity?
}

@Dao
internal interface ExerciseNoteDao {

    @Upsert
    suspend fun upsert(note: ExerciseNoteEntity)

    /** 수행 화면에서 즉시 열람 — 최신순 (FN-EXR-014) */
    @Query("SELECT * FROM exercise_note WHERE exercise_id = :exerciseId ORDER BY note_date DESC")
    fun observeByExercise(exerciseId: String): Flow<List<ExerciseNoteEntity>>

    @Query("SELECT COUNT(*) FROM exercise_note WHERE exercise_id = :exerciseId")
    suspend fun countByExercise(exerciseId: String): Int

    @Query("DELETE FROM exercise_note WHERE id = :noteId")
    suspend fun delete(noteId: String)
}

@Dao
internal interface ExerciseReferenceDao {

    @Upsert
    suspend fun upsert(reference: ExerciseReferenceEntity)

    /** 기본 제공 + 사용자 등록 링크를 함께 조회 (FN-EXR-017/004) */
    @Query("SELECT * FROM exercise_reference WHERE exercise_id = :exerciseId ORDER BY is_default DESC, order_no")
    fun observeByExercise(exerciseId: String): Flow<List<ExerciseReferenceEntity>>

    /** 기본 제공 자세 영상 1건 — 상세 화면 플레이어 (FN-EXR-017) */
    @Query(
        """
        SELECT * FROM exercise_reference
        WHERE exercise_id = :exerciseId AND is_default = 1 AND ref_type = 'VIDEO'
        ORDER BY order_no LIMIT 1
        """,
    )
    suspend fun findDefaultVideo(exerciseId: String): ExerciseReferenceEntity?

    /** 사용자 등록분만 삭제 가능 — 기본 제공 링크는 보존 */
    @Query("DELETE FROM exercise_reference WHERE id = :referenceId AND is_default = 0")
    suspend fun deleteUserReference(referenceId: String)
}

@Dao
internal interface TombstoneDao {

    @Upsert
    suspend fun upsert(tombstone: TombstoneEntity)

    @Upsert
    suspend fun upsertAll(tombstones: List<TombstoneEntity>)

    /** 동기화 PUSH 델타 추출 (FN-DAT-002) */
    @Query("SELECT * FROM tombstone WHERE is_dirty = 1")
    suspend fun findDirty(): List<TombstoneEntity>

    /** 서버 전파 완료 후 90일 경과분 정리 — 무한 증가 방지 */
    @Query("DELETE FROM tombstone WHERE is_dirty = 0 AND deleted_at < :before")
    suspend fun purgeSyncedBefore(before: Long)
}
