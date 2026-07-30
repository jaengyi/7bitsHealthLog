package com.sevenbits.myfit.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sevenbits.myfit.core.database.entity.BodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseBodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.ExerciseFavoriteEntity
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

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercise WHERE is_active = 1 ORDER BY name")
    fun observeActive(): Flow<List<ExerciseEntity>>

    /** 검색어 없이 목록만 여는 경우 (필터만 적용된 초기 상태) */
    @Query("SELECT * FROM exercise WHERE is_active = 1 ORDER BY name LIMIT :limit")
    suspend fun searchAll(limit: Int): List<ExerciseEntity>

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

    @Query("DELETE FROM exercise WHERE id = :id AND is_user_defined = 1")
    suspend fun deleteUserExercise(id: String)

    /** 논리/물리 삭제 판단 근거 — 이 종목으로 남긴 기록이 있는가 (FN-EXR-007) */
    @Query("SELECT COUNT(*) FROM workout_log_exercise WHERE exercise_id = :id")
    suspend fun countRecordsOf(id: String): Int

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM exercise WHERE name = :name AND id != :excludeId AND is_active = 1")
    suspend fun countByName(name: String, excludeId: String): Int

    // ── 부위 매핑 ─────────────────────────────────────────

    /**
     * 종목별 부위 매핑 조회.
     *
     * 검색 결과(최대 50건)에 대해 한 번만 조회해 메모리에서 결합한다.
     * 종목마다 개별 조회하면 N+1 이 된다.
     */
    @Query("SELECT * FROM exercise_body_part WHERE exercise_id IN (:exerciseIds)")
    suspend fun findBodyPartMappings(exerciseIds: List<String>): List<ExerciseBodyPartEntity>

    // ── 즐겨찾기 (FN-EXR-010) ────────────────────────────

    @Upsert
    suspend fun upsertFavorite(favorite: ExerciseFavoriteEntity)

    @Query("DELETE FROM exercise_favorite WHERE exercise_id = :exerciseId")
    suspend fun deleteFavorite(exerciseId: String)

    @Query("SELECT exercise_id FROM exercise_favorite")
    suspend fun findFavoriteIds(): List<String>

    @Query("SELECT exercise_id FROM exercise_favorite")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query(
        """
        SELECT e.* FROM exercise e
        JOIN exercise_favorite f ON e.id = f.exercise_id
        WHERE e.is_active = 1
        ORDER BY e.name
        """,
    )
    fun observeFavorites(): Flow<List<ExerciseEntity>>

    // ── 최근 사용 (FN-EXR-011) ───────────────────────────

    /**
     * 최근 수행한 종목. 종목 선택 화면 최상단에 노출한다.
     *
     * 같은 종목이 여러 일지에 있으므로 종목 단위로 묶고 최근 수행일 기준으로 정렬한다.
     */
    @Query(
        """
        SELECT e.* FROM exercise e
        JOIN workout_log_exercise wle ON e.id = wle.exercise_id
        JOIN workout_log wl          ON wle.workout_log_id = wl.id
        WHERE e.is_active = 1
        GROUP BY e.id
        ORDER BY MAX(wl.workout_date) DESC, MAX(wl.session_no) DESC
        LIMIT :limit
        """,
    )
    suspend fun findRecentlyUsed(limit: Int): List<ExerciseEntity>
}
