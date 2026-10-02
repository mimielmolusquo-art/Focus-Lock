package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FocusLockDatabase
import com.example.data.repository.FocusRepository
import com.example.service.FocusSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusLockRestrictionTest {

    private lateinit var context: Context
    private lateinit var database: FocusLockDatabase
    private lateinit var repository: FocusRepository
    private lateinit var sessionManager: FocusSessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FocusLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FocusRepository(database.focusSessionDao())
        sessionManager = FocusSessionManager.getInstance(context).apply {
            setRepository(repository)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test complete focus session and restriction flow`() = runBlocking {
        // 1. Start a 25-minute focus session with Chrome allowed
        val objective = "Finish my project"
        val durationMinutes = 25
        val allowedPackages = setOf("com.android.chrome")
        val allowedNames = "Google Chrome"

        sessionManager.startSession(
            objective = objective,
            durationMinutes = durationMinutes,
            allowedPackages = allowedPackages,
            allowedAppNames = allowedNames
        )

        // 2. Verify session is active
        assertTrue("Session should be active", sessionManager.isSessionActiveSync())
        assertEquals(objective, sessionManager.getActiveObjectiveSync())

        // 3. Verify Chrome is allowed
        assertTrue(
            "Chrome should be allowed",
            sessionManager.isPackageAllowedSync("com.android.chrome")
        )

        // 4. Verify Instagram is NOT allowed
        val instagramPackage = "com.instagram.android"
        assertFalse(
            "Instagram should NOT be allowed",
            sessionManager.isPackageAllowedSync(instagramPackage)
        )

        // 5. Simulate opening Instagram -> record attempt
        sessionManager.recordAttempt(instagramPackage, "Instagram")

        // 6. Verify state reflects blocked attempt
        val activeState = sessionManager.currentSessionState.value
        assertNotNull(activeState)
        assertEquals(1, activeState?.restrictedAttemptsCount)

        // 7. Allow IO coroutines to persist to Room
        kotlinx.coroutines.delay(300)

        val attemptsCount = repository.totalAttempts.first()
        assertEquals(1, attemptsCount)

        val attemptsList = repository.getAttemptsForSession(activeState!!.id).first()
        assertEquals(1, attemptsList.size)
        assertEquals(instagramPackage, attemptsList[0].packageName)
        assertEquals("Instagram", attemptsList[0].appLabel)

        // 8. Complete session and verify transition
        sessionManager.completeSession()
        assertFalse("Session should no longer be active", sessionManager.isSessionActiveSync())

        val completedState = sessionManager.lastCompletedSession.value
        assertNotNull(completedState)
        assertTrue(completedState!!.isFinished)
        assertEquals(1, completedState.restrictedAttemptsCount)
    }
}
