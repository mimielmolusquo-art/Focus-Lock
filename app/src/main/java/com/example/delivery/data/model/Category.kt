package com.example.delivery.data.model

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val requiredAccessId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
