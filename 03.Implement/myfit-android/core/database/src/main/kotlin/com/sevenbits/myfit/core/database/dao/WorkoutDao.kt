package com.sevenbits.myfit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

/** 일지 종목 + 종목 마스터 + 세트 목록 */
internal data class LogExerciseWithSets(
    @Embedded val logExercise: WorkoutLogExerciseEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "log_exercise_id")
    val sets: List<WorkoutSetEntity>,
)

/** 일지 헤더 + 하위 전체 */
internal data class WorkoutLogWithDetail(
    @Embedded val log: WorkoutLogEntity,
    @Relation(entity = WorkoutLogExerciseEntity::class, parentColumn = "id", entityColumn = "workout_log_id")
    val exercises: List<LogExerciseWithSets>,
)

@Dao
internal interface WorkoutDao {

    // ── 일지 ──────────────────────────────────────────────

    @Upsert
    suspend fun upsertLog(log: WorkoutLogEntity)

    @Upsert
    suspend fun upsertLogExercise(logExercise: WorkoutLogExerciseEntity)

    @Upsert
    suspend fun upsertLogExercises(logExercises: List<WorkoutLogExerciseEntity>)

    @Delete
    suspend fun deleteLog(log: WorkoutLogEntity)

    /**
     * 일지 상세 조회. **단일 트랜잭션 1회 조회**로 일지-종목-세트를 모두 가져온다.
     * 화면 진입 1초 이내 요구(REQ-NFR-001)의 전제다.
     */
    @Transaction
    @Query("SELECT * FROM workout_log WHERE id = :logId")
    fun observeLogDetail(logId: String): Flow<WorkoutLogWithDetail?>

    @Transaction
    @Query(
        """
        SELECT * FROM workout_log
        WHERE user_id = :userId AND workout_date = :date AND session_no = :sessionNo
        """,
    )
    suspend fun findLog(userId: String, date: String, sessionNo: Int): WorkoutLogEntity?

    @Query("SELECT * FROM workout_log WHERE id = :logId")
    suspend fun findLogById(logId: String): WorkoutLogEntity?

    /** 진행 중 세션 복원 (FN-WRK-023) */
    @Query("SELECT * FROM workout_log WHERE user_id = :userId AND status = 'IN_PROGRESS' LIMIT 1")
    suspend fun findInProgressLog(userId: String): WorkoutLogEntity?

    @Delete
    suspend fun deleteLogExercise(logExercise: WorkoutLogExerciseEntity)

    @Query("SELECT * FROM workout_log_exercise WHERE id = :id")
    suspend fun findLogExerciseById(id: String): WorkoutLogExerciseEntity?

    @Query("SELECT COALESCE(MAX(order_no), 0) FROM workout_log_exercise WHERE workout_log_id = :logId")
    suspend fun maxOrderNo(logId: String): Int

    /** 드래그 정렬 (FN-WRK-018) */
    @Query(
        """
        UPDATE workout_log_exercise
        SET order_no = :orderNo, updated_at = :now, is_dirty = 1
        WHERE id = :id
        """,
    )
    suspend fun updateExerciseOrder(id: String, orderNo: Int, now: Long)

    /** 같은 일자의 회차 목록 — 최대 3회차 제한 검증에 사용 (FN-WRK-005) */
    @Query("SELECT COUNT(*) FROM workout_log WHERE user_id = :userId AND workout_date = :date")
    suspend fun countSessionsOn(userId: String, date: String): Int

    // ── 세트 ──────────────────────────────────────────────

