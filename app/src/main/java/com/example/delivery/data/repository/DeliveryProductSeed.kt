package com.example.delivery.data.repository

import com.example.delivery.data.model.Category
import com.example.delivery.data.model.Product

object DeliveryProductSeed {
    val categories = listOf(
        Category("amnesia-haze", "AMNESIA HAZE", "🍃"),
        Category("jaune-mousseux", "JAUNE MOUSSEUX", "🍃"),
        Category("frozen", "FROZEN", "❄️"),
        Category("static-premium", "STATIC PREMIUM", "✨"),
    )

    val products = listOf(
        product("amnesia-haze", "AMNESIA HAZE", "5G", 4_000),
        product("amnesia-haze", "AMNESIA HAZE", "10G", 7_000),
        product("amnesia-haze", "AMNESIA HAZE", "25G", 13_000),
        product("amnesia-haze", "AMNESIA HAZE", "50G", 24_000),
        product("jaune-mousseux", "JAUNE MOUSSEUX", "12G", 5_000),
        product("jaune-mousseux", "JAUNE MOUSSEUX", "25G", 8_000),
        product("jaune-mousseux", "JAUNE MOUSSEUX", "50G", 14_000),
        product("jaune-mousseux", "JAUNE MOUSSEUX", "100G", 25_000),
        product("frozen", "FROZEN", "5G", 7_000),
        product("frozen", "FROZEN", "10G", 12_000),
        product("frozen", "FROZEN", "25G", 25_000),
        product("frozen", "FROZEN", "50G", 45_000),
        product("frozen", "FROZEN", "100G", 85_000),
        product("static-premium", "STATIC PREMIUM", "5G", 6_000),
        product("static-premium", "STATIC PREMIUM", "10G", 11_000),
        product("static-premium", "STATIC PREMIUM", "25G", 24_000),
        product("static-premium", "STATIC PREMIUM", "50G", 44_000),
        product("static-premium", "STATIC PREMIUM", "100G", 83_000),
    )

    private fun product(
        categoryId: String,
        categoryName: String,
        variantLabel: String,
        priceCents: Long,
    ) = Product(
        id = "$categoryId-${variantLabel.lowercase()}",
        name = "$categoryName · $variantLabel",
        description = "CBD · $variantLabel",
        priceCents = priceCents,
        categoryId = categoryId,
        imagePlaceholder = "🍃",
        categoryName = categoryName,
        variantLabel = variantLabel,
    )
}
