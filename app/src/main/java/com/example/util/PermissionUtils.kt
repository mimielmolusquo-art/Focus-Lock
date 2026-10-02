package com.example.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.example.service.FocusLockAccessibilityService

object PermissionUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Direct real-time check: If the service instance is currently bound and active
        if (FocusLockAccessibilityService.isRunning) {
            return true
        }

        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

        // Check 1: via AccessibilityManager enabled service list
        val enabledServices = accessibilityManager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices != null) {
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
                val pkg = serviceInfo.packageName
                val name = serviceInfo.name
                val pkgMatch = pkg == context.packageName || pkg == "com.example" || pkg.contains("focuslock")
                val classMatch = name.contains("FocusLockAccessibilityService")
                if (pkgMatch && classMatch) {
                    return true
                }
            }
        }

        // Check 2: via Settings.Secure string check
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = enabledServicesSetting.split(":")
            for (componentString in colonSplitter) {
                if (componentString.isBlank()) continue

                val component = ComponentName.unflattenFromString(componentString.trim())
                if (component != null) {
                    val pkgMatch = component.packageName == context.packageName ||
                            component.packageName == "com.example" ||
                            component.packageName.contains("focuslock")
                    val classMatch = component.className.contains("FocusLockAccessibilityService")
                    if (pkgMatch && classMatch) {
                        return true
                    }
                }

                if (componentString.contains("FocusLockAccessibilityService") &&
                    (componentString.contains(context.packageName) || componentString.contains("com.example") || componentString.contains("focuslock"))
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Error reading secure settings
        }

        return false
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
