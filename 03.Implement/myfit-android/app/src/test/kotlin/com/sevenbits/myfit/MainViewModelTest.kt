package com.sevenbits.myfit

import com.google.common.truth.Truth.assertThat
import com.sevenbits.myfit.core.domain.model.UserProfile
import com.sevenbits.myfit.core.domain.model.UserSetting
import com.sevenbits.myfit.core.domain.repository.AppInitializer
import com.sevenbits.myfit.core.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * 앱 진입 시점 동작 검증.
 *
 * 실제로 겪은 사고: [com.sevenbits.myfit.core.domain.repository.AppInitializer] 에 해당하는
 * 시드 적재 코드는 만들어져 있었고 단위 테스트도 통과했지만, **앱 시작 시점에 호출하는
 * 코드가 없었다.** 시더 테스트는 시더를 직접 부르므로 이 누락을 잡지 못했다.
 * 실기기에서 종목 목록이 통째로 비어서야 드러났다.
 *
 * "만들어졌는가"가 아니라 "연결되었는가"를 확인한다.
 */
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `앱 시작 시 시드 초기화를 호출한다`() = runTest(dispatcher) {
        val initializer = RecordingInitializer()
        val viewModel = MainViewModel(FakeUserRepository(), initializer)

        // init 블록의 코루틴을 흘려보낸다
        testScheduler.advanceUntilIdle()

        assertThat(initializer.callCount).isEqualTo(1)
        // 초기화가 끝나야 첫 화면이 정해진다
        assertThat(viewModel.startState.value).isNotEqualTo(StartState.Loading)
    }

    @Test
    fun `초기화가 끝나기 전에는 첫 화면을 정하지 않는다`() = runTest(dispatcher) {
        // 초기화가 진행 중인 동안 화면을 그리면 빈 DB 를 읽게 된다
        val initializer = RecordingInitializer()
        val viewModel = MainViewModel(FakeUserRepository(), initializer)

        assertThat(viewModel.startState.value).isEqualTo(StartState.Loading)
        assertThat(initializer.callCount).isEqualTo(0)
    }

    @Test
    fun `온보딩을 마치지 않았으면 온보딩으로 시작한다`() = runTest(dispatcher) {
        val viewModel = MainViewModel(
            FakeUserRepository(UserSetting(onboardingCompleted = false)),
            RecordingInitializer(),
        )
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.startState.value).isEqualTo(StartState.Onboarding)
    }

    @Test
    fun `온보딩을 마쳤으면 본 화면으로 시작한다`() = runTest(dispatcher) {
        val viewModel = MainViewModel(
            FakeUserRepository(UserSetting(onboardingCompleted = true)),
            RecordingInitializer(),
        )
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.startState.value).isEqualTo(StartState.Main)
    }

    private class RecordingInitializer : AppInitializer {
        var callCount = 0
            private set

        override suspend fun initialize(): Int {
            callCount++
            return 219
        }
    }

    private class FakeUserRepository(
        private val setting: UserSetting = UserSetting(),
    ) : UserRepository {
        override fun observeProfile(): Flow<UserProfile?> = flowOf(null)
        override suspend fun findProfile(): UserProfile = UserProfile()
        override suspend fun upsertProfile(profile: UserProfile) = Unit
        override fun observeSetting(): Flow<UserSetting> = flowOf(setting)
        override suspend fun findSetting(): UserSetting = setting
        override suspend fun upsertSetting(setting: UserSetting) = Unit
        override suspend fun markOnboardingCompleted() = Unit
    }
}
