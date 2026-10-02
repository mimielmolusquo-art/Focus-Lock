package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveSessionState(
    val id: Long,
    val objective: String,
    val durationMinutes: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val allowedPackages: Set<String>,
    val allowedAppNames: String,
    val restrictedAttemptsCount: Int = 0,
    val isFinished: Boolean = false,
    val isStrictMode: Boolean = false
)

class FocusSessionManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_lock_session_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: FocusRepository? = null

    private val _currentSessionState = MutableStateFlow<ActiveSessionState?>(null)
    val currentSessionState: StateFlow<ActiveSessionState?> = _currentSessionState.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<ActiveSessionState?>(null)
    val lastCompletedSession: StateFlow<ActiveSessionState?> = _lastCompletedSession.asStateFlow()

    @Volatile
    private var cachedAllowedPackages: Set<String>? = null

    fun setRepository(repo: FocusRepository) {
        this.repository = repo
        restoreActiveSessionIfAny()
    }

    fun restoreActiveSessionIfAny() {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val now = System.currentTimeMillis()

        if (isActive && endTime > now) {
            val id = prefs.getLong(KEY_SESSION_ID, 0L)
            val objective = prefs.getString(KEY_OBJECTIVE, "") ?: ""
            val duration = prefs.getInt(KEY_DURATION, 25)
            val startTime = prefs.getLong(KEY_START_TIME, now)
            val allowedPkgStr = prefs.getString(KEY_ALLOWED_PACKAGES, "") ?: ""
            val allowedPackages = if (allowedPkgStr.isNotEmpty()) allowedPkgStr.split(",").toSet() else emptySet()
            val allowedAppNames = prefs.getString(KEY_ALLOWED_NAMES, "") ?: ""
            val attempts = prefs.getInt(KEY_ATTEMPTS, 0)
            val isStrict = prefs.getBoolean(KEY_IS_STRICT_MODE, false)

            cachedAllowedPackages = allowedPackages
            val state = ActiveSessionState(
                id = id,
                objective = objective,
                durationMinutes = duration,
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                allowedPackages = allowedPackages,
                allowedAppNames = allowedAppNames,
                restrictedAttemptsCount = attempts,
                isStrictMode = isStrict
            )
            _currentSessionState.value = state

            // Start or re-bind foreground service
            startForegroundService()
        } else if (isActive && endTime <= now) {
            // It expired while app was closed
            cachedAllowedPackages = null
            val id = prefs.getLong(KEY_SESSION_ID, 0L)
            val objective = prefs.getString(KEY_OBJECTIVE, "") ?: ""
            val duration = prefs.getInt(KEY_DURATION, 25)
            val startTime = prefs.getLong(KEY_START_TIME, now)
            val allowedPkgStr = prefs.getString(KEY_ALLOWED_PACKAGES, "") ?: ""
            val allowedPackages = if (allowedPkgStr.isNotEmpty()) allowedPkgStr.split(",").toSet() else emptySet()
            val allowedAppNames = prefs.getString(KEY_ALLOWED_NAMES, "") ?: ""
            val attempts = prefs.getInt(KEY_ATTEMPTS, 0)
            val isStrict = prefs.getBoolean(KEY_IS_STRICT_MODE, false)

            val finishedState = ActiveSessionState(
                id = id,
                objective = objective,
                durationMinutes = duration,
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                allowedPackages = allowedPackages,
                allowedAppNames = allowedAppNames,
                restrictedAttemptsCount = attempts,
                isFinished = true,
                isStrictMode = isStrict
            )
            _lastCompletedSession.value = finishedState
            _currentSessionState.value = null

            prefs.edit().putBoolean(KEY_IS_ACTIVE, false).commit()
            scope.launch {
                repository?.markCompleted(id)
            }
        }
    }

    fun startSession(
        objective: String,
        durationMinutes: Int,
        allowedPackages: Set<String>,
        allowedAppNames: String,
        isStrictMode: Boolean = false,
        onStarted: (Long) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000L)
        val tempSessionId = now
        cachedAllowedPackages = allowedPackages

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, true)
            .putLong(KEY_SESSION_ID, tempSessionId)
            .putString(KEY_OBJECTIVE, objective)
            .putInt(KEY_DURATION, durationMinutes)
            .putLong(KEY_START_TIME, now)
            .putLong(KEY_END_TIME, endTime)
            .putString(KEY_ALLOWED_PACKAGES, allowedPackages.joinToString(","))
            .putString(KEY_ALLOWED_NAMES, allowedAppNames)
            .putInt(KEY_ATTEMPTS, 0)
            .putBoolean(KEY_IS_STRICT_MODE, isStrictMode)
            .commit()

        val state = ActiveSessionState(
            id = tempSessionId,
            objective = objective,
            durationMinutes = durationMinutes,
            startTimeMillis = now,
            endTimeMillis = endTime,
            allowedPackages = allowedPackages,
            allowedAppNames = allowedAppNames,
            restrictedAttemptsCount = 0,
            isStrictMode = isStrictMode
        )
        _currentSessionState.value = state
        startForegroundService()

        scope.launch {
            val sessionId = repository?.createSession(
                objective = objective,
                durationMinutes = durationMinutes,
                startTimeMillis = now,
                endTimeMillis = endTime,
                allowedPackages = allowedPackages,
                allowedAppNames = allowedAppNames,
                isStrictMode = isStrictMode
            ) ?: tempSessionId

            if (sessionId != tempSessionId) {
                prefs.edit().putLong(KEY_SESSION_ID, sessionId).commit()
                _currentSessionState.value = _currentSessionState.value?.copy(id = sessionId)
            }
            onStarted(sessionId)
        }
    }

    fun completeSession() {
        val current = _currentSessionState.value ?: return
        val finished = current.copy(isFinished = true)
        cachedAllowedPackages = null
        _lastCompletedSession.value = finished
        _currentSessionState.value = null

        prefs.edit().putBoolean(KEY_IS_ACTIVE, false).commit()
        stopForegroundService()

        scope.launch {
            repository?.markCompleted(current.id)
        }
    }

    fun interruptSession() {
        val current = _currentSessionState.value ?: return
        cachedAllowedPackages = null
        _currentSessionState.value = null

        prefs.edit().putBoolean(KEY_IS_ACTIVE, false).commit()
        stopForegroundService()

        scope.launch {
            repository?.markInterrupted(current.id)
        }
    }

    fun clearCompletedSession() {
        _lastCompletedSession.value = null
    }

    fun recordAttempt(packageName: String, appLabel: String) {
        val current = _currentSessionState.value ?: return
        val newCount = current.restrictedAttemptsCount + 1
        _currentSessionState.value = current.copy(restrictedAttemptsCount = newCount)

        prefs.edit().putInt(KEY_ATTEMPTS, newCount).commit()

        scope.launch {
            repository?.recordAttempt(current.id, packageName, appLabel)
        }
    }

    /**
     * In Strict Mode, modifications to allowed apps during an active session are forbidden.
     */
    fun canModifyAllowedPackages(): Boolean {
        val current = _currentSessionState.value ?: return true
        return !current.isStrictMode
    }

    fun modifyAllowedPackages(newAllowedPackages: Set<String>, newAllowedNames: String): Boolean {
        if (!canModifyAllowedPackages()) {
            return false
        }
        val current = _currentSessionState.value ?: return false
        cachedAllowedPackages = newAllowedPackages
        _currentSessionState.value = current.copy(
            allowedPackages = newAllowedPackages,
            allowedAppNames = newAllowedNames
        )
        prefs.edit()
            .putString(KEY_ALLOWED_PACKAGES, newAllowedPackages.joinToString(","))
            .putString(KEY_ALLOWED_NAMES, newAllowedNames)
            .commit()
        return true
    }

    // Fast synchronous checks for AccessibilityService
    fun isSessionActiveSync(): Boolean {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) return false
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        return System.currentTimeMillis() < endTime
    }

    fun isStrictModeSync(): Boolean {
        return prefs.getBoolean(KEY_IS_STRICT_MODE, false)
    }

    fun isPackageAllowedSync(packageName: String): Boolean {
        val cache = cachedAllowedPackages
        if (cache != null) {
            return cache.contains(packageName)
        }
        val allowedPkgStr = prefs.getString(KEY_ALLOWED_PACKAGES, "") ?: ""
        if (allowedPkgStr.isEmpty()) return false
        val set = allowedPkgStr.split(",").toSet()
        cachedAllowedPackages = set
        return set.contains(packageName)
    }

    fun getActiveObjectiveSync(): String {
        return prefs.getString(KEY_OBJECTIVE, "") ?: ""
    }

    fun getActiveEndTimeSync(): Long {
        return prefs.getLong(KEY_END_TIME, 0L)
    }

    fun getDefaultStrictMode(): Boolean {
        return prefs.getBoolean(KEY_DEFAULT_STRICT_MODE, false)
    }

    fun setDefaultStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_STRICT_MODE, enabled).commit()
    }

    private fun startForegroundService() {
        val intent = Intent(context, FocusSessionService::class.java).apply {
            action = FocusSessionService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(context, FocusSessionService::class.java).apply {
            action = FocusSessionService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_OBJECTIVE = "objective"
        private const val KEY_DURATION = "duration"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
        private const val KEY_ALLOWED_NAMES = "allowed_names"
        private const val KEY_ATTEMPTS = "attempts"
        private const val KEY_IS_STRICT_MODE = "is_strict_mode"
        private const val KEY_DEFAULT_STRICT_MODE = "default_strict_mode"

        @Volatile
        private var INSTANCE: FocusSessionManager? = null

        fun getInstance(context: Context): FocusSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FocusSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