    @Upsert
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Upsert
    suspend fun upsertSets(sets: List<WorkoutSetEntity>)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_set WHERE log_exercise_id = :logExerciseId ORDER BY set_no")
    fun observeSets(logExerciseId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_set WHERE log_exercise_id = :logExerciseId ORDER BY set_no")
    suspend fun findSetsOf(logExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_set WHERE id = :setId")
    suspend fun findSetById(setId: String): WorkoutSetEntity?

    /** 진행률 표시 (FN-WRK-021) */
    @Query(
        """
        SELECT COUNT(*) FROM workout_set ws
        JOIN workout_log_exercise wle ON ws.log_exercise_id = wle.id
        WHERE wle.workout_log_id = :logId AND ws.is_completed = 1
        """,
    )
    suspend fun countCompletedSets(logId: String): Int

    /** 웜업을 제외한 실작업 세트 수 */
    @Query(
        """
        SELECT COUNT(*) FROM workout_set ws
        JOIN workout_log_exercise wle ON ws.log_exercise_id = wle.id
        WHERE wle.workout_log_id = :logId AND ws.set_type != 'WARMUP'
        """,
    )
    suspend fun countWorkingSets(logId: String): Int

    /** 세트 삭제 후 순번 재정렬 (FN-WRK-016) */
    @Query(
        """
        UPDATE workout_set SET set_no = set_no - 1, updated_at = :now, is_dirty = 1
        WHERE log_exercise_id = :logExerciseId AND set_no > :deletedSetNo
        """,
    )
    suspend fun shiftSetNumbersAfter(logExerciseId: String, deletedSetNo: Int, now: Long)

    /**
     * 직전 수행 기록 조회 — 프리필 우선순위 2단계. (FN-WRK-014)
     *
     * 해당 종목을 마지막으로 수행한 일지의 세트 구성을 그대로 가져온다.
     */
    @Query(
        """
        SELECT ws.* FROM workout_set ws
        JOIN workout_log_exercise wle ON ws.log_exercise_id = wle.id
        JOIN workout_log wl          ON wle.workout_log_id = wl.id
        WHERE wle.exercise_id = :exerciseId
          AND wl.user_id = :userId
          AND wl.status = 'COMPLETED'
          AND wl.id != :excludeLogId
        ORDER BY wl.workout_date DESC, wl.session_no DESC, ws.set_no ASC
        LIMIT 20
        """,
    )
    suspend fun findLastPerformedSets(
        userId: String,
        exerciseId: String,
        excludeLogId: String,
    ): List<WorkoutSetEntity>

    // ── 집계 (SQL 에서 수행 — 메모리 로딩 금지) ─────────────

    /**
     * 세션 요약 집계. 웜업 세트를 제외한다. (FN-WRK-027)
     *
     * 리포트·요약 집계는 전부 SQL GROUP BY 로 수행한다. 세트 전체를 앱 메모리로
     * 로딩해 합산하는 구현을 금지한다. (REQ-NFR-001)
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN ws.set_type != 'WARMUP'
                THEN ws.weight_kg * ws.reps ELSE 0 END), 0) AS totalVolumeKg,
            COUNT(ws.id) AS totalSetCount,
            COALESCE(SUM(CASE WHEN ws.set_type != 'WARMUP' THEN ws.reps ELSE 0 END), 0) AS totalReps
        FROM workout_set ws
        JOIN workout_log_exercise wle ON ws.log_exercise_id = wle.id
        WHERE wle.workout_log_id = :logId
        """,
    )
    suspend fun aggregateSession(logId: String): SessionAggregate

    /** 월간 캘린더 마커 (FN-CAL-001) */
    @Query(
        """
        SELECT workout_date AS workoutDate,
               COUNT(*) AS logCount,
               SUM(total_volume_kg) AS totalVolumeKg
        FROM workout_log
        WHERE user_id = :userId
          AND workout_date BETWEEN :from AND :to
          AND status = 'COMPLETED'
        GROUP BY workout_date
        """,
    )
    fun observeDailySummaries(userId: String, from: String, to: String): Flow<List<DailySummaryRow>>

    /** 스트릭 산출 입력 — 수행 완료 일자 목록 (FN-CAL-008) */
    @Query(
        """
        SELECT DISTINCT workout_date FROM workout_log
        WHERE user_id = :userId AND status = 'COMPLETED' AND workout_date >= :from
        ORDER BY workout_date DESC
        """,
    )
    suspend fun findCompletedDatesSince(userId: String, from: String): List<String>
}

internal data class SessionAggregate(
    val totalVolumeKg: Double,
    val totalSetCount: Int,
    val totalReps: Int,
)

internal data class DailySummaryRow(
    val workoutDate: String,
    val logCount: Int,
    val totalVolumeKg: Double?,
)
