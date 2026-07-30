package com.sevenbits.myfit.core.common

import kotlin.math.round

enum class WeightUnit { KG, LB }
enum class LengthUnit { CM, INCH }
enum class DistanceUnit { KM, MILE }

/**
 * 단위 변환. (REQ-CMN-003 / 07_핵심로직설계서 §11)
 *
 * **저장은 항상 SI(kg/cm/km)**, 변환은 표시 시점에만 수행한다. (설계 원칙 P7)
 * 표시값은 소수 1자리로 반올림하되, 저장값은 반올림하지 않는다(왕복 오차 누적 방지).
 */
object UnitConverter {

    private const val LB_PER_KG = 2.20462262
    private const val CM_PER_INCH = 2.54
    private const val KM_PER_MILE = 1.609344

    fun kgToDisplay(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> round1(kg)
        WeightUnit.LB -> round1(kg * LB_PER_KG)
    }

    fun displayToKg(value: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> value
        WeightUnit.LB -> value / LB_PER_KG
    }

    fun cmToDisplay(cm: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.CM -> round1(cm)
        LengthUnit.INCH -> round1(cm / CM_PER_INCH)
    }

    fun displayToCm(value: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.CM -> value
        LengthUnit.INCH -> value * CM_PER_INCH
    }

    fun kmToDisplay(km: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.KM -> round1(km)
        DistanceUnit.MILE -> round1(km / KM_PER_MILE)
    }

    fun displayToKm(value: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.KM -> value
        DistanceUnit.MILE -> value * KM_PER_MILE
    }

    /** 원판 단위(기본 2.5kg)로 반올림 — %1RM 환산 목표 중량에 사용 */
    fun roundToStep(value: Double, step: Double): Double =
        if (step <= 0.0) value else round(value / step) * step

    private fun round1(value: Double): Double = round(value * 10.0) / 10.0
}
