package com.example.delivery.data.model

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPriceCents: Long,
)
