package com.example.delivery.data.local

import android.content.Context
import android.util.Log
import com.example.delivery.data.model.CartItem
import com.example.delivery.data.model.Order
import com.example.delivery.data.model.OrderItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface DeliveryLocalStore {
    fun readCart(): List<CartItem>
    fun writeCart(items: List<CartItem>)
    fun readFavorites(): Set<String>
    fun writeFavorites(productIds: Set<String>)
    fun readOrders(): List<Order>
    fun writeOrders(orders: List<Order>)
}

class InMemoryDeliveryLocalStore : DeliveryLocalStore {
    private var cart = emptyList<CartItem>()
    private var favorites = emptySet<String>()
    private var orders = emptyList<Order>()

    override fun readCart() = cart
    override fun writeCart(items: List<CartItem>) {
        cart = items.toList()
    }

    override fun readFavorites() = favorites
    override fun writeFavorites(productIds: Set<String>) {
        favorites = productIds.toSet()
    }

    override fun readOrders() = orders
    override fun writeOrders(orders: List<Order>) {
        this.orders = orders.toList()
    }
}

class SharedPreferencesDeliveryLocalStore(context: Context) : DeliveryLocalStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readCart(): List<CartItem> =
        readArray(CART_KEY).mapNotNull { value ->
            val item = value as? JSONObject ?: return@mapNotNull null
            val productId = item.optString("productId")
            val quantity = item.optInt("quantity")
            if (productId.isBlank() || quantity !in 1..MAX_ITEM_QUANTITY) null else CartItem(productId, quantity)
        }

    override fun writeCart(items: List<CartItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("productId", item.productId).put("quantity", item.quantity))
        }
        preferences.edit().putString(CART_KEY, array.toString()).apply()
    }

    override fun readFavorites(): Set<String> =
        readArray(FAVORITES_KEY).mapNotNull { (it as? String)?.takeIf(String::isNotBlank) }.toSet()

    override fun writeFavorites(productIds: Set<String>) {
        val array = JSONArray()
        productIds.forEach(array::put)
        preferences.edit().putString(FAVORITES_KEY, array.toString()).apply()
    }

    override fun readOrders(): List<Order> =
        readArray(ORDERS_KEY).mapNotNull { value ->
            val json = value as? JSONObject ?: return@mapNotNull null
            try {
                val itemsJson = json.getJSONArray("items")
                val items = buildList {
                    for (index in 0 until itemsJson.length()) {
                        val item = itemsJson.getJSONObject(index)
                        add(
                            OrderItem(
                                productId = item.getString("productId"),
                                productName = item.getString("productName"),
                                quantity = item.getInt("quantity"),
                                unitPriceCents = item.getLong("unitPriceCents"),
                            ),
                        )
                    }
                }
                Order(
                    id = json.getString("id"),
                    address = json.getString("address"),
                    items = items,
                    subtotalCents = json.getLong("subtotalCents"),
                    deliveryFeeCents = json.getLong("deliveryFeeCents"),
                    totalCents = json.getLong("totalCents"),
                    status = json.getString("status"),
                    createdAtEpochMillis = json.getLong("createdAtEpochMillis"),
                ).takeIf { it.id.isNotBlank() && items.isNotEmpty() && items.all { orderItem -> orderItem.quantity > 0 } }
            } catch (exception: JSONException) {
                Log.w(TAG, "Une commande locale illisible a été ignorée.", exception)
                null
            }
        }

    override fun writeOrders(orders: List<Order>) {
        val array = JSONArray()
        orders.forEach { order ->
            val items = JSONArray()
            order.items.forEach { item ->
                items.put(
                    JSONObject()
                        .put("productId", item.productId)
                        .put("productName", item.productName)
                        .put("quantity", item.quantity)
                        .put("unitPriceCents", item.unitPriceCents),
                )
            }
            array.put(
                JSONObject()
                    .put("id", order.id)
                    .put("address", order.address)
                    .put("items", items)
                    .put("subtotalCents", order.subtotalCents)
                    .put("deliveryFeeCents", order.deliveryFeeCents)
                    .put("totalCents", order.totalCents)
                    .put("status", order.status)
                    .put("createdAtEpochMillis", order.createdAtEpochMillis),
            )
        }
        preferences.edit().putString(ORDERS_KEY, array.toString()).apply()
    }

    private fun readArray(key: String): List<Any> {
        val saved = preferences.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(saved)
            buildList {
                for (index in 0 until array.length()) add(array.get(index))
            }
        } catch (exception: JSONException) {
            Log.w(TAG, "Des données locales illisibles ont été ignorées pour $key.", exception)
            emptyList()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "focus_delivery_local"
        const val CART_KEY = "cart"
        const val FAVORITES_KEY = "favorites"
        const val ORDERS_KEY = "orders"
        const val MAX_ITEM_QUANTITY = 99
        const val TAG = "DeliveryLocalStore"
    }
}
