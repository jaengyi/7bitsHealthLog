package com.sevenbits.myfit.di

import com.sevenbits.myfit.core.data.repository.AppInitializerImpl
import com.sevenbits.myfit.core.data.repository.ExerciseRepositoryImpl
import com.sevenbits.myfit.core.data.repository.RoutineRepositoryImpl
import com.sevenbits.myfit.core.data.repository.UserRepositoryImpl
import com.sevenbits.myfit.core.data.repository.WorkoutRepositoryImpl
import com.sevenbits.myfit.core.domain.repository.AppInitializer
import com.sevenbits.myfit.core.domain.repository.ExerciseRepository
import com.sevenbits.myfit.core.domain.repository.RestTimerController
import com.sevenbits.myfit.core.domain.repository.RoutineRepository
import com.sevenbits.myfit.core.domain.repository.UserRepository
import com.sevenbits.myfit.core.domain.repository.WorkoutRepository
import com.sevenbits.myfit.timer.RestTimerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 인터페이스 ↔ 구현 바인딩.
 *
 * 이 바인딩은 **:app 에서만** 수행한다. feature 모듈이 :core:data 를 직접 참조하지
 * 못하게 하여 계층 결합을 한 곳에 격리한다. (03_모듈설계서 §1.3 R-2)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindRestTimerController(impl: RestTimerEngine): RestTimerController

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAppInitializer(impl: AppInitializerImpl): AppInitializer
}
