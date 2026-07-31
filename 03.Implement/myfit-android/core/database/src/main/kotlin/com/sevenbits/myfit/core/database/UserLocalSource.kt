package com.sevenbits.myfit.core.database

import com.sevenbits.myfit.core.common.DistanceUnit
import com.sevenbits.myfit.core.common.LengthUnit
import com.sevenbits.myfit.core.common.WeightUnit
import com.sevenbits.myfit.core.database.dao.UserDao
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.UserSettingEntity
import com.sevenbits.myfit.core.domain.model.CareerLevel
import com.sevenbits.myfit.core.domain.model.Gender
import com.sevenbits.myfit.core.domain.model.GoalType
import com.sevenbits.myfit.core.domain.model.LoginType
import com.sevenbits.myfit.core.domain.model.OneRmFormula
import com.sevenbits.myfit.core.domain.model.ThemeMode
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 프로필·설정 로컬 데이터 소스.
 *
 * 게스트 사용자 레코드가 없으면 만들어 준다 — 로그인 없이 바로 쓸 수 있어야 한다.
 * (REQ-CMN-001)
 */
@Singleton
class UserLocalSource @Inject internal constructor(
    private val userDao: UserDao,
) {

    fun observeProfile(): Flow<UserProfile?> =
        userDao.observeCurrentUser().map { it?.toModel() }

    suspend fun findProfile(): UserProfile = ensureUser().toModel()

    suspend fun upsertProfile(profile: UserProfile) {
        val existing = ensureUser()
        userDao.upsert(
            existing.copy(
                displayName = profile.displayName,
                gender = profile.gender?.name,
                birthDate = profile.birthDate,
                heightCm = profile.heightCm,
                careerLevel = profile.careerLevel?.name,
                goalType = profile.goalType?.name,
                updatedAt = System.currentTimeMillis(),
                isDirty = true,
            ),
        )
    }

    fun observeSetting(): Flow<UserSetting> =
        userDao.observeSetting(ExerciseLocalSource.LOCAL_USER_ID)
            .map { it?.toModel() ?: UserSetting(userId = ExerciseLocalSource.LOCAL_USER_ID) }

    suspend fun findSetting(): UserSetting = ensureSetting().toModel()

    suspend fun upsertSetting(setting: UserSetting) {
        ensureUser()
        userDao.upsertSetting(
            UserSettingEntity(
                userId = ExerciseLocalSource.LOCAL_USER_ID,
                weightUnit = setting.weightUnit.name,
                lengthUnit = setting.lengthUnit.name,
                distanceUnit = setting.distanceUnit.name,
                themeMode = setting.themeMode.name,
                oneRmFormula = setting.oneRmFormula.name,
                defaultRestSec = setting.defaultRestSec,
                timerSoundEnabled = setting.timerSoundEnabled,
                timerVibrateEnabled = setting.timerVibrateEnabled,
                defaultBarWeightKg = setting.defaultBarWeightKg,
                weeklyGoalCount = setting.weeklyGoalCount,
                weightStepKg = setting.weightStepKg,
                autoBackupEnabled = setting.autoBackupEnabled,
                onboardingCompleted = setting.onboardingCompleted,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markOnboardingCompleted() {
        val current = ensureSetting()
        userDao.upsertSetting(
            current.copy(onboardingCompleted = true, updatedAt = System.currentTimeMillis()),
        )
    }

    private suspend fun ensureUser(): UserEntity {
        userDao.findCurrentUser()?.let { return it }
        val now = System.currentTimeMillis()
        val entity = UserEntity(
            id = ExerciseLocalSource.LOCAL_USER_ID,
            loginType = LoginType.GUEST.name,
            createdAt = now,
            updatedAt = now,
        )
        userDao.upsert(entity)
        return entity
    }

    private suspend fun ensureSetting(): UserSettingEntity {
        ensureUser()
        userDao.findSetting(ExerciseLocalSource.LOCAL_USER_ID)?.let { return it }
        val entity = UserSettingEntity(
            userId = ExerciseLocalSource.LOCAL_USER_ID,
            updatedAt = System.currentTimeMillis(),
        )
        userDao.upsertSetting(entity)
        return entity
    }

    // ── 매핑 ─────────────────────────────────────────────

    private fun UserEntity.toModel() = UserProfile(
        id = id,
        loginType = enumOrNull<LoginType>(loginType) ?: LoginType.GUEST,
        displayName = displayName,
        gender = gender?.let { enumOrNull<Gender>(it) },
        birthDate = birthDate,
        heightCm = heightCm,
        careerLevel = careerLevel?.let { enumOrNull<CareerLevel>(it) },
        goalType = goalType?.let { enumOrNull<GoalType>(it) },
    )

    private fun UserSettingEntity.toModel() = UserSetting(
        userId = userId,
        weightUnit = enumOrNull<WeightUnit>(weightUnit) ?: WeightUnit.KG,
        lengthUnit = enumOrNull<LengthUnit>(lengthUnit) ?: LengthUnit.CM,
        distanceUnit = enumOrNull<DistanceUnit>(distanceUnit) ?: DistanceUnit.KM,
        themeMode = enumOrNull<ThemeMode>(themeMode) ?: ThemeMode.DARK,
        oneRmFormula = enumOrNull<OneRmFormula>(oneRmFormula) ?: OneRmFormula.EPLEY,
        defaultRestSec = defaultRestSec,
        timerSoundEnabled = timerSoundEnabled,
        timerVibrateEnabled = timerVibrateEnabled,
        defaultBarWeightKg = defaultBarWeightKg,
        weeklyGoalCount = weeklyGoalCount,
        weightStepKg = weightStepKg,
        autoBackupEnabled = autoBackupEnabled,
        onboardingCompleted = onboardingCompleted,
    )

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }
}
