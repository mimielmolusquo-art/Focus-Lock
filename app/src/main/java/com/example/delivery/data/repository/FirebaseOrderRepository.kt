package com.example.delivery.data.repository

import com.example.delivery.data.model.Order
import com.example.delivery.data.model.OrderItem
import com.example.delivery.data.model.OrderStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseOrderRepository(
    private val firestore: FirebaseFirestore,
) : SharedOrderRepository {
    override fun submitOrder(order: Order): Task<Unit> =
        firestore.collection(ORDERS_COLLECTION)
            .document(order.id)
            .set(order.toFirestoreMap())
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("L'enregistrement de la commande a échoué.")
                }
                Unit
            }

    override fun fetchCustomerOrders(userId: String): Task<List<Order>> =
        firestore.collection(ORDERS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .continueWith { task ->
                task.requireSuccess().documents
                    .map(DocumentSnapshot::toOrder)
                    .sortedByDescending(Order::createdAtEpochMillis)
            }

    override fun fetchAllOrdersForAdmin(): Task<List<Order>> =
        firestore.collection(ORDERS_COLLECTION).get().continueWith { task ->
            task.requireSuccess().documents
                .map(DocumentSnapshot::toOrder)
                .sortedByDescending(Order::createdAtEpochMillis)
        }

    override fun updateOrderStatus(orderId: String, status: OrderStatus): Task<Unit> =
        firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("La mise à jour du statut a échoué.")
                }
                Unit
            }

    private companion object {
        const val ORDERS_COLLECTION = "orders"
    }
}

private fun Order.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "clientName" to clientName,
        "clientPhone" to clientPhone,
        "deliveryAddress" to deliveryAddress,
        "items" to items.map(OrderItem::toFirestoreMap),
        "subtotalCents" to subtotalCents,
        "deliveryFeeCents" to deliveryFeeCents,
        "totalCents" to totalCents,
        "paymentMethod" to paymentMethod.name,
        "status" to statusCode.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

private fun OrderItem.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "productId" to productId,
        "productName" to productName,
        "unitPriceCents" to unitPriceCents,
        "quantity" to quantity,
    )

private fun DocumentSnapshot.toOrder(): Order {
    val statusCode = enumValue<OrderStatus>(requiredString("status"), "status")
    val paymentMethod = enumValue<com.example.delivery.data.model.PaymentMethod>(
        requiredString("paymentMethod"),
        "paymentMethod",
    )
    val rawItems = get("items") as? List<*>
        ?: throw IllegalStateException("La commande $id ne contient pas une liste 'items' valide.")
    val items = rawItems.map { rawItem ->
        val item = rawItem as? Map<*, *>
            ?: throw IllegalStateException("Une ligne de la commande $id est invalide.")
        OrderItem(
            productId = item.requiredString("productId", id),
            productName = item.requiredString("productName", id),
            quantity = item.requiredLong("quantity", id).toInt(),
            unitPriceCents = item.requiredLong("unitPriceCents", id),
        )
    }
    val createdAt = timestampMillis("createdAt")
        ?: throw IllegalStateException("La commande $id ne contient pas de date de création valide.")

    return Order(
        id = id,
        address = requiredString("deliveryAddress"),
        items = items,
        subtotalCents = requiredLong("subtotalCents"),
        deliveryFeeCents = requiredLong("deliveryFeeCents"),
        totalCents = requiredLong("totalCents"),
        status = statusCode.frenchLabel(),
        createdAtEpochMillis = createdAt,
        userId = requiredString("userId"),
        clientName = requiredString("clientName"),
        clientPhone = requiredString("clientPhone"),
        paymentMethod = paymentMethod,
        statusCode = statusCode,
        updatedAt = timestampMillis("updatedAt") ?: createdAt,
    )
}

private fun OrderStatus.frenchLabel(): String =
    when (this) {
        OrderStatus.NEW -> "Commande reçue"
        OrderStatus.ACCEPTED -> "Acceptée"
        OrderStatus.PREPARING -> "En préparation"
        OrderStatus.OUT_FOR_DELIVERY -> "En livraison"
        OrderStatus.COMPLETED -> "Terminée"
        OrderStatus.CANCELLED -> "Annulée"
        OrderStatus.REFUSED -> "Refusée"
    }

private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: throw IllegalStateException("La valeur '$value' du champ '$field' n'est pas reconnue.")

private fun DocumentSnapshot.requiredString(field: String): String =
    getString(field)?.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("La commande $id ne contient pas de champ '$field' valide.")

private fun DocumentSnapshot.requiredLong(field: String): Long =
    getLong(field)
        ?: throw IllegalStateException("La commande $id ne contient pas de champ '$field' valide.")

private fun DocumentSnapshot.timestampMillis(field: String): Long? =
    when (val value = get(field)) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }

private fun Map<*, *>.requiredString(field: String, orderId: String): String =
    this[field] as? String
        ?: throw IllegalStateException("Une ligne de la commande $orderId ne contient pas '$field'.")

private fun Map<*, *>.requiredLong(field: String, orderId: String): Long =
    (this[field] as? Number)?.toLong()
        ?: throw IllegalStateException("Une ligne de la commande $orderId ne contient pas '$field'.")

private fun <T> Task<T>.requireSuccess(): T {
    if (!isSuccessful) {
        throw exception ?: IllegalStateException("La lecture des commandes Firebase a échoué.")
    }
    return result
}
