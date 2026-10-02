package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: Long): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdSync(id: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startTimeMillis DESC LIMIT 1")
    fun getActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveSessionSync(): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET status = :status WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, status: String)

    @Query("UPDATE focus_sessions SET restrictedAttemptsCount = restrictedAttemptsCount + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: RestrictionAttemptEntity): Long

    @Query("SELECT * FROM restriction_attempts WHERE sessionId = :sessionId ORDER BY timestampMillis DESC")
    fun getAttemptsForSession(sessionId: Long): Flow<List<RestrictionAttemptEntity>>

    @Query("SELECT * FROM restriction_attempts ORDER BY timestampMillis DESC")
    fun getAllAttempts(): Flow<List<RestrictionAttemptEntity>>

    @Query("SELECT COUNT(*) FROM restriction_attempts")
    fun getTotalAttemptsCount(): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getTotalCompletedMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getTotalCompletedSessions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'INTERRUPTED'")
    fun getTotalInterruptedSessions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM restriction_attempts")
    suspend fun clearAllAttempts()
}
