package com.sevenbits.myfit.core.domain.usecase.exercise

import com.sevenbits.myfit.core.domain.model.Exercise
import com.sevenbits.myfit.core.domain.model.ExerciseFilter
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * 종목 검색. (FN-EXR-008/009 / REQ-EXR-005)
 *
 * 검색 결과는 300ms 이내에 표시되어야 한다. 이를 위해
 * - 초성은 적재 시점에 미리 계산된 컬럼으로 조회하고
 * - 결과는 [SEARCH_LIMIT] 건으로 제한한다.
 *
 * 디바운스는 화면(ViewModel)의 책임이다 — UseCase 는 순수하게 유지한다.
 */
class SearchExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(filter: ExerciseFilter): List<Exercise> =
        repository.search(filter.normalized(), limit = SEARCH_LIMIT)

    /** 앞뒤 공백은 사용자의 의도가 아니므로 제거한다. */
    private fun ExerciseFilter.normalized() = copy(query = query.trim())

    companion object {
        const val SEARCH_LIMIT = 50
    }
}

/**
 * 종목 선택 화면의 초기 목록. (FN-EXR-010/011)
 *
 * 최근 사용과 즐겨찾기를 최상단에 노출한다 —
 * 대부분의 종목 추가는 이 두 목록에서 끝난다.
 */
class GetExercisePickerSectionsUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(): PickerSections = PickerSections(
        recent = repository.findRecentlyUsed(limit = RECENT_LIMIT),
    )

    companion object {
        const val RECENT_LIMIT = 20
    }
}

data class PickerSections(
    val recent: List<Exercise> = emptyList(),
)
