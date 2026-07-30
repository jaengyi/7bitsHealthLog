package com.sevenbits.myfit.core.database

import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.dao.ExerciseNoteDao
import com.sevenbits.myfit.core.database.dao.ExerciseReferenceDao
import com.sevenbits.myfit.core.database.entity.ExerciseBodyPartEntity
import com.sevenbits.myfit.core.database.entity.ExerciseEntity
import com.sevenbits.myfit.core.database.entity.ExerciseFavoriteEntity
import com.sevenbits.myfit.core.database.entity.ExerciseNoteEntity
import com.sevenbits.myfit.core.database.entity.ExerciseReferenceEntity
import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Equipment
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseNote
import com.sevenbits.myfit.core.domain.model.ExerciseReference
import com.sevenbits.myfit.core.domain.model.ExerciseType
import com.sevenbits.myfit.core.domain.model.MovementPattern
import com.sevenbits.myfit.core.domain.model.MovementType
import com.sevenbits.myfit.core.domain.model.RecordType
import com.sevenbits.myfit.core.domain.model.RefProvider
import com.sevenbits.myfit.core.domain.model.RefType
import com.sevenbits.myfit.core.domain.text.ChosungExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 종목 로컬 데이터 소스.
 *
 * Room Entity 는 `:core:database` 밖으로 노출하지 않으므로(R-5),
 * 이 클래스가 **Entity ↔ Domain Model 경계**를 담당한다.
 * `:core:data` 는 이 공개 타입만 사용한다.
 */
