package com.sevenbits.myfit.core.domain.text

/**
 * 한글 초성 추출. (FN-EXR-008 / 07_핵심로직설계서 §10)
 *
 * 종목 검색 300ms 기준(REQ-EXR-005)을 만족시키려면 런타임 변환이 아니라
 * **등록·시드 시점에 미리 계산해 `exercise.name_chosung` 컬럼에 저장**해야 한다.
 */
object ChosungExtractor {

    private const val HANGUL_BASE = 0xAC00
    private const val HANGUL_END = 0xD7A3

    /** 초성 1자당 (중성 21 × 종성 28) = 588 */
    private const val CHOSUNG_UNIT = 588

    private val CHOSUNG = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    )

    /**
     * 문자열의 초성 인덱스를 만든다.
     *
     * - 한글 음절 → 초성 1자
     * - 영문·숫자 → 소문자로 그대로 (영문 종목명도 같은 컬럼으로 검색)
     * - 공백·기호 → 제거
     *
     * 예: `벤치프레스` → `ㅂㅊㅍㄹㅅ`
     */
    fun extract(text: String): String = buildString(text.length) {
        text.forEach { ch ->
            when {
                ch.code in HANGUL_BASE..HANGUL_END ->
                    append(CHOSUNG[(ch.code - HANGUL_BASE) / CHOSUNG_UNIT])

                ch.isLetterOrDigit() -> append(ch.lowercaseChar())

                else -> Unit // 공백·기호 제거
            }
        }
    }

    /**
     * 검색어가 초성만으로 구성되었는지 판정한다.
     *
     * 초성 검색은 **전방 일치**로만 수행한다. 중간 일치까지 허용하면
     * 두 글자 입력(`ㅂㅊ`)에 무관한 종목이 대량으로 걸려 노이즈가 급증한다.
     */
    fun isChosungOnly(query: String): Boolean {
        val trimmed = query.filterNot { it.isWhitespace() }
        return trimmed.isNotEmpty() && trimmed.all { it in CHOSUNG }
    }
}
