package com.example.delivery.data.model

data class Order(
    val id: String,
    val address: String,
    val items: List<OrderItem>,
    val subtotalCents: Long,
    val deliveryFeeCents: Long,
    val totalCents: Long,
    val status: String = "Commande reçue",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
