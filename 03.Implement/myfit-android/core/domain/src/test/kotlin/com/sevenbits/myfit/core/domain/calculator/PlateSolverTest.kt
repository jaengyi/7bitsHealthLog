package com.sevenbits.myfit.core.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.domain.calculator.PlateSolver.Plate
import org.junit.Test

/** 07_핵심로직설계서 §6.3 검증 케이스 */
class PlateSolverTest {

    private val standard = listOf(
        Plate(20.0, 2), Plate(15.0, 2), Plate(10.0, 2),
        Plate(5.0, 2), Plate(2.5, 2), Plate(1.25, 2),
    )

    @Test
    fun `목표 100kg 바 20kg - 한쪽 20kg 2장`() {
        val results = PlateSolver.solve(targetKg = 100.0, barKg = 20.0, inventory = standard)

        assertThat(results).hasSize(1)
        val r = results.first()
        assertThat(r.isExact).isTrue()
        assertThat(r.actualTotalKg).isWithin(0.001).of(100.0)
        assertThat(r.plates).containsExactly(PlateSolver.PlateCount(20.0, 2))
    }

    @Test
    fun `목표 102_5kg - 20kg 2장 더하기 1_25kg 1장`() {
        val results = PlateSolver.solve(102.5, 20.0, standard)

        assertThat(results).hasSize(1)
        assertThat(results.first().isExact).isTrue()
        assertThat(results.first().actualTotalKg).isWithin(0.001).of(102.5)
    }

    @Test
    fun `정확히 맞출 수 없으면 아래 위 근사 2안을 제시한다`() {
        val results = PlateSolver.solve(101.0, 20.0, standard)

        assertThat(results).hasSize(2)
        assertThat(results.all { !it.isExact }).isTrue()
        assertThat(results.map { it.actualTotalKg }).containsExactly(100.0, 102.5).inOrder()
    }

    @Test
    fun `목표가 바 무게와 같으면 빈 바`() {
        val results = PlateSolver.solve(20.0, 20.0, standard)

        assertThat(results).hasSize(1)
        assertThat(results.first().isExact).isTrue()
        assertThat(results.first().plates).isEmpty()
        assertThat(results.first().actualTotalKg).isWithin(0.001).of(20.0)
    }

    @Test
    fun `목표가 바보다 가벼우면 조합 불가`() {
        assertThat(PlateSolver.solve(15.0, 20.0, standard)).isEmpty()
    }

    @Test
    fun `보유 수량 제약을 넘지 않는다`() {
        // 한쪽에 20kg 1장만 보유 → 최대 총 중량 60kg
        val limited = listOf(Plate(20.0, 1))
        val results = PlateSolver.solve(100.0, 20.0, limited)

        assertThat(results).isNotEmpty()
        results.forEach { r ->
            val used = r.plates.firstOrNull { it.weightKg == 20.0 }?.count ?: 0
            assertThat(used).isAtMost(1)
        }
    }

    @Test
    fun `표준 규격이 아닌 임의 원판도 정확 조합을 찾는다`() {
        // 그리디로는 실패하는 조합: 목표 한쪽 12kg, 보유 7kg·5kg
        val custom = listOf(Plate(7.0, 1), Plate(5.0, 1))
        val results = PlateSolver.solve(targetKg = 44.0, barKg = 20.0, inventory = custom)

        assertThat(results).hasSize(1)
        assertThat(results.first().isExact).isTrue()
        assertThat(results.first().actualTotalKg).isWithin(0.001).of(44.0)
    }

    @Test
    fun `보유 원판이 없으면 조합 불가`() {
        assertThat(PlateSolver.solve(100.0, 20.0, emptyList())).isEmpty()
    }
}
