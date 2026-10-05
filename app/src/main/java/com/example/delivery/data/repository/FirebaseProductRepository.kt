package com.example.delivery.data.repository

import com.example.delivery.data.model.Category
import com.example.delivery.data.model.Product
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseProductRepository(
    private val firestore: FirebaseFirestore,
) : SharedProductRepository {
    override fun fetchCategories(): Task<List<Category>> =
        firestore.collection(CATEGORIES_COLLECTION).get().continueWith { task ->
            val documents = task.requireSuccess().documents
            if (documents.isEmpty()) {
                return@continueWith DeliveryProductSeed.categories.filter { it.requiredAccessId == null }
            }
            documents
                .map(DocumentSnapshot::toCategory)
                .filter { it.isActive && it.requiredAccessId == null }
        }

    override fun fetchProducts(): Task<List<Product>> =
        firestore.collection(CATEGORIES_COLLECTION).get().continueWithTask { categoryTask ->
            val categoryDocuments = categoryTask.requireSuccess().documents
            val useSeedCatalog = categoryDocuments.isEmpty()
            val categories = categoryDocuments
                .map(DocumentSnapshot::toCategory)
                .associateBy { it.id }
                .let { if (useSeedCatalog) DeliveryProductSeed.categories.associateBy { it.id } else it }
            firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("isPrivate", false)
                .get()
                .continueWith { productTask ->
                    val documents = productTask.requireSuccess().documents
                    if (useSeedCatalog && documents.isEmpty()) {
                        return@continueWith DeliveryProductSeed.products.filterNot { it.isPrivate }
                    }
                    documents
                        .map { snapshot -> snapshot.toProduct(categories[snapshot.getString("categoryId")]?.name) }
                        .filter { product ->
                            val category = categories[product.categoryId]
                            product.isActive && !product.isPrivate &&
                                category?.isActive == true && category.requiredAccessId == null
                        }
                }
        }

    override fun fetchProduct(productId: String): Task<Product?> =
        firestore.collection(PRODUCTS_COLLECTION)
            .whereEqualTo(FieldPath.documentId(), productId)
            .whereEqualTo("isPrivate", false)
            .get()
            .continueWithTask { productTask ->
                val productSnapshot = productTask.requireSuccess().documents.firstOrNull()
                if (productSnapshot == null) {
                    return@continueWithTask firestore.collection(CATEGORIES_COLLECTION).get()
                        .continueWith { categoryTask ->
                            if (categoryTask.requireSuccess().documents.isEmpty()) {
                                DeliveryProductSeed.products.firstOrNull {
                                    it.id == productId && !it.isPrivate
                                }
                            } else {
                                null
                            }
                        }
                }
                val product = productSnapshot.toProduct()
                firestore.collection(CATEGORIES_COLLECTION).document(product.categoryId).get()
                    .continueWith { categoryTask ->
                        val categorySnapshot = categoryTask.requireSuccess()
                        if (!categorySnapshot.exists()) return@continueWith null
                        val category = categorySnapshot.toCategory()
                        product.copy(categoryName = product.categoryName ?: category.name).takeIf {
                            it.isActive && !it.isPrivate &&
                                category.isActive && category.requiredAccessId == null
                        }
                    }
            }

    override fun fetchAllCategoriesForAdmin(): Task<List<Category>> =
        firestore.collection(CATEGORIES_COLLECTION).get().continueWith { task ->
            task.requireSuccess().documents.map(DocumentSnapshot::toCategory)
        }

    override fun fetchAllProductsForAdmin(): Task<List<Product>> =
        firestore.collection(PRODUCTS_COLLECTION).get().continueWith { task ->
            task.requireSuccess().documents.map { it.toProduct() }
        }

    private companion object {
        const val CATEGORIES_COLLECTION = "categories"
        const val PRODUCTS_COLLECTION = "products"
    }
}

private fun DocumentSnapshot.toCategory(): Category =
    Category(
        id = id,
        name = requiredString("name"),
        emoji = getString("emoji") ?: "",
        requiredAccessId = getString("requiredAccessId"),
        isActive = getBoolean("isActive") ?: true,
        createdAt = timestampMillis("createdAt"),
        updatedAt = timestampMillis("updatedAt"),
    )

private fun DocumentSnapshot.toProduct(categoryName: String? = null): Product =
    Product(
        id = id,
        name = requiredString("name"),
        description = requiredString("description"),
        priceCents = requiredLong("priceCents"),
        categoryId = requiredString("categoryId"),
        imagePlaceholder = getString("imagePlaceholder") ?: "🍽️",
        featured = getBoolean("featured") ?: false,
        imageUrl = getString("imageUrl"),
        storagePath = getString("storagePath"),
        isActive = getBoolean("isActive") ?: true,
        isPrivate = getBoolean("isPrivate") ?: false,
        accessId = getString("accessId"),
        createdAt = timestampMillis("createdAt"),
        updatedAt = timestampMillis("updatedAt"),
        categoryName = getString("categoryName") ?: categoryName,
        variantLabel = getString("variantLabel"),
    )

private fun DocumentSnapshot.requiredString(field: String): String =
    getString(field)?.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("Le document $id ne contient pas de champ '$field' valide.")

private fun DocumentSnapshot.requiredLong(field: String): Long =
    getLong(field)
        ?: throw IllegalStateException("Le document $id ne contient pas de champ '$field' valide.")

private fun DocumentSnapshot.timestampMillis(field: String): Long? =
    when (val value = get(field)) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }

private fun <T> Task<T>.requireSuccess(): T {
    if (!isSuccessful) {
        throw exception ?: IllegalStateException("La lecture des données Firebase a échoué.")
    }
    return result
}
