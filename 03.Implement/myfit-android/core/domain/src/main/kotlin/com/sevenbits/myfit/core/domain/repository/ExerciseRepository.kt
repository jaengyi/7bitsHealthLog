package com.sevenbits.myfit.core.domain.repository

import com.sevenbits.myfit.core.domain.model.BodyPart
import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseFilter
import com.sevenbits.myfit.core.domain.model.ExerciseNote
import com.sevenbits.myfit.core.domain.model.ExerciseReference
import kotlinx.coroutines.flow.Flow

/**
 * 종목 저장소 계약. (03_모듈설계서 §1.3 R-2)
 *
 * `:feature:*` 는 구현체가 아니라 이 인터페이스에만 의존한다.
 * 구현 바인딩은 `:app` 의 Hilt 모듈에서만 수행한다.
 */
interface ExerciseRepository {

    /** 검색·필터 결과. 초성 판별과 상한 적용은 구현에서 처리한다. (FN-EXR-008/009) */
    suspend fun search(filter: ExerciseFilter, limit: Int = 50): List<Exercise>

    fun observeById(exerciseId: String): Flow<Exercise?>

    suspend fun findById(exerciseId: String): Exercise?

    /** 최근 수행한 종목. 종목 선택 화면 최상단에 노출한다. (FN-EXR-011) */
    suspend fun findRecentlyUsed(limit: Int = 20): List<Exercise>

    fun observeFavorites(): Flow<List<Exercise>>

    suspend fun toggleFavorite(exerciseId: String)

    fun observeBodyParts(): Flow<List<BodyPart>>

    /** 사용자 정의 종목 등록·수정 (FN-EXR-005/006) */
    suspend fun upsertUserExercise(exercise: Exercise)

    /**
     * 종목 삭제. 기록이 존재하면 논리 삭제(`is_active=false`)로 처리한다. (FN-EXR-007)
     *
     * @return 논리 삭제되었으면 true, 물리 삭제되었으면 false
     */
    suspend fun deleteOrDeactivate(exerciseId: String): Boolean

    // ── 자세 가이드 (REQ-EXR-003) ────────────────────────

    /** 기본 제공 + 사용자 등록 링크. 기본 제공이 앞에 온다. */
    fun observeReferences(exerciseId: String): Flow<List<ExerciseReference>>

    /** 상세·미리보기 화면 플레이어에 쓰는 기본 제공 자세 영상 1건 (FN-EXR-017) */
    suspend fun findDefaultVideo(exerciseId: String): ExerciseReference?

    suspend fun addUserReference(reference: ExerciseReference)

    /** 사용자 등록분만 삭제할 수 있다. 기본 제공 링크는 보존된다. */
    suspend fun removeUserReference(referenceId: String)

    // ── PT 학습 노트 (REQ-EXR-008) ──────────────────────

    fun observeNotes(exerciseId: String): Flow<List<ExerciseNote>>

    suspend fun countNotes(exerciseId: String): Int

    suspend fun upsertNote(note: ExerciseNote)

    suspend fun deleteNote(noteId: String)
}
