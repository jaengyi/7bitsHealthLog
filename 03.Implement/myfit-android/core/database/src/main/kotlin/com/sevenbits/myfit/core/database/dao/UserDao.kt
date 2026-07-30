package com.sevenbits.myfit.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sevenbits.myfit.core.database.entity.UserEntity
import com.sevenbits.myfit.core.database.entity.UserSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface UserDao {

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertSetting(setting: UserSettingEntity)

    @Query("SELECT * FROM user LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun findCurrentUser(): UserEntity?

    @Query("SELECT * FROM user_setting WHERE user_id = :userId")
    fun observeSetting(userId: String): Flow<UserSettingEntity?>

    @Query("SELECT * FROM user_setting WHERE user_id = :userId")
    suspend fun findSetting(userId: String): UserSettingEntity?

    /** 게스트 데이터 승계 — 로컬 레코드의 소유자를 일괄 변경한다 (FN-CMN-003) */
    @Query("UPDATE workout_log SET user_id = :newUserId, updated_at = :now, is_dirty = 1 WHERE user_id = :guestUserId")
    suspend fun reassignWorkoutLogs(guestUserId: String, newUserId: String, now: Long): Int
}
