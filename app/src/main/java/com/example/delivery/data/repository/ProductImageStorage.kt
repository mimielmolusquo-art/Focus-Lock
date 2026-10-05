package com.example.delivery.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task

interface ProductImageStorage {
    fun uploadProductImage(productId: String, imageUri: Uri): Task<Unit>
    fun downloadUrl(storagePath: String): Task<Uri>
    fun deleteImage(storagePath: String): Task<Unit>
}
