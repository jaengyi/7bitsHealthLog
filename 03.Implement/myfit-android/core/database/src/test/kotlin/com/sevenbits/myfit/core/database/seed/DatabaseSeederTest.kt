package com.sevenbits.myfit.core.database.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.database.MyFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 종목 마스터 초기 적재 검증. (FN-EXR-001/002 / REQ-EXR-001) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseSeederTest {

    private lateinit var db: MyFitDatabase
    private lateinit var seeder: DatabaseSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MyFitDatabase::class.java)
            .allowMainThreadQueries().build()
        seeder = DatabaseSeeder(context, db.exerciseDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `최초 실행 시 종목과 부위를 적재한다`() = runTest {
        val inserted = seeder.seedIfEmpty(nowMillis = 1000L)

        assertThat(inserted).isGreaterThan(0)
        assertThat(db.exerciseDao().count()).isEqualTo(inserted)
        assertThat(db.exerciseDao().observeBodyParts().first()).isNotEmpty()
        assertThat(db.exerciseDao().countBodyPartMappings()).isGreaterThan(0)
    }

    @Test
    fun `이미 적재된 상태에서 다시 호출해도 중복되지 않는다`() = runTest {
        val first = seeder.seedIfEmpty(1000L)
        val second = seeder.seedIfEmpty(2000L)

        assertThat(second).isEqualTo(0)
        assertThat(db.exerciseDao().count()).isEqualTo(first)
    }

    @Test
    fun `적재 시점에 초성이 미리 계산된다`() = runTest {
        seeder.seedIfEmpty(1000L)

        // 런타임 변환이 아니라 저장된 컬럼으로 초성 검색이 동작해야 한다 (REQ-EXR-005)
        val result = db.exerciseDao().search("ㅂㅂ", "ㅂㅂ")

        assertThat(result).isNotEmpty()
        assertThat(result.all { it.nameChosung.startsWith("ㅂㅂ") }).isTrue()
    }

    @Test
    fun `주동근 매핑으로 부위별 종목을 조회할 수 있다`() = runTest {
        seeder.seedIfEmpty(1000L)

        val chest = db.exerciseDao().findByPrimaryBodyPart("CHEST")

        assertThat(chest).isNotEmpty()
        assertThat(chest.map { it.name }).contains("바벨 벤치프레스")
    }

    @Test
    fun `기본 종목은 동기화 대상이 아니다`() = runTest {
        seeder.seedIfEmpty(1000L)

        val all = db.exerciseDao().observeActive().first()

        // 앱 내장 시드이므로 서버로 전송하지 않는다 (05_API설계서 §5.5)
        assertThat(all.none { it.isDirty }).isTrue()
        assertThat(all.none { it.isUserDefined }).isTrue()
    }

    @Test
    fun `기록 유형 5종이 모두 시드에 포함된다`() = runTest {
        seeder.seedIfEmpty(1000L)

        val types = db.exerciseDao().observeActive().first().map { it.recordType }.toSet()

        assertThat(types).containsAtLeast(
            "WEIGHT_REPS", "REPS_ONLY", "TIME", "DISTANCE_TIME", "WEIGHT_TIME",
        )
    }

    @Test
    fun `주요 부위와 장비가 모두 시드에 포함된다`() = runTest {
        seeder.seedIfEmpty(1000L)
        val all = db.exerciseDao().observeActive().first()

        assertThat(all.map { it.equipment }.toSet())
            .containsAtLeast("BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BODYWEIGHT")

        // 부위별로 최소 1종 이상 존재해야 루틴 구성이 가능하다
        listOf("CHEST", "BACK", "SHOULDER", "ARM", "LEG", "ABS", "GLUTE", "CALF").forEach { part ->
            assertThat(db.exerciseDao().findByPrimaryBodyPart(part)).isNotEmpty()
        }
    }

    @Test
    fun `밸런스 분석을 위한 동작 패턴이 부여된다`() = runTest {
        seeder.seedIfEmpty(1000L)
        val patterns = db.exerciseDao().observeActive().first()
            .mapNotNull { it.movementPattern }.toSet()

        // 밀기·당기기 비율과 상하체 비율 분석의 집계 축 (FN-RPT-014/015)
        assertThat(patterns).containsAtLeast("PUSH", "PULL", "SQUAT", "HINGE")
    }

    @Test
    fun `시드 규모가 루틴 구성에 충분하다`() = runTest {
        val count = seeder.seedIfEmpty(1000L)

        // Phase 1 목표는 300종 이상이며 현재는 헬스장 실사용 종목을 선별한 중간 단계다.
        assertThat(count).isAtLeast(200)
    }

    @Test
    fun `모든 종목의 자세 가이드 이미지가 실제로 존재한다`() = runTest {
        seeder.seedIfEmpty(1000L)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val all = db.exerciseDao().observeActive().first()

        // 경로만 있고 파일이 없으면 런타임에 빈 화면이 된다. 시드 단계에서 잡는다. (FN-EXR-016)
        val broken = all.mapNotNull { it.guideImageAsset }.filter { path ->
            runCatching { context.assets.open(path).close() }.isFailure
        }

        assertThat(all.count { it.guideImageAsset != null }).isEqualTo(all.size)
        assertThat(broken).isEmpty()
    }
}
