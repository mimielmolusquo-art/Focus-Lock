package com.example.delivery.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth,
) : ClientAuthenticationRepository, AdminAuthorizationRepository {
    override fun signInAnonymously(): Task<String> =
        auth.signInAnonymously().continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("L'authentification anonyme a échoué.")
            }
            task.result?.user?.uid
                ?: throw IllegalStateException("Firebase Authentication n'a retourné aucun utilisateur.")
        }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun isCurrentUserAdmin(): Task<Boolean> {
        val user = auth.currentUser ?: return Tasks.forResult(false)
        return user.getIdToken(false).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("La vérification du rôle administrateur a échoué.")
            }
            task.result?.claims?.get(ADMIN_CLAIM) == true
        }
    }

    private companion object {
        const val ADMIN_CLAIM = "admin"
    }
}
