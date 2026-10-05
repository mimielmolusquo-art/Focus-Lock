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
    val userId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH_ON_DELIVERY,
    val statusCode: OrderStatus = OrderStatus.NEW,
    val updatedAt: Long = createdAtEpochMillis,
) {
    val deliveryAddress: String
        get() = address

    val createdAt: Long
        get() = createdAtEpochMillis
}

enum class PaymentMethod {
    CASH_ON_DELIVERY,
}

enum class OrderStatus {
    NEW,
    ACCEPTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    COMPLETED,
    CANCELLED,
    REFUSED,
}
