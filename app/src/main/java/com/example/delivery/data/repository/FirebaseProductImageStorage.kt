package com.example.delivery.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage

class FirebaseProductImageStorage(
    private val storage: FirebaseStorage,
) : ProductImageStorage {
    override fun uploadProductImage(productId: String, imageUri: Uri): Task<Unit> =
        storage.reference.child("$PRODUCT_IMAGES_PATH/$productId").putFile(imageUri).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Le téléversement de l'image a échoué.")
            }
            Unit
        }

    override fun downloadUrl(storagePath: String): Task<Uri> =
        storage.reference.child(storagePath).downloadUrl

    override fun deleteImage(storagePath: String): Task<Unit> =
        storage.reference.child(storagePath).delete().continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("La suppression de l'image a échoué.")
            }
            Unit
        }

    private companion object {
        const val PRODUCT_IMAGES_PATH = "product-images"
    }
}
