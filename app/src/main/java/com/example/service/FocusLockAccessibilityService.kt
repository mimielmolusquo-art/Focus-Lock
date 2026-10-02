package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.ui.blocker.BlockedAppActivity

class FocusLockAccessibilityService : AccessibilityService() {

    private lateinit var sessionManager: FocusSessionManager
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 50
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        // Extract foreground package name
        var packageName = event.packageName?.toString()

        // Fallback to active window root if event.packageName is null or generic 'android'
        if (packageName.isNullOrEmpty() || packageName == "android") {
            try {
                val root = rootInActiveWindow
                val rootPkg = root?.packageName?.toString()
                if (!rootPkg.isNullOrEmpty() && rootPkg != "android") {
                    packageName = rootPkg
                }
            } catch (e: Exception) {
                // Ignore retrieval issues
            }
        }

        if (packageName.isNullOrEmpty()) {
            return
        }

        // 1. Is a focus session currently active?
        if (!sessionManager.isSessionActiveSync()) {
            return
        }

        // 2. Android Safety Whitelist Checks
        if (isSystemSafetyPackage(packageName)) {
            return
        }

        // 3. Is this application in the user's allowed list?
        if (sessionManager.isPackageAllowedSync(packageName)) {
            lastBlockedPackage = null
            return
        }

        // 4. Restricted application detected!
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && (now - lastBlockedTimestamp < 1000L)) {
            // Prevent spamming duplicate intents within 1.0 second
            return
        }

        lastBlockedPackage = packageName
        lastBlockedTimestamp = now

        val appLabel = resolveAppLabel(packageName)
        val objective = sessionManager.getActiveObjectiveSync()
        val endTime = sessionManager.getActiveEndTimeSync()

        // Record the attempt in Room database & session stats
        sessionManager.recordAttempt(packageName, appLabel)

        // Launch the Focus Lock blocking screen
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_LABEL, appLabel)
            putExtra(BlockedAppActivity.EXTRA_OBJECTIVE, objective)
            putExtra(BlockedAppActivity.EXTRA_END_TIME, endTime)
        }
        startActivity(intent)
    }

    private fun isSystemSafetyPackage(packageName: String): Boolean {
        // Our own app
        if (packageName == applicationContext.packageName ||
            packageName == "com.example" ||
            packageName.startsWith("com.aistudio.focuslock")
        ) {
            lastBlockedPackage = null
            return true
        }

        // System UI (notifications, status bar, recents overview, lock screen)
        if (packageName == "com.android.systemui") {
            return true
        }

        // Android Settings & Package Installer (User must never be locked out of phone settings)
        if (packageName == "com.android.settings" ||
            packageName.startsWith("com.android.settings") ||
            packageName.contains("settings") ||
            packageName == "com.google.android.packageinstaller" ||
            packageName == "com.android.packageinstaller" ||
            packageName == "android"
        ) {
            return true
        }

        // Phone calls, Dialer & Emergency functions (Essential Android safety)
        if (packageName.contains("dialer") ||
            packageName.contains("telecom") ||
            packageName.contains("phone") ||
            packageName.contains("incall") ||
            packageName.contains("emergency") ||
            packageName == "com.android.server.telecom"
        ) {
            return true
        }

        // Keyboards / Input Method Editors (IMEs)
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            val imes = imm?.enabledInputMethodList
            if (imes != null) {
                for (ime in imes) {
                    if (ime.packageName == packageName) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Default Launcher / Home screen (So user can navigate their launcher, but tapping a restricted app triggers the block)
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val defaultHome = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (defaultHome?.activityInfo?.packageName == packageName) {
                return true
            }
            val allHomes = packageManager.queryIntentActivities(homeIntent, 0)
            if (allHomes.any { it.activityInfo.packageName == packageName }) {
                return true
            }
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
