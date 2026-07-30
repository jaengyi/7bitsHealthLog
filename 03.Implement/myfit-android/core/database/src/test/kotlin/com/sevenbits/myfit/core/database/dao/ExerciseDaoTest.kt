package com.sevenbits.myfit.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.database.MyFitDatabase
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.domain.text.ChosungExtractor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 종목 검색 쿼리 검증. (FN-EXR-008/009 / REQ-EXR-005) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseDaoTest {

    private lateinit var db: MyFitDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyFitDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() = db.close()

    private fun exercise(
        id: String,
        name: String,
        nameEn: String? = null,
        equipment: String = "BARBELL",
        type: String = "WEIGHT",
        active: Boolean = true,
    ) = ExerciseEntity(
        id = id,
        name = name,
        nameEn = nameEn,
        nameChosung = ChosungExtractor.extract(name),
        recordType = "WEIGHT_REPS",
        exerciseType = type,
        equipment = equipment,
        isActive = active,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `한글 부분 일치로 검색된다`() = runTest {
        dao.upsertAll(
            listOf(
                exercise("1", "바벨 벤치프레스"),
                exercise("2", "인클라인 벤치프레스"),
                exercise("3", "랫풀다운"),
            ),
        )

        val result = dao.search("벤치", "벤치")

        assertThat(result.map { it.id }).containsExactly("1", "2")
    }

    @Test
    fun `영문명으로도 검색된다`() = runTest {
        dao.upsertAll(listOf(exercise("1", "바벨 벤치프레스", nameEn = "Barbell Bench Press")))

        val result = dao.search("Bench", "bench")

        assertThat(result.map { it.id }).containsExactly("1")
    }

    @Test
    fun `초성 전방 일치로 검색된다`() = runTest {
        dao.upsertAll(
            listOf(
                exercise("1", "벤치프레스"),   // ㅂㅊㅍㄹㅅ
                exercise("2", "바벨로우"),     // ㅂㅂㄹㅇ
                exercise("3", "랫풀다운"),     // ㄹㅍㄷㅇ
            ),
        )

        val result = dao.search("ㅂㅊ", "ㅂㅊ")

        assertThat(result.map { it.id }).containsExactly("1")
    }

    @Test
    fun `초성 검색은 중간 일치를 허용하지 않는다`() = runTest {
        // ㅊㅍ 는 벤치프레스(ㅂㅊㅍㄹㅅ)의 중간에 있지만 전방 일치가 아니므로 제외된다.
        dao.upsertAll(listOf(exercise("1", "벤치프레스")))

        val result = dao.search("ㅊㅍ", "ㅊㅍ")

        assertThat(result).isEmpty()
    }

    @Test
    fun `전방 일치 결과가 상단에 정렬된다`() = runTest {
        dao.upsertAll(
            listOf(
                exercise("1", "인클라인 벤치프레스"), // 중간 일치
                exercise("2", "벤치프레스"),         // 전방 일치
            ),
        )

        val result = dao.search("벤치", "벤치")

        assertThat(result.first().id).isEqualTo("2")
    }

    @Test
    fun `비활성 종목은 검색에서 제외된다`() = runTest {
        dao.upsertAll(
            listOf(
                exercise("1", "벤치프레스"),
                exercise("2", "벤치프레스 변형", active = false),
            ),
        )

        val result = dao.search("벤치", "벤치")

        assertThat(result.map { it.id }).containsExactly("1")
    }

    @Test
    fun `기록이 있는 종목은 논리 삭제된다`() = runTest {
        dao.upsert(exercise("1", "벤치프레스"))

        dao.deactivate("1", now = 100L)

        val found = dao.findById("1")
        assertThat(found).isNotNull()
        assertThat(found!!.isActive).isFalse()
        assertThat(found.updatedAt).isEqualTo(100L)
        // 논리 삭제도 동기화 대상 변경이다
        assertThat(found.isDirty).isTrue()
    }

    @Test
    fun `검색 결과는 상한으로 제한된다`() = runTest {
        dao.upsertAll((1..80).map { exercise("$it", "벤치프레스 $it") })

        val result = dao.search("벤치", "벤치", limit = 50)

        assertThat(result).hasSize(50)
    }
}
