package com.sevenbits.myfit.core.data.repository

import com.sevenbits.myfit.core.database.ExerciseLocalSource
import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseFilter
import com.sevenbits.myfit.core.domain.model.ExerciseNote
import com.sevenbits.myfit.core.domain.model.ExerciseReference
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 종목 저장소 구현. (03_모듈설계서 §2.2)
 *
 * 로컬(Room)이 단일 진실 공급원이다. 서버 조회를 하지 않는다. (설계 원칙 P1/P2)
 */
@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val local: ExerciseLocalSource,
) : ExerciseRepository {

    override suspend fun search(filter: ExerciseFilter, limit: Int): List<Exercise> {
        val found = local.search(filter.query, limit)
        // 부위·장비 필터는 메모리에서 적용한다.
        // 검색 결과가 이미 limit(50)건으로 제한되어 있어 비용이 무시할 수준이고,
        // 다중 조건 조합마다 쿼리를 늘리는 것보다 단순하다.
        return found.filter { exercise ->
            val partOk = filter.bodyPartCodes.isEmpty() ||
                exercise.primaryBodyParts.any { it in filter.bodyPartCodes }
            val equipOk = filter.equipments.isEmpty() || exercise.equipment in filter.equipments
            partOk && equipOk
        }
    }

    override fun observeById(exerciseId: String): Flow<Exercise?> = local.observeById(exerciseId)

    override suspend fun findById(exerciseId: String): Exercise? = local.findById(exerciseId)

    override suspend fun findRecentlyUsed(limit: Int): List<Exercise> =
        local.findRecentlyUsed(limit)

    override fun observeFavorites(): Flow<List<Exercise>> = local.observeFavorites()

    override suspend fun toggleFavorite(exerciseId: String) = local.toggleFavorite(exerciseId)

    override fun observeBodyParts(): Flow<List<BodyPart>> = local.observeBodyParts()

    override suspend fun upsertUserExercise(exercise: Exercise) =
        local.upsertUserExercise(exercise)

    override suspend fun deleteOrDeactivate(exerciseId: String): Boolean =
        local.deleteOrDeactivate(exerciseId)

    override fun observeReferences(exerciseId: String): Flow<List<ExerciseReference>> =
        local.observeReferences(exerciseId)

    override suspend fun findDefaultVideo(exerciseId: String): ExerciseReference? =
        local.findDefaultVideo(exerciseId)

    override suspend fun addUserReference(reference: ExerciseReference) =
        local.addUserReference(reference)

    override suspend fun removeUserReference(referenceId: String) =
        local.removeUserReference(referenceId)

    override fun observeNotes(exerciseId: String): Flow<List<ExerciseNote>> =
        local.observeNotes(exerciseId)

    override suspend fun countNotes(exerciseId: String): Int = local.countNotes(exerciseId)

    override suspend fun upsertNote(note: ExerciseNote) = local.upsertNote(note)

    override suspend fun deleteNote(noteId: String) = local.deleteNote(noteId)
}
