package com.example.delivery.data.repository

import com.google.android.gms.tasks.Task

interface ClientAuthenticationRepository {
    fun signInAnonymously(): Task<String>
    fun currentUserId(): String?
}

interface AdminAuthorizationRepository {
    fun isCurrentUserAdmin(): Task<Boolean>
}
