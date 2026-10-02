package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objective: String,
    val durationMinutes: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val allowedPackagesJson: String, // Comma-separated or serialized list of packages
    val allowedAppNames: String,     // Human readable allowed apps summary
    val status: String,              // "ACTIVE", "COMPLETED", "INTERRUPTED"
    val restrictedAttemptsCount: Int = 0,
    val isStrictMode: Boolean = false
)
