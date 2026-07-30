package com.sevenbits.myfit.core.domain.calculator

import kotlin.math.abs

/**
 * 원판 조합 산출. (FN-TOL-008 / 07_핵심로직설계서 §6)
 *
 * 보유 원판 수량 제약 하에서 한쪽에 올릴 조합을 구한다.
 * 정확히 맞출 수 없으면 아래/위 근사값 2안을 제시한다.
 */
object PlateSolver {

    private const val EPSILON = 1e-6

    /** 보유 원판 — 수량은 **한쪽 기준** */
    data class Plate(val weightKg: Double, val countPerSide: Int)

    data class PlateCount(val weightKg: Double, val count: Int)

    data class Result(
        val plates: List<PlateCount>,
        /** 바를 포함한 실제 총 중량 */
        val actualTotalKg: Double,
        val isExact: Boolean,
    )

    /**
     * @return 조합 결과. 정확 조합이 있으면 1건, 없으면 근사 2건(아래/위), 불가능하면 빈 목록.
     */
    fun solve(targetKg: Double, barKg: Double, inventory: List<Plate>): List<Result> {
        val perSide = (targetKg - barKg) / 2.0
        if (perSide < -EPSILON) return emptyList() // 목표가 바보다 가벼움
        if (abs(perSide) < EPSILON) {
            return listOf(Result(emptyList(), barKg, isExact = true))
        }

        val usable = inventory.filter { it.weightKg > 0 && it.countPerSide > 0 }
            .sortedByDescending { it.weightKg }
        if (usable.isEmpty()) return emptyList()

        exactCombination(perSide, usable)?.let { combo ->
            return listOf(combo.toResult(barKg, isExact = true))
        }

        val lower = greedyFloor(perSide, usable)
        val upper = smallestAbove(perSide, usable)
        return listOfNotNull(lower, upper)
            .distinctBy { it.totalKg }
            .map { it.toResult(barKg, isExact = false) }
    }

    // ── 내부 구현 ────────────────────────────────────────────

    private data class Combo(val plates: List<PlateCount>) {
        val totalKg: Double get() = plates.sumOf { it.weightKg * it.count }
        fun toResult(barKg: Double, isExact: Boolean) =
            Result(plates, barKg + totalKg * 2, isExact)
    }

    /**
     * 큰 원판 우선 그리디. 표준 원판 규격(20/15/10/5/2.5/1.25)에서는 최적해를 준다.
     * 사용자가 임의 규격을 등록한 경우를 대비해 실패 시 DP 로 재확인한다.
     */
    private fun exactCombination(perSide: Double, inv: List<Plate>): Combo? {
        greedy(perSide, inv)?.let { return it }
        return dpExact(perSide, inv)
    }

    private fun greedy(perSide: Double, inv: List<Plate>): Combo? {
        var remain = perSide
        val used = mutableListOf<PlateCount>()
        for (plate in inv) {
            val n = minOf((remain / plate.weightKg + EPSILON).toInt(), plate.countPerSide)
            if (n > 0) {
                used += PlateCount(plate.weightKg, n)
                remain -= n * plate.weightKg
            }
        }
        return if (abs(remain) < EPSILON) Combo(used) else null
    }

    /** perSide 이하에서 가장 무거운 조합 */
    private fun greedyFloor(perSide: Double, inv: List<Plate>): Combo? {
        var remain = perSide
        val used = mutableListOf<PlateCount>()
        for (plate in inv) {
            val n = minOf((remain / plate.weightKg + EPSILON).toInt(), plate.countPerSide)
            if (n > 0) {
                used += PlateCount(plate.weightKg, n)
                remain -= n * plate.weightKg
            }
        }
        return if (used.isEmpty()) null else Combo(used)
    }

    /** perSide 를 초과하는 조합 중 가장 가벼운 것 */
    private fun smallestAbove(perSide: Double, inv: List<Plate>): Combo? =
        allSums(inv).firstOrNull { it.totalKg > perSide + EPSILON }

    private fun dpExact(perSide: Double, inv: List<Plate>): Combo? =
        allSums(inv).firstOrNull { abs(it.totalKg - perSide) < EPSILON }

    /**
     * 가능한 모든 조합을 무게 오름차순으로 열거.
     * 원판 종류 ≤ 10, 종류당 수량 ≤ 10 을 전제로 탐색 공간이 충분히 작다.
     */
    private fun allSums(inv: List<Plate>): List<Combo> {
        var combos = listOf(Combo(emptyList()))
        for (plate in inv) {
            val expanded = mutableListOf<Combo>()
            for (combo in combos) {
                for (n in 0..plate.countPerSide) {
                    expanded += if (n == 0) combo
                    else Combo(combo.plates + PlateCount(plate.weightKg, n))
                }
            }
            combos = expanded
        }
        return combos.sortedBy { it.totalKg }
    }
}
