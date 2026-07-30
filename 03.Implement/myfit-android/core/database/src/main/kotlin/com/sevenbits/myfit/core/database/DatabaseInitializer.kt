package com.sevenbits.myfit.core.database

import com.sevenbits.myfit.core.database.seed.DatabaseSeeder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DB 초기 적재 진입점. (03_모듈설계서 §6)
 *
 * `:core:database` 내부 타입(Entity/DAO/Seeder)은 모두 `internal` 이므로(R-5),
 * 모듈 밖에서는 이 클래스만 호출한다.
 */
@Singleton
class DatabaseInitializer @Inject internal constructor(
    private val seeder: DatabaseSeeder,
) {

    /**
     * 최초 실행 시 종목 마스터를 적재한다. 이미 적재되어 있으면 아무 것도 하지 않는다(멱등).
     *
     * @return 새로 적재한 종목 수
     */
    suspend fun initialize(nowMillis: Long = System.currentTimeMillis()): Int =
        seeder.seedIfEmpty(nowMillis)
}
