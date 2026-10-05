package com.example.delivery.data.repository

import com.example.delivery.data.model.Category
import com.example.delivery.data.model.Product
import com.google.android.gms.tasks.Task

interface SharedProductRepository {
    fun fetchCategories(): Task<List<Category>>
    fun fetchProducts(): Task<List<Product>>
    fun fetchProduct(productId: String): Task<Product?>

    fun fetchAllCategoriesForAdmin(): Task<List<Category>>
    fun fetchAllProductsForAdmin(): Task<List<Product>>
}
