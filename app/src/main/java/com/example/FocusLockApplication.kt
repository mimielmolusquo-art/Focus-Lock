package com.example

import android.app.Application
import com.example.data.local.FocusLockDatabase
import com.example.data.repository.FocusRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.service.FocusSessionManager

class FocusLockApplication : Application() {

    lateinit var database: FocusLockDatabase
        private set

    lateinit var focusRepository: FocusRepository
        private set

    lateinit var installedAppsRepository: InstalledAppsRepository
        private set

    lateinit var sessionManager: FocusSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = FocusLockDatabase.getDatabase(this)
        focusRepository = FocusRepository(database.focusSessionDao())
        installedAppsRepository = InstalledAppsRepository(this)
        sessionManager = FocusSessionManager.getInstance(this).apply {
            setRepository(focusRepository)
        }
    }
}
