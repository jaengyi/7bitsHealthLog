package com.sevenbits.myfit.core.common

/**
 * 화면 간 결과 전달 키. (03_모듈설계서 §1.3 R-3)
 *
 * feature 모듈은 서로를 참조하지 않으므로, 화면 A 가 화면 B 를 호출하고 결과를 돌려받는
 * 흐름은 **NavBackStackEntry 의 SavedStateHandle** 을 경유한다.
 * 키를 공용 모듈에 두어 양쪽이 같은 이름을 쓰도록 강제한다.
 */
object NavResult {

    /**
     * 종목 선택 화면(SCR-EXR-001)이 운동일지(SCR-WRK-001)로 돌려주는 종목 ID 목록.
     *
     * `SavedStateHandle` 은 프로세스 재생성을 넘겨야 하므로 Bundle 에 안전한 타입만 담는다.
     * 목록 대신 구분자로 이은 문자열을 쓰는 이유다 — 종목 ID 에는 구분자가 포함되지 않는다.
     */
    const val SELECTED_EXERCISE_IDS = "result:selectedExerciseIds"

    const val DELIMITER = ","

    fun encodeIds(ids: List<String>): String = ids.joinToString(DELIMITER)

    fun decodeIds(encoded: String): List<String> =
        encoded.split(DELIMITER).map { it.trim() }.filter { it.isNotEmpty() }
}
