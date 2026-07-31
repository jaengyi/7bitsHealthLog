package com.sevenbits.myfit.core.domain.repository

import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 프로필·설정 저장소 계약. (REQ-CMN-002/003/004/005)
 */
interface UserRepository {

    fun observeProfile(): Flow<UserProfile?>

    suspend fun findProfile(): UserProfile?

    suspend fun upsertProfile(profile: UserProfile)

    fun observeSetting(): Flow<UserSetting>

    /**
     * 설정 1회 조회.
     *
     * ViewModel 초기화 시점처럼 Flow 구독이 과한 경우에 쓴다.
     */
    suspend fun findSetting(): UserSetting

    suspend fun upsertSetting(setting: UserSetting)

    /** 온보딩 완료 표시 (FN-CMN-009) */
    suspend fun markOnboardingCompleted()
}