@Singleton
class ExerciseLocalSource @Inject internal constructor(
    private val exerciseDao: ExerciseDao,
    private val noteDao: ExerciseNoteDao,
    private val referenceDao: ExerciseReferenceDao,
) {

    /**
     * 종목 검색. 초성으로만 구성된 검색어면 초성 인덱스를, 아니면 이름을 조회한다.
     * (FN-EXR-008 / 07_핵심로직설계서 §10.2)
     */
    suspend fun search(query: String, limit: Int): List<Exercise> {
        val trimmed = query.trim()
        val rows = if (trimmed.isEmpty()) {
            exerciseDao.searchAll(limit)
        } else {
            // 초성 검색은 전방 일치만 허용한다. 중간 일치까지 허용하면 노이즈가 급증한다.
            val chosung = if (ChosungExtractor.isChosungOnly(trimmed)) trimmed else NO_MATCH
            exerciseDao.search(trimmed, chosung, limit)
        }
        return rows.enrich()
    }

    /**
     * 부위 매핑과 즐겨찾기 여부를 한 번에 결합한다.
     *
     * 종목마다 개별 조회하면 N+1 이 되므로, 결과 집합 전체에 대해 각각 1회씩만 조회한다.
     */
    private suspend fun List<ExerciseEntity>.enrich(): List<Exercise> {
        if (isEmpty()) return emptyList()
        val mappings = exerciseDao.findBodyPartMappings(map { it.id })
        val favoriteIds = exerciseDao.findFavoriteIds().toSet()
        return map { entity ->
            entity.toModel()
                .withBodyParts(mappings)
                .copy(isFavorite = entity.id in favoriteIds)
        }
    }

    private fun Exercise.withBodyParts(mappings: List<ExerciseBodyPartEntity>): Exercise {
        val mine = mappings.filter { it.exerciseId == id }
        return copy(
            primaryBodyParts = mine.filter { it.role == ROLE_PRIMARY }.map { it.bodyPartCode },
            secondaryBodyParts = mine.filter { it.role == ROLE_SECONDARY }.map { it.bodyPartCode },
        )
    }

    suspend fun findById(id: String): Exercise? = exerciseDao.findById(id)?.let { entity ->
        entity.toModel().withBodyParts(exerciseDao.findBodyPartMappings(listOf(id)))
            .copy(isFavorite = id in exerciseDao.findFavoriteIds())
    }

    fun observeById(id: String): Flow<Exercise?> =
        exerciseDao.observeById(id).map { it?.toModel() }

    suspend fun findRecentlyUsed(limit: Int): List<Exercise> =
        exerciseDao.findRecentlyUsed(limit).enrich()

    fun observeFavorites(): Flow<List<Exercise>> =
        exerciseDao.observeFavorites().map { list -> list.map { it.toModel().copy(isFavorite = true) } }

    /** 즐겨찾기 토글. 이미 있으면 해제, 없으면 등록한다. (FN-EXR-010) */
    suspend fun toggleFavorite(exerciseId: String) {
        val now = System.currentTimeMillis()
        if (exerciseId in exerciseDao.findFavoriteIds()) {
            exerciseDao.deleteFavorite(exerciseId)
        } else {
            exerciseDao.upsertFavorite(
                ExerciseFavoriteEntity(
                    id = UUID.randomUUID().toString(),
                    userId = LOCAL_USER_ID,
                    exerciseId = exerciseId,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /** 사용자 정의 종목 등록·수정 (FN-EXR-005/006) */
    suspend fun upsertUserExercise(exercise: Exercise) {
        val now = System.currentTimeMillis()
        require(exerciseDao.countByName(exercise.name, exercise.id) == 0) {
            "이미 같은 이름의 종목이 있습니다: ${exercise.name}"
        }
        exerciseDao.upsert(
            ExerciseEntity(
                id = exercise.id.ifBlank { UUID.randomUUID().toString() },
                userId = LOCAL_USER_ID,
                name = exercise.name,
                nameEn = exercise.nameEn,
                nameChosung = ChosungExtractor.extract(exercise.name),
                recordType = exercise.recordType.name,
                exerciseType = exercise.exerciseType.name,
                equipment = exercise.equipment.name,
                movementType = exercise.movementType.name,
                movementPattern = exercise.movementPattern?.name,
                isUnilateral = exercise.isUnilateral,
                description = exercise.description,
                caution = exercise.caution,
                guideImageAsset = exercise.guideImageAsset,
                muscleMapAsset = exercise.muscleMapAsset,
                defaultRestSec = exercise.defaultRestSec,
                isUserDefined = true,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val mappings = exercise.primaryBodyParts.map {
            ExerciseBodyPartEntity(exercise.id, it, ROLE_PRIMARY)
        } + exercise.secondaryBodyParts.map {
            ExerciseBodyPartEntity(exercise.id, it, ROLE_SECONDARY)
        }
        if (mappings.isNotEmpty()) exerciseDao.upsertExerciseBodyParts(mappings)
    }

    /**
     * 종목 삭제. **기록이 존재하면 물리 삭제하지 않는다** — 과거 일지가 종목명을 잃기 때문이다.
     * (FN-EXR-007)
     *
     * @return 논리 삭제되었으면 true
     */
    suspend fun deleteOrDeactivate(exerciseId: String): Boolean {
        val hasRecords = exerciseDao.countRecordsOf(exerciseId) > 0
        if (hasRecords) {
            exerciseDao.deactivate(exerciseId, System.currentTimeMillis())
        } else {
            exerciseDao.deleteUserExercise(exerciseId)
        }
        return hasRecords
    }

    suspend fun findDefaultVideo(exerciseId: String): ExerciseReference? =
        referenceDao.findDefaultVideo(exerciseId)?.let {
            ExerciseReference(
                id = it.id,
                exerciseId = it.exerciseId,
                refType = RefType.VIDEO,
                provider = enumOrNull<RefProvider>(it.provider) ?: RefProvider.WEB,
                url = it.url,
                videoId = it.videoId,
                startSec = it.startSec,
                title = it.title,
                isDefault = true,
            )
        }

    suspend fun addUserReference(reference: ExerciseReference) {
        val now = System.currentTimeMillis()
        referenceDao.upsert(
            ExerciseReferenceEntity(
                id = reference.id.ifBlank { UUID.randomUUID().toString() },
                exerciseId = reference.exerciseId,
                userId = LOCAL_USER_ID,
                refType = reference.refType.name,
                provider = reference.provider.name,
                url = reference.url,
                videoId = reference.videoId,
                startSec = reference.startSec,
                title = reference.title,
                isDefault = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun removeUserReference(referenceId: String) =
        referenceDao.deleteUserReference(referenceId)

    suspend fun upsertNote(note: ExerciseNote) {
        val now = System.currentTimeMillis()
        noteDao.upsert(
            ExerciseNoteEntity(
                id = note.id.ifBlank { UUID.randomUUID().toString() },
                userId = LOCAL_USER_ID,
                exerciseId = note.exerciseId,
                content = note.content,
                trainerName = note.trainerName,
                noteDate = note.noteDate,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteNote(noteId: String) = noteDao.delete(noteId)

    fun observeBodyParts(): Flow<List<BodyPart>> =
        exerciseDao.observeBodyParts().map { list ->
            list.map { BodyPart(it.code, it.name, it.upperGroup, it.sortOrder) }
        }

    suspend fun findByPrimaryBodyPart(code: String): List<Exercise> =
        exerciseDao.findByPrimaryBodyPart(code).map { it.toModel() }

    fun observeReferences(exerciseId: String): Flow<List<ExerciseReference>> =
        referenceDao.observeByExercise(exerciseId).map { list ->
            list.map {
                ExerciseReference(
                    id = it.id,
                    exerciseId = it.exerciseId,
                    refType = enumOrNull<RefType>(it.refType) ?: RefType.VIDEO,
                    provider = enumOrNull<RefProvider>(it.provider) ?: RefProvider.WEB,
                    url = it.url,
                    videoId = it.videoId,
                    startSec = it.startSec,
                    title = it.title,
                    isDefault = it.isDefault,
                )
            }
        }

    fun observeNotes(exerciseId: String): Flow<List<ExerciseNote>> =
        noteDao.observeByExercise(exerciseId).map { list ->
            list.map {
                ExerciseNote(
                    id = it.id,
                    exerciseId = it.exerciseId,
                    content = it.content,
                    trainerName = it.trainerName,
                    noteDate = it.noteDate,
                )
            }
        }

    suspend fun countNotes(exerciseId: String): Int = noteDao.countByExercise(exerciseId)

    // ── 매핑 ─────────────────────────────────────────────

    private fun ExerciseEntity.toModel() = Exercise(
        id = id,
        name = name,
        nameEn = nameEn,
        recordType = enumOrNull<RecordType>(recordType) ?: RecordType.WEIGHT_REPS,
        exerciseType = enumOrNull<ExerciseType>(exerciseType) ?: ExerciseType.WEIGHT,
        equipment = enumOrNull<Equipment>(equipment) ?: Equipment.ETC,
        movementType = enumOrNull<MovementType>(movementType) ?: MovementType.COMPOUND,
        movementPattern = movementPattern?.let { enumOrNull<MovementPattern>(it) },
        isUnilateral = isUnilateral,
        description = description,
        caution = caution,
        guideImageAsset = guideImageAsset,
        muscleMapAsset = muscleMapAsset,
        defaultRestSec = defaultRestSec,
        isUserDefined = isUserDefined,
    )

    /**
     * 저장된 코드값이 현재 앱의 enum 에 없을 수 있다(구버전 데이터, 손상된 값).
     * 예외를 던지는 대신 null 로 처리하고 호출부에서 기본값을 적용한다 —
     * 코드값 하나 때문에 화면 전체가 죽는 것을 막는다.
     */
    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    companion object {
        /** 어떤 초성 컬럼과도 일치하지 않는 값 — 초성 검색을 비활성화할 때 사용 */
        private const val NO_MATCH = " "
        private const val ROLE_PRIMARY = "PRIMARY"
        private const val ROLE_SECONDARY = "SECONDARY"

        /**
         * 로컬 단일 사용자 식별자.
         *
         * Phase 1 은 게스트 모드 단독 사용이 기본이다(REQ-CMN-001).
         * 계정 연결 시 이 id 의 레코드를 서버 계정으로 승계한다(FN-CMN-003).
         */
        const val LOCAL_USER_ID = "local-user"
    }
}
