package com.sevenbits.myfit.feature.settings.component

import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import com.sevenbits.myfit.core.domain.model.ThemeMode

/**
 * 열거형 -> 한국어 표시명.
 *
 * 프로필 편집·온보딩·환경설정·마이페이지가 같은 열거형을 각자 번역하면 표기가 갈린다.
 * 문자열 리소스로 뽑는 것은 다국어(FN-CMN-011, P3) 진입 시점에 한다.
 */
internal fun Gender.label(): String = when (this) {
    Gender.MALE -> "남"
    Gender.FEMALE -> "여"
    Gender.UNSPECIFIED -> "선택안함"
}

internal fun CareerLevel.label(): String = when (this) {
    CareerLevel.BEGINNER -> "입문"
    CareerLevel.INTERMEDIATE -> "중급"
    CareerLevel.ADVANCED -> "상급"
}

internal fun GoalType.label(): String = when (this) {
    GoalType.DIET -> "다이어트"
    GoalType.BULK -> "벌크업"
    GoalType.MAINTAIN -> "유지"
}

internal fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "시스템"
    ThemeMode.LIGHT -> "라이트"
    ThemeMode.DARK -> "다크"
}

internal fun OneRmFormula.label(): String = when (this) {
    OneRmFormula.EPLEY -> "Epley"
    OneRmFormula.BRZYCKI -> "Brzycki"
    OneRmFormula.LOMBARDI -> "Lombardi"
}

internal fun WeightUnit.label(): String = when (this) {
    WeightUnit.KG -> "kg"
    WeightUnit.LB -> "lb"
}
