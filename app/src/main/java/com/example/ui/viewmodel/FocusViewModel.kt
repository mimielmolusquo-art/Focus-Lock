package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.local.FocusSessionEntity
import com.example.data.model.AppInfo
import com.example.data.repository.FocusRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.service.ActiveSessionState
import com.example.service.FocusSessionManager
import com.example.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Screen {
    ONBOARDING,
    HOME,
    SESSION_CREATION,
    ACTIVE_SESSION,
    COMPLETION,
    HISTORY,
    STATISTICS,
    SETTINGS,
    PERMISSIONS
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusLockApplication
    private val focusRepository: FocusRepository = app.focusRepository
    private val installedAppsRepository: InstalledAppsRepository = app.installedAppsRepository
    val sessionManager: FocusSessionManager = app.sessionManager

    private val userPrefs: SharedPreferences =
        application.getSharedPreferences("focus_lock_user_prefs", Context.MODE_PRIVATE)

    // Onboarding & Settings
    val hasSeenOnboarding = MutableStateFlow(userPrefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false))
    val themeMode = MutableStateFlow(userPrefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val defaultStrictMode = MutableStateFlow(sessionManager.getDefaultStrictMode())

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow(
        if (!userPrefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)) Screen.ONBOARDING else Screen.HOME
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Screen navigation stack to support back navigation cleanly
    private val screenStack = mutableListOf<Screen>()

    // Form inputs for Session Creation
    val objective = MutableStateFlow("")
    val selectedDurationMinutes = MutableStateFlow(25)
    val customDurationInput = MutableStateFlow("30")
    val showCustomDurationDialog = MutableStateFlow(false)
    val isStrictModeForSession = MutableStateFlow(sessionManager.getDefaultStrictMode())

    // Apps Selection
    private val _allInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allInstalledApps: StateFlow<List<AppInfo>> = _allInstalledApps.asStateFlow()

    val appSearchQuery = MutableStateFlow("")
    val allowedPackages = MutableStateFlow<Set<String>>(emptySet())
    val isLoadingApps = MutableStateFlow(false)

    // Active session and completed session from sessionManager
    val activeSessionState: StateFlow<ActiveSessionState?> = sessionManager.currentSessionState
    val lastCompletedSession: StateFlow<ActiveSessionState?> = sessionManager.lastCompletedSession

    // Permissions State
    val isAccessibilityEnabled = MutableStateFlow(false)
    val isNotificationEnabled = MutableStateFlow(false)
    val canDrawOverlays = MutableStateFlow(false)

    // History and Stats from Room
    val pastSessions: StateFlow<List<FocusSessionEntity>> = focusRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCompletedMinutes: StateFlow<Int> = focusRepository.totalCompletedMinutes
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCompletedSessions: StateFlow<Int> = focusRepository.totalCompletedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalInterruptedSessions: StateFlow<Int> = focusRepository.totalInterruptedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBlockedAttempts: StateFlow<Int> = focusRepository.totalAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current streak (consecutive days of completed sessions)
    val currentStreak: StateFlow<Int> = focusRepository.allSessions.map { sessions ->
        computeCurrentStreak(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refreshPermissions()
        loadInstalledApps()

        // If an active session is currently running on launch, navigate straight to active session screen
        viewModelScope.launch {
            sessionManager.currentSessionState.collect { state ->
                if (state != null && !state.isFinished && _currentScreen.value != Screen.ACTIVE_SESSION) {
                    _currentScreen.value = Screen.ACTIVE_SESSION
                }
            }
        }

        // If session finished, navigate to completion screen
        viewModelScope.launch {
            sessionManager.lastCompletedSession.collect { completed ->
                if (completed != null && _currentScreen.value != Screen.COMPLETION) {
                    _currentScreen.value = Screen.COMPLETION
                }
            }
        }
    }

    fun completeOnboarding() {
        userPrefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
        hasSeenOnboarding.value = true
        _currentScreen.value = Screen.HOME
        screenStack.clear()
    }

    fun setThemeMode(mode: String) {
        userPrefs.edit().putString(KEY_THEME_MODE, mode).apply()
        themeMode.value = mode
    }

    fun setDefaultStrictMode(enabled: Boolean) {
        sessionManager.setDefaultStrictMode(enabled)
        defaultStrictMode.value = enabled
        isStrictModeForSession.value = enabled
    }

    fun refreshPermissions() {
        isAccessibilityEnabled.value = PermissionUtils.isAccessibilityServiceEnabled(getApplication())
        isNotificationEnabled.value = PermissionUtils.isNotificationPermissionGranted(getApplication())
        canDrawOverlays.value = PermissionUtils.canDrawOverlays(getApplication())
    }

    fun loadInstalledApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            isLoadingApps.value = true
            val apps = installedAppsRepository.getInstalledApps(forceRefresh)
            _allInstalledApps.value = apps
            isLoadingApps.value = false
        }
    }

    fun setObjective(text: String) {
        objective.value = text
    }

    fun setDuration(minutes: Int) {
        selectedDurationMinutes.value = minutes
    }

    fun toggleAppAllowed(packageName: String) {
        val current = allowedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        allowedPackages.value = current
    }

    fun setAllAppsAllowed(allowed: Boolean) {
        if (allowed) {
            allowedPackages.value = _allInstalledApps.value.map { it.packageName }.toSet()
        } else {
            allowedPackages.value = emptySet()
        }
    }

    fun allowEssentialPreset() {
        val essentials = _allInstalledApps.value.filter {
            val name = it.appName.lowercase()
            val pkg = it.packageName.lowercase()
            name.contains("calc") || name.contains("notes") || name.contains("horloge") ||
                    name.contains("clock") || pkg.contains("dialer") || pkg.contains("phone")
        }.map { it.packageName }.toSet()
        allowedPackages.value = essentials
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    fun startFocusSession() {
        val obj = objective.value.trim().ifEmpty { "Session de travail profond" }
        val duration = selectedDurationMinutes.value
        val allowed = allowedPackages.value
        val isStrict = isStrictModeForSession.value

        val allowedNames = _allInstalledApps.value
            .filter { allowed.contains(it.packageName) }
            .joinToString(", ") { it.appName }

        sessionManager.startSession(
            objective = obj,
            durationMinutes = duration,
            allowedPackages = allowed,
            allowedAppNames = allowedNames,
            isStrictMode = isStrict
        ) {
            _currentScreen.value = Screen.ACTIVE_SESSION
            screenStack.clear()
        }
    }

    fun interruptCurrentSession() {
        sessionManager.interruptSession()
        _currentScreen.value = Screen.HOME
        screenStack.clear()
    }

    fun finishSessionDone() {
        sessionManager.clearCompletedSession()
        _currentScreen.value = Screen.HOME
        screenStack.clear()
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            focusRepository.clearAllHistory()
        }
    }

    private fun computeCurrentStreak(sessions: List<FocusSessionEntity>): Int {
        val completedDates = sessions
            .filter { it.status == "COMPLETED" }
            .map { s ->
                val cal = Calendar.getInstance().apply { timeInMillis = s.startTimeMillis }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(cal.time)
            }
            .toSet()

        if (completedDates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        var currentCheckCal = Calendar.getInstance()
        var streak = 0

        if (completedDates.contains(todayStr)) {
            // Started streak today
            streak = 1
            currentCheckCal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (completedDates.contains(yesterdayStr)) {
            // Streak alive from yesterday
            streak = 1
            currentCheckCal.time = cal.time
            currentCheckCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            return 0
        }

        while (true) {
            val dateStr = sdf.format(currentCheckCal.time)
            if (completedDates.contains(dateStr)) {
                streak++
                currentCheckCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    companion object {
        private const val KEY_HAS_SEEN_ONBOARDING = "pref_has_seen_onboarding"
        private const val KEY_THEME_MODE = "pref_theme_mode"
    }
}
