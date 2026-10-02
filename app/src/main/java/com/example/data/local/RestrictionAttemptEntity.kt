package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restriction_attempts")
data class RestrictionAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val packageName: String,
    val appLabel: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
