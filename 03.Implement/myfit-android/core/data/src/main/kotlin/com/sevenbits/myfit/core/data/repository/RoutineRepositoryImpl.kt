package com.sevenbits.myfit.core.data.repository

import com.sevenbits.myfit.core.database.RoutineLocalSource
import com.sevenbits.myfit.core.domain.model.Routine
import com.sevenbits.myfit.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val local: RoutineLocalSource,
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> = local.observeRoutines()

    override suspend fun findRoutine(routineId: String): Routine? = local.findRoutine(routineId)

    override suspend fun upsertRoutine(routine: Routine): String = local.upsertRoutine(routine)

    override suspend fun deactivateRoutine(routineId: String) = local.deactivateRoutine(routineId)

    override suspend fun instantiateToLog(routineId: String, date: String, sessionNo: Int): String =
        local.instantiateToLog(routineId, date, sessionNo)

    override suspend fun saveLogAsRoutine(logId: String, routineName: String): String =
        local.saveLogAsRoutine(logId, routineName)

    override suspend fun suggestNextRoutine(): Routine? = local.suggestNextRoutine()
}
