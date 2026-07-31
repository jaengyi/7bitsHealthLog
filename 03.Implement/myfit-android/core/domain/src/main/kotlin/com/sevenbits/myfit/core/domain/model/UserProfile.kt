package com.sevenbits.myfit.core.domain.model

import com.sevenbits.myfit.core.common.DistanceUnit
import com.sevenbits.myfit.core.common.LengthUnit
import com.sevenbits.myfit.core.common.WeightUnit

/** 사용자 프로필. (ENT-001 / REQ-CMN-002) */
data class UserProfile(
    val id: String = "",
    val loginType: LoginType = LoginType.GUEST,
    val displayName: String? = null,
    val gender: Gender? = null,
    /** `yyyy-MM-dd` */
    val birthDate: String? = null,
    /** 100~250 검증. BMI 산출 기준 (FN-BDY-002) */
    val heightCm: Double? = null,
    val careerLevel: CareerLevel? = null,
    val goalType: GoalType? = null,
)

enum class LoginType { GUEST, GOOGLE, KAKAO, EMAIL }
enum class Gender { MALE, FEMALE, UNSPECIFIED }
enum class CareerLevel { BEGINNER, INTERMEDIATE, ADVANCED }
enum class GoalType { DIET, BULK, MAINTAIN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 사용자 환경설정. (ENT-002 / REQ-CMN-003/005)
 *
 * DataStore 가 아니라 테이블에 두는 이유: 기기 이전 시 승계되어야 한다. (REQ-DAT-004)
 */
data class UserSetting(
    val userId: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val lengthUnit: LengthUnit = LengthUnit.CM,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    /** 헬스장 조명 환경을 고려해 기본값 다크 (REQ-CMN-005) */
    val themeMode: ThemeMode = ThemeMode.DARK,
    val oneRmFormula: OneRmFormula = OneRmFormula.EPLEY,
    /** 종목별 설정이 없을 때 쓰는 전역 휴식시간 (FN-TOL-002) */
    val defaultRestSec: Int = 90,
    val timerSoundEnabled: Boolean = true,
    val timerVibrateEnabled: Boolean = true,
    val defaultBarWeightKg: Double = 20.0,
    /** 스트릭 판정 기준 (FN-CAL-008) */
    val weeklyGoalCount: Int = 3,
    /** ± 버튼 증감 단위 (FN-WRK-010) */
    val weightStepKg: Double = 2.5,
    val autoBackupEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
