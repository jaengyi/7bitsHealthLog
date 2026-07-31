package com.sevenbits.myfit.core.data.repository

import com.sevenbits.myfit.core.database.UserLocalSource
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import com.sevenbits.myfit.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val local: UserLocalSource,
) : UserRepository {

    override fun observeProfile(): Flow<UserProfile?> = local.observeProfile()

    override suspend fun findProfile(): UserProfile? = local.findProfile()

    override suspend fun upsertProfile(profile: UserProfile) = local.upsertProfile(profile)

    override fun observeSetting(): Flow<UserSetting> = local.observeSetting()

    override suspend fun findSetting(): UserSetting = local.findSetting()

    override suspend fun upsertSetting(setting: UserSetting) = local.upsertSetting(setting)

    override suspend fun markOnboardingCompleted() = local.markOnboardingCompleted()
}
