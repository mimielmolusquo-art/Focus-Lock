package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    private var cachedApps: List<AppInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedApps != null) {
            return@withContext cachedApps!!
        }

        val packageManager = context.packageManager
        val myPackageName = context.packageName

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            packageManager.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackageName || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            val label = try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                pkg
            }

            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            val isSystem = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }

            appList.add(
                AppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isAllowed = false,
                    isSystem = isSystem
                )
            )
        }

        // If launcher query returned very few apps (e.g. strict emulator sandbox), also check getInstalledApplications
        if (appList.isEmpty()) {
            val installedApps = try {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }

            for (app in installedApps) {
                if (app.packageName == myPackageName || seenPackages.contains(app.packageName)) {
                    continue
                }
                seenPackages.add(app.packageName)
                val label = try {
                    packageManager.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                val icon = try {
                    packageManager.getApplicationIcon(app)
                } catch (e: Exception) {
                    null
                }
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                appList.add(
                    AppInfo(
                        packageName = app.packageName,
                        appName = label,
                        icon = icon,
                        isAllowed = false,
                        isSystem = isSystem
                    )
                )
            }
        }

        val sortedList = appList.sortedWith(
            compareBy<AppInfo> { it.isSystem }
                .thenBy { it.appName.lowercase() }
        )

        cachedApps = sortedList
        sortedList
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
