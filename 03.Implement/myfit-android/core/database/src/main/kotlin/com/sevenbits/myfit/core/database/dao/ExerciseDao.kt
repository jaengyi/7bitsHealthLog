package com.sevenbits.myfit.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sevenbits.myfit.core.database.entity.BodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseBodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExerciseDao {

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    // ── 시드 적재 (FN-EXR-001/002) ────────────────────────

    @Upsert
    suspend fun upsertBodyParts(bodyParts: List<BodyPartEntity>)

    @Upsert
    suspend fun upsertExerciseBodyParts(mappings: List<ExerciseBodyPartEntity>)

    @Query("SELECT * FROM body_part ORDER BY sort_order")
    fun observeBodyParts(): Flow<List<BodyPartEntity>>

    @Query("SELECT COUNT(*) FROM exercise_body_part")
    suspend fun countBodyPartMappings(): Int

    /** 특정 부위를 주동근으로 하는 종목 (필터·리포트 조인) */
    @Query(
        """
        SELECT e.* FROM exercise e
        JOIN exercise_body_part ebp ON e.id = ebp.exercise_id
        WHERE ebp.body_part_code = :bodyPartCode AND ebp.role = 'PRIMARY' AND e.is_active = 1
        ORDER BY e.name
        """,
    )
    suspend fun findByPrimaryBodyPart(bodyPartCode: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun findById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE is_active = 1 ORDER BY name")
    fun observeActive(): Flow<List<ExerciseEntity>>

    /**
     * 종목 검색. 한글·영문·초성을 함께 조회한다. (FN-EXR-008)
     *
     * 초성은 **전방 일치**만 허용한다 — 중간 일치까지 허용하면 노이즈가 급증한다.
     * 전방 일치 결과를 상단으로 정렬하고 50건으로 제한하여 300ms 기준을 충족한다.
     */
    @Query(
        """
        SELECT * FROM exercise
        WHERE is_active = 1
          AND ( name LIKE '%' || :query || '%'
             OR name_en LIKE '%' || :query || '%'
             OR name_chosung LIKE :chosungQuery || '%' )
        ORDER BY
          CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
          name
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, chosungQuery: String, limit: Int = 50): List<ExerciseEntity>

    @Query("UPDATE exercise SET is_active = 0, updated_at = :now, is_dirty = 1 WHERE id = :id")
    suspend fun deactivate(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int
}
