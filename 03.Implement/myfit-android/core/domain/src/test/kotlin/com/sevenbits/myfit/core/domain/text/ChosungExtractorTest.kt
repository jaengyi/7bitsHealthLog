package com.sevenbits.myfit.core.domain.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 07_핵심로직설계서 §10.1 */
class ChosungExtractorTest {

    @Test
    fun `한글 종목명의 초성을 추출한다`() {
        assertThat(ChosungExtractor.extract("벤치프레스")).isEqualTo("ㅂㅊㅍㄹㅅ")
        assertThat(ChosungExtractor.extract("랫풀다운")).isEqualTo("ㄹㅍㄷㅇ")
    }

    @Test
    fun `공백은 제거된다`() {
        assertThat(ChosungExtractor.extract("인클라인 덤벨프레스"))
            .isEqualTo("ㅇㅋㄹㅇㄷㅂㅍㄹㅅ")
    }

    @Test
    fun `쌍자음 초성을 올바르게 추출한다`() {
        // 딥스(ㄷ), 뻐근(ㅃ), 짐(ㅈ), 짜(ㅉ), 크런치(ㅋ)
        assertThat(ChosungExtractor.extract("딥스")).isEqualTo("ㄷㅅ")
        assertThat(ChosungExtractor.extract("뻐근")).isEqualTo("ㅃㄱ")
        assertThat(ChosungExtractor.extract("짜장")).isEqualTo("ㅉㅈ")
        assertThat(ChosungExtractor.extract("크런치")).isEqualTo("ㅋㄹㅊ")
    }

    @Test
    fun `영문과 숫자는 소문자로 유지된다`() {
        assertThat(ChosungExtractor.extract("Bench Press")).isEqualTo("benchpress")
        assertThat(ChosungExtractor.extract("21s")).isEqualTo("21s")
    }

    @Test
    fun `한글과 영문이 섞여도 처리한다`() {
        assertThat(ChosungExtractor.extract("바벨 Row")).isEqualTo("ㅂㅂrow")
    }

    @Test
    fun `기호는 제거된다`() {
        assertThat(ChosungExtractor.extract("풀-업(와이드)")).isEqualTo("ㅍㅇㅇㅇㄷ")
    }

    @Test
    fun `빈 문자열은 빈 결과`() {
        assertThat(ChosungExtractor.extract("")).isEmpty()
        assertThat(ChosungExtractor.extract("   ")).isEmpty()
    }

    @Test
    fun `초성만으로 구성된 검색어를 판별한다`() {
        assertThat(ChosungExtractor.isChosungOnly("ㅂㅊ")).isTrue()
        assertThat(ChosungExtractor.isChosungOnly("ㅃㄹ")).isTrue()
        assertThat(ChosungExtractor.isChosungOnly("벤치")).isFalse()
        assertThat(ChosungExtractor.isChosungOnly("bench")).isFalse()
        assertThat(ChosungExtractor.isChosungOnly("ㅂ치")).isFalse()
        assertThat(ChosungExtractor.isChosungOnly("")).isFalse()
    }
}
