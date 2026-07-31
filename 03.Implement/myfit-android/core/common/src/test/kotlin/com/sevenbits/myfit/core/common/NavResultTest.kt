package com.sevenbits.myfit.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 화면 간 결과 전달 인코딩 검증.
 *
 * 이 왕복이 깨지면 일지에 종목을 추가할 수 없다. 실기기 없이 검증 가능한 지점이므로
 * 테스트로 고정한다.
 */
class NavResultTest {

    @Test
    fun `종목 ID 목록을 인코딩하고 되돌린다`() {
        val ids = listOf("ex-0001", "ex-0042", "ex-0219")

        val decoded = NavResult.decodeIds(NavResult.encodeIds(ids))

        assertThat(decoded).containsExactlyElementsIn(ids).inOrder()
    }

    @Test
    fun `단일 종목도 처리한다`() {
        assertThat(NavResult.decodeIds(NavResult.encodeIds(listOf("ex-0001"))))
            .containsExactly("ex-0001")
    }

    @Test
    fun `빈 문자열은 빈 목록으로 해석된다`() {
        // 결과 소비 후 키를 빈 문자열로 비우므로, 이 경우가 재추가를 유발하면 안 된다
        assertThat(NavResult.decodeIds("")).isEmpty()
    }

    @Test
    fun `공백만 있는 항목은 무시된다`() {
        assertThat(NavResult.decodeIds("ex-0001, ,ex-0002"))
            .containsExactly("ex-0001", "ex-0002").inOrder()
    }

    @Test
    fun `사용자 정의 종목의 UUID 형식도 안전하다`() {
        // 구분자(,)가 포함되지 않는 것이 전제다
        val ids = listOf(
            "3f2a1b4c-5d6e-7f80-9a1b-2c3d4e5f6071",
            "77c1a2b3-c4d5-e6f7-8091-a2b3c4d5e6f7",
        )

        assertThat(NavResult.decodeIds(NavResult.encodeIds(ids)))
            .containsExactlyElementsIn(ids).inOrder()
    }

    @Test
    fun `빈 목록을 인코딩하면 빈 문자열이 된다`() {
        assertThat(NavResult.encodeIds(emptyList())).isEmpty()
    }
}
