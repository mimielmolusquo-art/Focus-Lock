package com.example.data.repository

import com.example.data.local.FocusSessionDao
import com.example.data.local.FocusSessionEntity
import com.example.data.local.RestrictionAttemptEntity
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val focusSessionDao: FocusSessionDao) {

    val allSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()
    val activeSession: Flow<FocusSessionEntity?> = focusSessionDao.getActiveSession()
    val totalAttempts: Flow<Int> = focusSessionDao.getTotalAttemptsCount()
    val totalCompletedMinutes: Flow<Int?> = focusSessionDao.getTotalCompletedMinutes()
    val totalCompletedSessions: Flow<Int> = focusSessionDao.getTotalCompletedSessions()
    val totalInterruptedSessions: Flow<Int> = focusSessionDao.getTotalInterruptedSessions()
    val totalSessionsCount: Flow<Int> = focusSessionDao.getTotalSessionsCount()
    val allAttempts: Flow<List<RestrictionAttemptEntity>> = focusSessionDao.getAllAttempts()

    suspend fun getActiveSessionSync(): FocusSessionEntity? {
        return focusSessionDao.getActiveSessionSync()
    }

    suspend fun getSessionByIdSync(id: Long): FocusSessionEntity? {
        return focusSessionDao.getSessionByIdSync(id)
    }

    fun getAttemptsForSession(sessionId: Long): Flow<List<RestrictionAttemptEntity>> {
        return focusSessionDao.getAttemptsForSession(sessionId)
    }

    suspend fun createSession(
        objective: String,
        durationMinutes: Int,
        startTimeMillis: Long,
        endTimeMillis: Long,
        allowedPackages: Set<String>,
        allowedAppNames: String,
        isStrictMode: Boolean = false
    ): Long {
        val packagesString = allowedPackages.joinToString(",")
        val entity = FocusSessionEntity(
            objective = objective,
            durationMinutes = durationMinutes,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            allowedPackagesJson = packagesString,
            allowedAppNames = allowedAppNames,
            status = "ACTIVE",
            restrictedAttemptsCount = 0,
            isStrictMode = isStrictMode
        )
        return focusSessionDao.insertSession(entity)
    }

    suspend fun markCompleted(sessionId: Long) {
        focusSessionDao.updateSessionStatus(sessionId, "COMPLETED")
    }

    suspend fun markInterrupted(sessionId: Long) {
        focusSessionDao.updateSessionStatus(sessionId, "INTERRUPTED")
    }

    suspend fun recordAttempt(sessionId: Long, packageName: String, appLabel: String) {
        focusSessionDao.incrementAttempts(sessionId)
        focusSessionDao.insertAttempt(
            RestrictionAttemptEntity(
                sessionId = sessionId,
                packageName = packageName,
                appLabel = appLabel,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllHistory() {
        focusSessionDao.clearAllAttempts()
        focusSessionDao.clearAllSessions()
    }
}
