package com.sevenbits.myfit.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Test

/**
 * 하단 탭 라우트 검증.
 *
 * Navigation 의 `startDestination` 과 `navigate()` 는 파라미터 타입이 `Any` 라서
 * **무엇을 넣어도 컴파일된다**. 잘못 넣으면 실행 시점에야 죽는다.
 *
 * 실제로 겪은 사고:
 * `WorkoutRoute` 는 `data object` 가 아니라 인자를 받는 `data class` 인데
 * 괄호 없이 `WorkoutRoute` 라고 써서 **컴패니언 객체**가 넘어갔다. 빌드는 통과했고
 * 테스트 106건도 전부 초록이었지만, 실기기에서 첫 화면을 그리는 순간
 * `Serializer for class 'WorkoutRoute$Companion' is not found` 로 앱이 죽었다.
 *
 * 타입 시스템이 막아 주지 못하는 자리이므로 테스트로 막는다.
 */
class TopLevelDestinationTest {

    @Test
    fun `탭 라우트는 컴패니언 객체가 아니다`() {
        TopLevelDestination.entries.forEach { destination ->
            assertThat(destination.route.javaClass.simpleName).isNotEqualTo("Companion")
        }
    }

    @Test
    fun `탭 라우트는 Serializable 이 붙은 타입의 인스턴스다`() {
        // Navigation 타입 안전 라우트는 직렬화로 경로를 만든다.
        // @Serializable 이 없는 객체를 넣으면 이동 시점에 죽는다.
        TopLevelDestination.entries.forEach { destination ->
            val type = destination.route.javaClass
            assertThat(type.isAnnotationPresent(Serializable::class.java)).isTrue()
        }
    }

    @Test
    fun `탭마다 목적지가 서로 다르다`() {
        // 같은 화면을 가리키는 탭이 둘이면 선택 표시가 동시에 켜져 고장으로 보인다.
        val routeTypes = TopLevelDestination.entries.map { it.route.javaClass }
        assertThat(routeTypes).containsNoDuplicates()
    }
}
