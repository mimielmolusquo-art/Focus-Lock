package com.example.delivery.data.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val categoryId: String,
    val imagePlaceholder: String,
    val featured: Boolean = false,
)
