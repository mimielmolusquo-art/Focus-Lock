package com.example.delivery.data.repository

import com.example.delivery.data.model.Category
import com.example.delivery.data.model.Product

interface ProductRepository {
    fun getCategories(unlockedAccessIds: Set<String> = emptySet()): List<Category>
    fun getProducts(unlockedAccessIds: Set<String> = emptySet()): List<Product>
    fun getProduct(productId: String, unlockedAccessIds: Set<String> = emptySet()): Product?
    fun getRestrictedAccessIds(): Set<String>
}
