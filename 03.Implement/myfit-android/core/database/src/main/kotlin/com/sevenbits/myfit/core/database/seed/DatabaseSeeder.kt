package com.sevenbits.myfit.core.database.seed

import android.content.Context
import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.entity.BodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseBodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.domain.text.ChosungExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 종목 마스터 초기 적재. (FN-EXR-001 / 03_모듈설계서 §6 "AppInitializer 1회성")
 *
 * 최초 실행 시 **네트워크 없이** 종목을 사용할 수 있어야 한다. (REQ-EXR-001)
 * 이미 적재되어 있으면 아무 것도 하지 않으므로 매 실행 호출해도 안전하다(멱등).
 */
@Singleton
internal class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @return 새로 적재한 종목 수. 이미 적재된 상태면 0.
     */
    suspend fun seedIfEmpty(nowMillis: Long): Int {
        if (exerciseDao.count() > 0) return 0

        val bodyParts = readSeed<SeedBodyPart>(BODY_PARTS_ASSET)
        val exercises = readSeed<SeedExercise>(EXERCISES_ASSET)

        exerciseDao.upsertBodyParts(
            bodyParts.map {
                BodyPartEntity(
                    code = it.code,
                    name = it.name,
                    upperGroup = it.upperGroup,
                    sortOrder = it.sortOrder,
                )
            },
        )

        exerciseDao.upsertAll(exercises.map { it.toEntity(nowMillis) })

        // 주동근·보조근 매핑 — 부위별 세트 비중 집계의 조인 축 (FN-WRK-028)
        val mappings = exercises.flatMap { seed ->
            seed.primary.map { ExerciseBodyPartEntity(seed.id, it, ROLE_PRIMARY) } +
                seed.secondary.map { ExerciseBodyPartEntity(seed.id, it, ROLE_SECONDARY) }
        }
        exerciseDao.upsertExerciseBodyParts(mappings)

        return exercises.size
    }

    private inline fun <reified T> readSeed(assetPath: String): List<T> =
        context.assets.open(assetPath).bufferedReader().use { reader ->
            json.decodeFromString<List<T>>(reader.readText())
        }

    private fun SeedExercise.toEntity(now: Long) = ExerciseEntity(
        id = id,
        userId = null,
        name = name,
        nameEn = nameEn,
        // 검색 300ms 기준을 맞추기 위해 적재 시점에 미리 계산한다 (REQ-EXR-005)
        nameChosung = ChosungExtractor.extract(name),
        recordType = recordType,
        exerciseType = exerciseType,
        equipment = equipment,
        movementType = movementType,
        movementPattern = movementPattern,
        isUnilateral = isUnilateral,
        description = description,
        caution = caution,
        guideImageAsset = guideImageAsset,
        defaultRestSec = defaultRestSec,
        isUserDefined = false,
        isActive = true,
        createdAt = now,
        updatedAt = now,
        // 기본 종목은 앱 내장이므로 서버로 전송하지 않는다 (05_API설계서 §5.5)
        isDirty = false,
    )

    private companion object {
        const val BODY_PARTS_ASSET = "seed/body_parts.json"
        const val EXERCISES_ASSET = "seed/exercises.json"
        const val ROLE_PRIMARY = "PRIMARY"
        const val ROLE_SECONDARY = "SECONDARY"
    }
}
