package com.example.delivery.data.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val categoryId: String,
    val imagePlaceholder: String,
    val featured: Boolean = false,
    val imageUrl: String? = null,
    val storagePath: String? = null,
    val isActive: Boolean = true,
    val isPrivate: Boolean = false,
    val accessId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val categoryName: String? = null,
    val variantLabel: String? = null,
)
