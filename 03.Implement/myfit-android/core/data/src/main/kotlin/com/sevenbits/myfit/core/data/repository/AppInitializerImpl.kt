package com.sevenbits.myfit.core.data.repository

import com.sevenbits.myfit.core.database.DatabaseInitializer
import com.sevenbits.myfit.core.domain.repository.AppInitializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializerImpl @Inject constructor(
    private val databaseInitializer: DatabaseInitializer,
) : AppInitializer {

    override suspend fun initialize(): Int = databaseInitializer.initialize()
}
