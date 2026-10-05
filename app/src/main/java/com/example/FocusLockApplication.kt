package com.example

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.local.FocusLockDatabase
import com.example.data.repository.FocusRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.service.FocusSessionManager
import com.example.delivery.data.repository.FirebaseAuthenticationRepository
import com.example.delivery.data.repository.ClientAuthenticationRepository
import com.example.delivery.data.repository.FirebaseOrderRepository
import com.example.delivery.data.repository.FirebaseProductRepository

class FocusLockApplication : Application() {

    lateinit var database: FocusLockDatabase
        private set

    lateinit var focusRepository: FocusRepository
        private set

    lateinit var installedAppsRepository: InstalledAppsRepository
        private set

    lateinit var sessionManager: FocusSessionManager
        private set

    lateinit var authRepository: ClientAuthenticationRepository
        private set

    lateinit var productRepository: FirebaseProductRepository
        private set

    lateinit var orderRepository: FirebaseOrderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = FocusLockDatabase.getDatabase(this)
        focusRepository = FocusRepository(database.focusSessionDao())
        installedAppsRepository = InstalledAppsRepository(this)
        sessionManager = FocusSessionManager.getInstance(this).apply {
            setRepository(focusRepository)
        }
        val firestore = FirebaseFirestore.getInstance()
        productRepository = FirebaseProductRepository(firestore)
        orderRepository = FirebaseOrderRepository(firestore)
        authRepository = FirebaseAuthenticationRepository(FirebaseAuth.getInstance())
        authRepository.signInAnonymously()
    }
}
