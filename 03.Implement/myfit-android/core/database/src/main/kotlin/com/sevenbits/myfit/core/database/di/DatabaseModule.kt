package com.sevenbits.myfit.core.database.di

import android.content.Context
import androidx.room.Room
import com.sevenbits.myfit.core.database.MyFitDatabase
import com.sevenbits.myfit.core.database.dao.ExerciseDao
import com.sevenbits.myfit.core.database.dao.ExerciseNoteDao
import com.sevenbits.myfit.core.database.dao.ExerciseReferenceDao
import com.sevenbits.myfit.core.database.dao.RoutineDao
import com.sevenbits.myfit.core.database.dao.TombstoneDao
import com.sevenbits.myfit.core.database.dao.UserDao
import com.sevenbits.myfit.core.database.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyFitDatabase =
        Room.databaseBuilder(context, MyFitDatabase::class.java, MyFitDatabase.NAME)
            // 최초 실행 시 네트워크 없이 종목 마스터를 사용할 수 있어야 한다 (REQ-EXR-001)
            // .createFromAsset("database/myfit_seed.db")
            // fallbackToDestructiveMigration 은 사용하지 않는다 — 기록 유실 방지
            .build()

    @Provides
    fun provideExerciseDao(database: MyFitDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideExerciseNoteDao(database: MyFitDatabase): ExerciseNoteDao = database.exerciseNoteDao()

    @Provides
    fun provideExerciseReferenceDao(database: MyFitDatabase): ExerciseReferenceDao =
        database.exerciseReferenceDao()

    @Provides
    fun provideWorkoutDao(database: MyFitDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideRoutineDao(database: MyFitDatabase): RoutineDao = database.routineDao()

    @Provides
    fun provideTombstoneDao(database: MyFitDatabase): TombstoneDao = database.tombstoneDao()

    @Provides
    fun provideUserDao(database: MyFitDatabase): UserDao = database.userDao()
}
