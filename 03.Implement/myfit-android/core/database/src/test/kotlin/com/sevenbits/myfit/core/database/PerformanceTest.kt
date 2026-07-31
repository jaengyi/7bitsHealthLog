package com.sevenbits.myfit.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.database.dao.WorkoutDao
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogEntity
import com.sevenbits.myfit.core.database.entity.WorkoutLogExerciseEntity
import com.sevenbits.myfit.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 누적 3년치 데이터에서의 쿼리 성능. (REQ-NFR-001)
 *
 * 요구사항 기준:
 * - 운동일지 화면 진입 1초 이내
 * - 세트 입력 후 반영 100ms 이내
 * - 종목 검색 결과 300ms 이내
 *
 * **여기서 재는 것은 DB 왕복 시간뿐이다.** 화면 전체 응답에는 Compose 렌더링이
 * 더해지므로, 이 수치는 예산의 일부를 차지할 뿐이다. 그래서 임계값을 요구사항의
 * 절반 이하로 잡았다 — DB 가 예산 절반을 먹기 시작하면 이미 위험 신호다.
 *
 * 인메모리 DB 라 디스크 I/O 가 빠지지만, 인덱스 사용 여부와 쿼리 계획은 동일하다.
 * 이 테스트가 잡으려는 것은 "행 수에 비례해 느려지는 쿼리"이지 절대 시간이 아니다.
 *
 * 데이터 규모: 500 세션 × 6 종목 × 10 세트 = **30,000 세트**
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceTest {

    /**
     * 30,000 세트 적재는 클래스당 한 번만 한다. 테스트마다 다시 만들면 수 분이 걸린다.
     *
     * `@BeforeClass` 를 쓸 수 없다 — Robolectric 은 테스트 실행 시점에야 안드로이드
     * 환경을 세우므로 클래스 초기화 시점에는 Context 가 없다.
     */
    @Before
    fun seedOnce() = runTest {
        if (isSeeded) return@runTest
        seedLargeDataset()
        isSeeded = true
    }

    @Test
    fun `일지 상세 조회가 1초 예산의 절반 안에 끝난다`() = runTest {
        // 가장 최근 일지 — 화면 진입 시 조회 대상
        val elapsed = measure(REPEAT) {
            val detail = dao.observeLogDetail(lastLogId).first()
            assertThat(detail!!.exercises).hasSize(EXERCISES_PER_LOG)
        }
        report("일지 상세 조회", elapsed, budget = 500)
    }

    @Test
    fun `세션 집계가 100ms 안에 끝난다`() = runTest {
        val elapsed = measure(REPEAT) {
            val agg = dao.aggregateSession(lastLogId)
            assertThat(agg.totalSetCount).isEqualTo(EXERCISES_PER_LOG * SETS_PER_EXERCISE)
        }
        report("세션 집계", elapsed, budget = 100)
    }

    @Test
    fun `세트 단건 갱신이 50ms 안에 끝난다`() = runTest {
        val target = dao.findSetsOf("${lastLogId}-ex0").first()
        val elapsed = measure(REPEAT) {
            dao.upsertSet(target.copy(reps = (target.reps ?: 0) + 1, updatedAt = 1L))
        }
        // 입력 반영 100ms 안에 낙관적 UI 갱신과 DB 쓰기가 모두 들어가야 한다
        report("세트 단건 갱신", elapsed, budget = 50)
    }

    @Test
    fun `월간 캘린더 요약이 300ms 안에 끝난다`() = runTest {
        val elapsed = measure(REPEAT) {
            val rows = dao.observeDailySummaries(USER_ID, "2026-01-01", "2026-01-31").first()
            assertThat(rows).isNotEmpty()
        }
        report("월간 캘린더 요약", elapsed, budget = 300)
    }

    @Test
    fun `종목 검색이 300ms 안에 끝난다`() = runTest {
        val elapsed = measure(REPEAT) {
            val found = db.exerciseDao().search("종목", "ㅈㅁ", limit = 50)
            assertThat(found).isNotEmpty()
        }
        report("종목 검색", elapsed, budget = 300)
    }

    @Test
    fun `직전 수행 세트 조회가 300ms 안에 끝난다`() = runTest {
        // 종목 추가 시 프리필에 쓰인다 (FN-WRK-014). 3년치 전체를 훑는 쿼리라
        // 인덱스가 빠지면 가장 먼저 느려진다.
        val elapsed = measure(REPEAT) {
            val sets = dao.findLastPerformedSets(USER_ID, "ex-0", "none")
            assertThat(sets).isNotEmpty()
        }
        report("직전 수행 세트 조회", elapsed, budget = 300)
    }

    @Test
    fun `데이터 규모가 3년치 기준을 충족한다`() = runTest {
        assertThat(totalSets).isEqualTo(LOG_COUNT * EXERCISES_PER_LOG * SETS_PER_EXERCISE)
        assertThat(totalSets).isAtLeast(30_000)
    }

    /** 워밍업 후 중앙값을 쓴다. 첫 실행에는 JIT·쿼리 계획 수립 비용이 섞인다. */
    private inline fun measure(repeat: Int, block: () -> Unit): Long {
        block() // 워밍업
        val samples = LongArray(repeat) {
            val start = System.nanoTime()
            block()
            (System.nanoTime() - start) / 1_000_000
        }
        samples.sort()
        return samples[repeat / 2]
    }

    private fun report(label: String, elapsedMs: Long, budget: Long) {
        println("[REQ-NFR-001] $label: ${elapsedMs}ms (예산 ${budget}ms)")
        assertThat(elapsedMs).isAtMost(budget)
    }

    companion object {
        private const val USER_ID = "perf-user"
        private const val LOG_COUNT = 500
        private const val EXERCISES_PER_LOG = 6
        private const val SETS_PER_EXERCISE = 10
        private const val EXERCISE_KINDS = 30
        private const val REPEAT = 5

        private lateinit var db: MyFitDatabase
        private lateinit var dao: WorkoutDao
        private lateinit var lastLogId: String
        private var totalSets = 0
        private var isSeeded = false

        private suspend fun seedLargeDataset() {
            db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MyFitDatabase::class.java,
            ).allowMainThreadQueries().build()
            dao = db.workoutDao()

            db.userDao().upsert(UserEntity(id = USER_ID, createdAt = 0L, updatedAt = 0L))
            repeat(EXERCISE_KINDS) { i ->
                db.exerciseDao().upsert(
                    ExerciseEntity(
                        id = "ex-$i",
                        name = "종목$i",
                        nameChosung = "ㅈㅁ",
                        recordType = "WEIGHT_REPS",
                        exerciseType = "WEIGHT",
                        equipment = "BARBELL",
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                )
            }

            // 3년 전부터 오늘까지 이틀에 한 번꼴로 수행한 이력
            val start = LocalDate.of(2026, 7, 30).minusDays(LOG_COUNT * 2L)
            val logs = ArrayList<WorkoutLogEntity>(LOG_COUNT)
            val logExercises = ArrayList<WorkoutLogExerciseEntity>(LOG_COUNT * EXERCISES_PER_LOG)
            val sets = ArrayList<WorkoutSetEntity>(LOG_COUNT * EXERCISES_PER_LOG * SETS_PER_EXERCISE)

            repeat(LOG_COUNT) { logIndex ->
                val logId = "log-$logIndex"
                logs += WorkoutLogEntity(
                    id = logId,
                    userId = USER_ID,
                    workoutDate = start.plusDays(logIndex * 2L).toString(),
                    sessionNo = 1,
                    status = "COMPLETED",
                    createdAt = 0L,
                    updatedAt = 0L,
                )
                repeat(EXERCISES_PER_LOG) { exIndex ->
                    val logExerciseId = "$logId-ex$exIndex"
                    logExercises += WorkoutLogExerciseEntity(
                        id = logExerciseId,
                        workoutLogId = logId,
                        exerciseId = "ex-${(logIndex * EXERCISES_PER_LOG + exIndex) % EXERCISE_KINDS}",
                        orderNo = exIndex,
                        createdAt = 0L,
                        updatedAt = 0L,
                    )
                    repeat(SETS_PER_EXERCISE) { setIndex ->
                        sets += WorkoutSetEntity(
                            id = "$logExerciseId-s$setIndex",
                            logExerciseId = logExerciseId,
                            setNo = setIndex + 1,
                            setType = if (setIndex == 0) "WARMUP" else "NORMAL",
                            weightKg = 40.0 + setIndex * 5,
                            reps = 12 - setIndex,
                            isCompleted = true,
                            createdAt = 0L,
                            updatedAt = 0L,
                        )
                    }
                }
            }

            logs.forEach { dao.upsertLog(it) }
            logExercises.chunked(500).forEach { dao.upsertLogExercises(it) }
            sets.chunked(1000).forEach { dao.upsertSets(it) }

            lastLogId = "log-${LOG_COUNT - 1}"
            totalSets = sets.size
            println("[REQ-NFR-001] 적재 완료 — 일지 ${logs.size}건 / 세트 ${sets.size}건")
        }
    }
}
