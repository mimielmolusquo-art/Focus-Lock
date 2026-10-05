package com.example.delivery.data.repository

import com.example.delivery.data.model.Order
import com.example.delivery.data.model.OrderStatus
import com.google.android.gms.tasks.Task

interface SharedOrderRepository {
    fun submitOrder(order: Order): Task<Unit>
    fun fetchCustomerOrders(userId: String): Task<List<Order>>
    fun fetchAllOrdersForAdmin(): Task<List<Order>>
    fun updateOrderStatus(orderId: String, status: OrderStatus): Task<Unit>
}
