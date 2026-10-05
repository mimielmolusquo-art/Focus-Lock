package com.example.delivery

import com.example.delivery.data.local.InMemoryDeliveryLocalStore
import com.example.delivery.data.repository.ConfiguredMenuAccessCodeValidator
import com.example.delivery.data.repository.DeliveryProductSeed
import com.example.delivery.data.repository.MenuAccessCode
import com.example.delivery.data.repository.MenuAccessCodeValidator
import com.example.delivery.viewmodel.DeliveryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryViewModelTest {
    @Test
    fun `cart quantities update the subtotal and delivery fee`() {
        val viewModel = DeliveryViewModel()

        assertEquals(0L, viewModel.totalCents())
        viewModel.addToCart("classic-burger")
        viewModel.addToCart("classic-burger", quantity = 2)
        viewModel.addToCart("margherita")

        assertEquals(4, viewModel.cartItemCount())
        assertEquals(5_220L, viewModel.subtotalCents())
        assertEquals(290L, viewModel.deliveryFeeCents())
        assertEquals(5_510L, viewModel.totalCents())

        viewModel.updateQuantity("classic-burger", 1)
        assertEquals(2_640L, viewModel.subtotalCents())
        viewModel.updateQuantity("margherita", 0)
        assertEquals(1, viewModel.cartItemCount())
        assertEquals(1_290L, viewModel.subtotalCents())
    }

    @Test
    fun `cart rejects invalid quantities and caps valid quantities`() {
        val viewModel = DeliveryViewModel()
        viewModel.addToCart("classic-burger", quantity = -2)
        viewModel.addToCart("unknown-product")
        assertTrue(viewModel.state.cartItems.isEmpty())

        viewModel.addToCart("classic-burger", quantity = Int.MAX_VALUE)
        viewModel.addToCart("classic-burger")
        assertEquals(DeliveryViewModel.MAX_ITEM_QUANTITY, viewModel.cartItemCount())

        viewModel.updateQuantity("classic-burger", 1)
        viewModel.addToCart("classic-burger", quantity = Int.MAX_VALUE)
        assertEquals(DeliveryViewModel.MAX_ITEM_QUANTITY, viewModel.cartItemCount())

        viewModel.updateQuantity("classic-burger", Int.MAX_VALUE)
        assertEquals(DeliveryViewModel.MAX_ITEM_QUANTITY, viewModel.cartItemCount())
        viewModel.updateQuantity("classic-burger", -1)
        assertTrue(viewModel.state.cartItems.isEmpty())
    }

    @Test
    fun `address validation rejects incomplete and oversized addresses`() {
        val viewModel = DeliveryViewModel()
        assertTrue(!viewModel.isDeliveryAddressValid())
        viewModel.setDeliveryAddress("  123 ")
        assertTrue(!viewModel.isDeliveryAddressValid())
        assertNotNull(viewModel.deliveryAddressError())
        viewModel.setDeliveryAddress("!!!!!!!!!!!!!!!!")
        assertTrue(!viewModel.isDeliveryAddressValid())
        viewModel.setDeliveryAddress("12 rue des Fleurs, Paris")
        assertTrue(viewModel.isDeliveryAddressValid())
        viewModel.setDeliveryAddress("a".repeat(201))
        assertTrue(!viewModel.isDeliveryAddressValid())
    }

    @Test
    fun `empty cart cannot be checked out even with a valid address`() {
        val viewModel = DeliveryViewModel()
        viewModel.setDeliveryAddress("12 rue des Fleurs, Paris")

        assertNull(viewModel.placeOrder())
        assertTrue(viewModel.state.cartItems.isEmpty())
    }

    @Test
    fun `search and category selection filter the local catalog`() {
        val viewModel = DeliveryViewModel()
        viewModel.setSearchQuery("basilic")

        assertEquals(listOf("margherita"), viewModel.visibleProducts().map { it.id })

        viewModel.setSearchQuery("")
        viewModel.selectCategory("pizza")
        assertEquals(setOf("margherita", "four-cheeses"), viewModel.visibleProducts().map { it.id }.toSet())

        viewModel.selectCategory(null)
        assertEquals(viewModel.products.size, viewModel.visibleProducts().size)
    }

    @Test
    fun `demo catalog includes legacy categories and bowls start at thirty euros`() {
        val viewModel = DeliveryViewModel()

        assertEquals(
            setOf("burgers", "pizza", "bowls") + DeliveryProductSeed.categories.map { it.id },
            viewModel.categories.map { it.id }.toSet(),
        )
        viewModel.categories.forEach { category ->
            assertTrue(viewModel.products.any { it.categoryId == category.id })
        }
        val bowlPrices = viewModel.products.filter { it.categoryId == "bowls" }.map { it.priceCents }
        assertEquals(listOf(3_000L, 3_500L), bowlPrices)
        assertNull(viewModel.product("private-tasting-menu"))
        assertTrue(!viewModel.unlockMenu("incorrect"))
        assertNull(viewModel.product("private-tasting-menu"))
    }

    @Test
    fun `CBD product seed contains every requested category variant and exact price`() {
        val expected = mapOf(
            "AMNESIA HAZE" to listOf("5G" to 4_000L, "10G" to 7_000L, "25G" to 13_000L, "50G" to 24_000L),
            "JAUNE MOUSSEUX" to listOf("12G" to 5_000L, "25G" to 8_000L, "50G" to 14_000L, "100G" to 25_000L),
            "FROZEN" to listOf("5G" to 7_000L, "10G" to 12_000L, "25G" to 25_000L, "50G" to 45_000L, "100G" to 85_000L),
            "STATIC PREMIUM" to listOf("5G" to 6_000L, "10G" to 11_000L, "25G" to 24_000L, "50G" to 44_000L, "100G" to 83_000L),
        )
        val categoriesById = DeliveryProductSeed.categories.associateBy { it.id }

        assertEquals(expected.keys, DeliveryProductSeed.categories.map { it.name }.toSet())
        assertEquals(18, DeliveryProductSeed.products.size)
        assertEquals(18, DeliveryProductSeed.products.map { it.id }.toSet().size)
        expected.forEach { (categoryName, variants) ->
            val category = DeliveryProductSeed.categories.single { it.name == categoryName }
            assertEquals(
                variants,
                DeliveryProductSeed.products
                    .filter { it.categoryId == category.id }
                    .map { it.variantLabel to it.priceCents },
            )
            DeliveryProductSeed.products.filter { it.categoryId == category.id }.forEach { product ->
                assertEquals(categoryName, product.categoryName)
                assertEquals(categoryName, categoriesById.getValue(product.categoryId).name)
                assertTrue(product.name.contains(product.variantLabel.orEmpty()))
            }
        }
    }

    @Test
    fun `code validator can unlock private menu products for normal cart and order flow`() {
        val viewModel = DeliveryViewModel(
            menuAccessCodeValidator = MenuAccessCodeValidator { code ->
                if (code == "invite-42") "private-menu" else null
            },
        )

        assertTrue(viewModel.unlockMenu("invite-42"))
        assertNotNull(viewModel.product("private-tasting-menu"))
        viewModel.addToCart("private-tasting-menu", quantity = 2)
        assertEquals(2, viewModel.cartItemCount())
        assertEquals(9_800L, viewModel.subtotalCents())
        assertEquals(10_090L, viewModel.totalCents())
        viewModel.updateQuantity("private-tasting-menu", 1)
        assertEquals(4_900L, viewModel.subtotalCents())

        viewModel.setDeliveryAddress("12 rue des Fleurs, Paris")
        val order = viewModel.placeOrder()
        assertNotNull(order)
        assertEquals("Menu dégustation secret", order?.items?.single()?.productName)
        assertEquals(4_900L, order?.subtotalCents)
        assertNull(viewModel.state.orderErrorMessage)
        assertEquals(order?.id, viewModel.state.orders.single().id)
    }

    @Test
    fun `public order is blocked without clearing cart or adding order history`() {
        val viewModel = DeliveryViewModel()
        viewModel.addToCart("classic-burger", quantity = 2)
        viewModel.setDeliveryAddress("12 rue des Fleurs, Paris")

        assertNull(viewModel.placeOrder())
        assertEquals(
            DeliveryViewModel.ZONE_NOT_SERVED_MESSAGE,
            viewModel.state.orderErrorMessage,
        )
        assertEquals(2, viewModel.cartItemCount())
        assertEquals(2_580L, viewModel.subtotalCents())
        assertEquals(2_870L, viewModel.totalCents())
        assertEquals("12 rue des Fleurs, Paris", viewModel.state.deliveryAddress)
        assertTrue(viewModel.state.orders.isEmpty())
        assertNull(viewModel.state.currentOrder)
    }

    @Test
    fun `mixed cart is blocked even when private access is unlocked`() {
        val viewModel = DeliveryViewModel(
            menuAccessCodeValidator = MenuAccessCodeValidator { "private-menu" },
        )
        assertTrue(viewModel.unlockMenu("valid-code"))
        viewModel.addToCart("private-tasting-menu")
        viewModel.addToCart("classic-burger")
        viewModel.setDeliveryAddress("12 rue des Fleurs, Paris")

        assertNull(viewModel.placeOrder())
        assertEquals(
            DeliveryViewModel.ZONE_NOT_SERVED_MESSAGE,
            viewModel.state.orderErrorMessage,
        )
        assertEquals(2, viewModel.cartItemCount())
        assertTrue(viewModel.state.orders.isEmpty())
    }

    @Test
    fun `configured individual codes only unlock their enabled associated access`() {
        val validator = ConfiguredMenuAccessCodeValidator(
            listOf(
                MenuAccessCode("enabled-code", "private-menu"),
                MenuAccessCode("disabled-code", "private-menu", enabled = false),
            ),
        )
        val viewModel = DeliveryViewModel(menuAccessCodeValidator = validator)

        assertTrue(!viewModel.unlockMenu("disabled-code"))
        assertNull(viewModel.product("private-tasting-menu"))
        assertTrue(viewModel.unlockMenu("enabled-code"))
        assertNotNull(viewModel.product("private-tasting-menu"))
    }

    @Test
    fun `cart favorites and private order history survive viewmodel recreation`() {
        val store = InMemoryDeliveryLocalStore()
        val validator = MenuAccessCodeValidator { "private-menu" }
        val firstViewModel = DeliveryViewModel(localStore = store, menuAccessCodeValidator = validator)
        firstViewModel.addToCart("classic-burger", quantity = 2)
        firstViewModel.toggleFavorite("classic-burger")

        val restoredViewModel = DeliveryViewModel(localStore = store, menuAccessCodeValidator = validator)
        assertEquals(2, restoredViewModel.cartItemCount())
        assertTrue("classic-burger" in restoredViewModel.state.favoriteProductIds)
        assertTrue(restoredViewModel.unlockMenu("valid-code"))

        restoredViewModel.setDeliveryAddress("12 rue des Fleurs, Paris")
        assertNull(restoredViewModel.placeOrder())
        assertTrue(restoredViewModel.state.orders.isEmpty())
        restoredViewModel.removeFromCart("classic-burger")
        restoredViewModel.addToCart("private-tasting-menu")
        val order = restoredViewModel.placeOrder()
        assertNotNull(order)
        assertTrue(restoredViewModel.state.cartItems.isEmpty())

        val afterOrderViewModel = DeliveryViewModel(localStore = store, menuAccessCodeValidator = validator)
        assertTrue(afterOrderViewModel.state.cartItems.isEmpty())
        assertTrue("classic-burger" in afterOrderViewModel.state.favoriteProductIds)
        assertEquals(order?.id, afterOrderViewModel.state.orders.single().id)
        assertEquals(order?.totalCents, afterOrderViewModel.order(order!!.id)?.totalCents)
    }

    @Test
    fun `private order requires an address and clears the cart after confirmation`() {
        val viewModel = DeliveryViewModel(
            menuAccessCodeValidator = MenuAccessCodeValidator { "private-menu" },
        )
        viewModel.unlockMenu("valid-code")
        viewModel.addToCart("private-tasting-menu", quantity = 2)

        assertNull(viewModel.placeOrder())

        viewModel.setDeliveryAddress("  12 rue des Fleurs, Paris  ")
        val order = viewModel.placeOrder()

        assertNotNull(order)
        assertEquals("12 rue des Fleurs, Paris", order?.address)
        assertEquals(2, order?.items?.single()?.quantity)
        assertEquals(9_800L, order?.subtotalCents)
        assertEquals(10_090L, order?.totalCents)
        assertEquals("Commande reçue", order?.status)
        assertTrue(viewModel.state.cartItems.isEmpty())
        assertEquals(0L, viewModel.totalCents())

        viewModel.addToCart("private-tasting-menu")
        viewModel.setDeliveryAddress("Paris")
        assertTrue(order?.id != viewModel.placeOrder()?.id)
    }
}
