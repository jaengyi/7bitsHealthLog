package com.sevenbits.myfit.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.database.MyFitDatabase
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 운동기록 3계층 쿼리 검증. (FN-WRK-001/013/014/016/027) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutDaoTest {

    private lateinit var db: MyFitDatabase
    private lateinit var dao: WorkoutDao

    private val userId = "u1"
    private val exerciseId = "e1"
    private val logId = "log1"
    private val logExerciseId = "le1"

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyFitDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.workoutDao()

        db.exerciseDao().upsert(
            ExerciseEntity(
                id = exerciseId, name = "벤치프레스", nameChosung = "ㅂㅊㅍㄹㅅ",
                recordType = "WEIGHT_REPS", exerciseType = "WEIGHT", equipment = "BARBELL",
                createdAt = 0L, updatedAt = 0L,
            ),
        )
        db.userDao().upsert(UserEntity(id = userId, createdAt = 0L, updatedAt = 0L))
    }

    @After
    fun tearDown() = db.close()

    private fun log(id: String = logId, date: String = "2026-07-30", sessionNo: Int = 1) =
        WorkoutLogEntity(
            id = id, userId = userId, workoutDate = date, sessionNo = sessionNo,
            status = "COMPLETED", createdAt = 0L, updatedAt = 0L,
        )

    private fun set(
        id: String,
        setNo: Int,
        weight: Double?,
        reps: Int?,
        type: String = "NORMAL",
    ) = WorkoutSetEntity(
        id = id, logExerciseId = logExerciseId, setNo = setNo,
        setType = type, weightKg = weight, reps = reps,
        createdAt = 0L, updatedAt = 0L,
    )

    private suspend fun givenLogWithExercise() {
        dao.upsertLog(log())
        dao.upsertLogExercise(
            WorkoutLogExerciseEntity(
                id = logExerciseId, workoutLogId = logId, exerciseId = exerciseId,
                orderNo = 1, createdAt = 0L, updatedAt = 0L,
            ),
        )
    }

    @Test
    fun `일지 상세를 단일 조회로 가져온다`() = runTest {
        givenLogWithExercise()
        dao.upsertSets(listOf(set("s1", 1, 60.0, 10), set("s2", 2, 80.0, 8)))

        val detail = dao.observeLogDetail(logId).first()

        assertThat(detail).isNotNull()
        assertThat(detail!!.exercises).hasSize(1)
        assertThat(detail.exercises.first().exercise.name).isEqualTo("벤치프레스")
        assertThat(detail.exercises.first().sets).hasSize(2)
    }

    @Test
    fun `세션 집계에서 웜업 세트는 볼륨과 횟수에서 제외된다`() = runTest {
        givenLogWithExercise()
        dao.upsertSets(
            listOf(
                set("s1", 1, 40.0, 12, type = "WARMUP"), // 제외
                set("s2", 2, 60.0, 10),                  // 600
                set("s3", 3, 80.0, 8),                   // 640
            ),
        )

        val agg = dao.aggregateSession(logId)

        assertThat(agg.totalVolumeKg).isWithin(0.001).of(1240.0)
        assertThat(agg.totalReps).isEqualTo(18)
        // 세트 수는 웜업을 포함한 전체다
        assertThat(agg.totalSetCount).isEqualTo(3)
    }

    @Test
    fun `드랍세트와 실패세트는 볼륨에 포함된다`() = runTest {
        givenLogWithExercise()
        dao.upsertSets(
            listOf(
                set("s1", 1, 60.0, 5, type = "DROP"),    // 300
                set("s2", 2, 40.0, 3, type = "FAILURE"), // 120
            ),
        )

        assertThat(dao.aggregateSession(logId).totalVolumeKg).isWithin(0.001).of(420.0)
    }

    @Test
    fun `같은 일자 같은 회차는 중복 생성되지 않는다`() = runTest {
        dao.upsertLog(log(id = "a", sessionNo = 1))
        // 동일 (user, date, session_no) 로 다른 id 를 넣으면 UNIQUE 제약에 걸린다
        runCatching { dao.upsertLog(log(id = "b", sessionNo = 1)) }

        assertThat(dao.countSessionsOn(userId, "2026-07-30")).isEqualTo(1)
    }

    @Test
    fun `회차를 나누면 같은 일자에 복수 일지가 생성된다`() = runTest {
        dao.upsertLog(log(id = "a", sessionNo = 1))
        dao.upsertLog(log(id = "b", sessionNo = 2))

        assertThat(dao.countSessionsOn(userId, "2026-07-30")).isEqualTo(2)
    }

    @Test
    fun `진행 중 세션을 복원용으로 조회한다`() = runTest {
        dao.upsertLog(log(id = "done").copy(status = "COMPLETED"))
        dao.upsertLog(log(id = "doing", date = "2026-07-31").copy(status = "IN_PROGRESS"))

        assertThat(dao.findInProgressLog(userId)?.id).isEqualTo("doing")
    }

    @Test
    fun `세트 삭제 후 순번이 재정렬된다`() = runTest {
        givenLogWithExercise()
        dao.upsertSets(
            listOf(set("s1", 1, 60.0, 10), set("s2", 2, 70.0, 9), set("s3", 3, 80.0, 8)),
        )

        dao.deleteSet(set("s2", 2, 70.0, 9))
        dao.shiftSetNumbersAfter(logExerciseId, deletedSetNo = 2, now = 100L)

        val sets = dao.observeSets(logExerciseId).first()
        assertThat(sets.map { it.setNo }).containsExactly(1, 2).inOrder()
        assertThat(sets.map { it.id }).containsExactly("s1", "s3").inOrder()
    }

    @Test
    fun `일지 삭제 시 하위 종목과 세트가 연쇄 삭제된다`() = runTest {
        givenLogWithExercise()
        dao.upsertSets(listOf(set("s1", 1, 60.0, 10)))

        dao.deleteLog(log())

        assertThat(dao.observeSets(logExerciseId).first()).isEmpty()
        assertThat(dao.observeLogDetail(logId).first()).isNull()
    }

    @Test
    fun `직전 수행 기록을 프리필용으로 조회한다`() = runTest {
        // 과거 완료 일지
        givenLogWithExercise()
        dao.upsertSets(listOf(set("s1", 1, 80.0, 8), set("s2", 2, 80.0, 8)))

        // 오늘 작성 중인 일지 — 자기 자신은 제외되어야 한다
        val todayLogId = "log2"
        dao.upsertLog(log(id = todayLogId, date = "2026-07-31").copy(status = "IN_PROGRESS"))

        val previous = dao.findLastPerformedSets(userId, exerciseId, excludeLogId = todayLogId)

        assertThat(previous).hasSize(2)
        assertThat(previous.first().weightKg).isWithin(0.001).of(80.0)
    }

    @Test
    fun `캘린더 마커용 일자별 집계를 조회한다`() = runTest {
        dao.upsertLog(log(id = "a", date = "2026-07-01").copy(totalVolumeKg = 1000.0))
        dao.upsertLog(log(id = "b", date = "2026-07-15").copy(totalVolumeKg = 2000.0))
        dao.upsertLog(log(id = "c", date = "2026-08-01").copy(totalVolumeKg = 3000.0))

        val summaries = dao.observeDailySummaries(userId, "2026-07-01", "2026-07-31").first()

        assertThat(summaries.map { it.workoutDate }).containsExactly("2026-07-01", "2026-07-15")
    }
}
