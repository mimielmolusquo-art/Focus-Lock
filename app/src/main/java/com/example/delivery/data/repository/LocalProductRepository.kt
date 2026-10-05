package com.example.delivery.data.repository

import com.example.delivery.data.model.Category
import com.example.delivery.data.model.Product

object LocalProductRepository : ProductRepository {
    private val categories = listOf(
        Category("burgers", "Burgers", "🍔"),
        Category("pizza", "Pizzas", "🍕"),
        Category("bowls", "Bowls", "🥗"),
        Category("private-menu", "Menu privé", "🔒", requiredAccessId = "private-menu"),
    ) + DeliveryProductSeed.categories

    private val products = listOf(
        Product(
            "classic-burger",
            "Le Classique",
            "Steak haché, cheddar fondant, salade fraîche et sauce maison.",
            1290,
            "burgers",
            "🍔",
            featured = true,
        ),
        Product(
            "veggie-burger",
            "Le Veggie",
            "Galette de légumes, avocat crémeux et pousses d'épinards.",
            1190,
            "burgers",
            "🥬",
            featured = true,
        ),
        Product(
            "margherita",
            "Margherita",
            "Tomates mûries au soleil, mozzarella et basilic frais.",
            1350,
            "pizza",
            "🍕",
            featured = true,
        ),
        Product(
            "four-cheeses",
            "Quatre fromages",
            "Une pâte fine garnie de quatre fromages généreux.",
            1490,
            "pizza",
            "🧀",
        ),
        Product(
            "salmon-bowl",
            "Bowl saumon",
            "Saumon grillé, riz parfumé, avocat et légumes croquants.",
            3000,
            "bowls",
            "🥗",
            featured = true,
        ),
        Product(
            "falafel-bowl",
            "Bowl falafel",
            "Falafels dorés, quinoa, concombre et sauce tahini.",
            3500,
            "bowls",
            "🥙",
        ),
        Product(
            "private-tasting-menu",
            "Menu dégustation secret",
            "Une sélection surprise réservée aux invités.",
            4900,
            "private-menu",
            "✨",
            isPrivate = true,
            accessId = "private-menu",
        ),
        Product(
            "private-chef-bowl",
            "Bowl du chef",
            "Une création exclusive préparée en édition limitée.",
            3600,
            "private-menu",
            "👨‍🍳",
            isPrivate = true,
            accessId = "private-menu",
        ),
    ) + DeliveryProductSeed.products

    override fun getCategories(unlockedAccessIds: Set<String>): List<Category> =
        categories.filter { category ->
            category.requiredAccessId == null || category.requiredAccessId in unlockedAccessIds
        }

    override fun getProducts(unlockedAccessIds: Set<String>): List<Product> {
        val visibleCategoryIds = getCategories(unlockedAccessIds).mapTo(mutableSetOf()) { it.id }
        return products.filter { it.isActive && it.categoryId in visibleCategoryIds }
    }

    override fun getProduct(productId: String, unlockedAccessIds: Set<String>): Product? =
        getProducts(unlockedAccessIds).firstOrNull { it.id == productId }

    override fun getRestrictedAccessIds(): Set<String> =
        categories.mapNotNullTo(mutableSetOf()) { it.requiredAccessId }
}
