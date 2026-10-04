package com.example.delivery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.delivery.data.model.CartItem
import com.example.delivery.data.model.Order
import com.example.delivery.data.model.OrderItem
import com.example.delivery.data.model.Product
import com.example.delivery.data.local.DeliveryLocalStore
import com.example.delivery.data.local.InMemoryDeliveryLocalStore
import com.example.delivery.data.repository.LocalProductRepository
import com.example.delivery.data.repository.MenuAccessCodeValidator
import com.example.delivery.data.repository.NoMenuAccessCodeValidator
import com.example.delivery.data.repository.ProductRepository
import java.util.UUID
import com.example.delivery.data.repository.ConfiguredMenuAccessCodeValidator
import com.example.delivery.data.repository.MenuAccessCode

data class DeliveryUiState(
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val deliveryAddress: String = "",
    val currentOrder: Order? = null,
    val favoriteProductIds: Set<String> = emptySet(),
    val orders: List<Order> = emptyList(),
    val unlockedMenuAccessIds: Set<String> = emptySet(),
    val orderErrorMessage: String? = null,
)

class DeliveryViewModel(
    private val repository: ProductRepository = LocalProductRepository,
    private val localStore: DeliveryLocalStore = InMemoryDeliveryLocalStore(),
    private val menuAccessCodeValidator: MenuAccessCodeValidator =
        ConfiguredMenuAccessCodeValidator(
                    listOf(
                                    MenuAccessCode(
                                                        code = "2401",
                                                                        accessId = "private-menu",
                                    ),
                    ),
        ),
                                    )
                    
        
: ViewModel() {
    var state by mutableStateOf(loadInitialState())
        private set

    val categories get() = repository.getCategories(state.unlockedMenuAccessIds)
    val products get() = repository.getProducts(state.unlockedMenuAccessIds)

    fun product(productId: String) = repository.getProduct(productId, state.unlockedMenuAccessIds)

    fun featuredProducts() = products.filter { it.featured }

    fun favoriteProducts(): List<Product> = products.filter { it.id in state.favoriteProductIds }

    fun order(orderId: String): Order? = state.orders.firstOrNull { it.id == orderId }

    fun unlockMenu(code: String): Boolean {
        if (code.isBlank()) return false
        val accessId = menuAccessCodeValidator.accessIdFor(code) ?: return false
        if (accessId !in repository.getRestrictedAccessIds()) return false
        state = state.copy(
            unlockedMenuAccessIds = state.unlockedMenuAccessIds + accessId,
            orderErrorMessage = null,
        )
        return true
    }

    fun toggleFavorite(productId: String) {
        if (product(productId) == null) return
        val favorites = if (productId in state.favoriteProductIds) {
            state.favoriteProductIds - productId
        } else {
            state.favoriteProductIds + productId
        }
        state = state.copy(favoriteProductIds = favorites)
        localStore.writeFavorites(favorites)
    }

    fun visibleProducts(): List<Product> {
        val query = state.searchQuery.trim()
        return products.filter { product ->
            (state.selectedCategoryId == null || product.categoryId == state.selectedCategoryId) &&
                (query.isEmpty() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true))
        }
    }

    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun selectCategory(categoryId: String?) {
        state = state.copy(selectedCategoryId = categoryId)
    }

    fun addToCart(productId: String, quantity: Int = 1) {
        if (product(productId) == null || quantity <= 0) return
        val existing = state.cartItems.firstOrNull { it.productId == productId }
        val updatedQuantity = if (existing == null) {
            quantity.coerceAtMost(MAX_ITEM_QUANTITY)
        } else {
            existing.quantity + quantity.coerceAtMost(MAX_ITEM_QUANTITY - existing.quantity)
        }
        state = state.copy(
            cartItems = if (existing == null) {
                state.cartItems + CartItem(productId, updatedQuantity)
            } else {
                state.cartItems.map {
                    if (it.productId == productId) {
                        it.copy(quantity = updatedQuantity)
                    } else {
                        it
                    }
                }
            },
            orderErrorMessage = null,
        )
        localStore.writeCart(state.cartItems)
    }

    fun updateQuantity(productId: String, quantity: Int) {
        state = state.copy(
            cartItems = if (quantity <= 0) {
                state.cartItems.filterNot { it.productId == productId }
            } else {
                state.cartItems.map {
                    if (it.productId == productId) {
                        it.copy(quantity = quantity.coerceAtMost(MAX_ITEM_QUANTITY))
                    } else {
                        it
                    }
                }
            },
            orderErrorMessage = null,
        )
        localStore.writeCart(state.cartItems)
    }

    fun removeFromCart(productId: String) {
        state = state.copy(
            cartItems = state.cartItems.filterNot { it.productId == productId },
            orderErrorMessage = null,
        )
        localStore.writeCart(state.cartItems)
    }

    fun subtotalCents(): Long =
        state.cartItems.sumOf { item ->
            (product(item.productId)?.priceCents ?: 0L) * item.quantity
        }

    fun deliveryFeeCents(): Long = if (state.cartItems.isEmpty()) 0L else DELIVERY_FEE_CENTS

    fun totalCents(): Long = subtotalCents() + deliveryFeeCents()

    fun cartItemCount(): Int = state.cartItems.sumOf { it.quantity }

    fun setDeliveryAddress(address: String) {
        state = state.copy(deliveryAddress = address, orderErrorMessage = null)
    }

    fun deliveryAddressError(): String? {
        val address = state.deliveryAddress.trim()
        return when {
            address.length > MAX_ADDRESS_LENGTH -> "L'adresse ne peut pas dépasser 200 caractères."
            address.isNotEmpty() && address.length < MIN_ADDRESS_LENGTH ->
                "Ajoutez une adresse plus complète (5 caractères minimum)."
            address.any { it.isISOControl() && it != '\n' && it != '\t' } ->
                "L'adresse contient un caractère non autorisé."
            address.isNotEmpty() && address.none { it.isLetterOrDigit() } ->
                "Saisissez une adresse contenant au moins une lettre ou un chiffre."
            else -> null
        }
    }

    fun isDeliveryAddressValid(): Boolean =
        state.deliveryAddress.trim().let { address ->
            address.length in MIN_ADDRESS_LENGTH..MAX_ADDRESS_LENGTH &&
                address.none { it.isISOControl() && it != '\n' && it != '\t' } &&
                address.any { it.isLetterOrDigit() }
        }

    fun placeOrder(): Order? {
        val address = state.deliveryAddress.trim()
        if (!isDeliveryAddressValid() || state.cartItems.isEmpty()) return null

        val orderProducts = state.cartItems.map { item ->
            product(item.productId) ?: return null
        }
        val privateCategoryIds = repository.getCategories(state.unlockedMenuAccessIds)
            .filter { it.requiredAccessId != null }
            .mapTo(mutableSetOf()) { it.id }
        if (orderProducts.any { it.categoryId !in privateCategoryIds }) {
            state = state.copy(orderErrorMessage = ZONE_NOT_SERVED_MESSAGE)
            return null
        }

        val orderItems = state.cartItems.map { item ->
            val product = product(item.productId) ?: return null
            if (item.quantity !in 1..MAX_ITEM_QUANTITY) return null
            OrderItem(
                productId = product.id,
                productName = product.name,
                quantity = item.quantity,
                unitPriceCents = product.priceCents,
            )
        }
        if (orderItems.isEmpty()) return null

        val subtotal = subtotalCents()
        val deliveryFee = deliveryFeeCents()
        val order = Order(
            id = "FD-${UUID.randomUUID().toString().uppercase()}",
            address = address,
            items = orderItems,
            subtotalCents = subtotal,
            deliveryFeeCents = deliveryFee,
            totalCents = subtotal + deliveryFee,
        )
        state = state.copy(
            cartItems = emptyList(),
            deliveryAddress = "",
            currentOrder = order,
            orders = listOf(order) + state.orders,
            orderErrorMessage = null,
        )
        localStore.writeCart(emptyList())
        localStore.writeOrders(state.orders)
        return order
    }

    private fun loadInitialState(): DeliveryUiState {
        val knownProductIds = repository.getProducts().mapTo(mutableSetOf()) { it.id }
        val cart = localStore.readCart()
            .filter { it.productId in knownProductIds && it.quantity in 1..MAX_ITEM_QUANTITY }
            .distinctBy { it.productId }
        val favorites = localStore.readFavorites().intersect(knownProductIds)
        val orders = localStore.readOrders().sortedByDescending { it.createdAtEpochMillis }
        return DeliveryUiState(cartItems = cart, favoriteProductIds = favorites, orders = orders)
    }

    companion object {
        const val DELIVERY_FEE_CENTS = 290L
        const val MIN_ADDRESS_LENGTH = 5
        const val MAX_ADDRESS_LENGTH = 200
        const val MAX_ITEM_QUANTITY = 99
        const val ZONE_NOT_SERVED_MESSAGE = "Votre zone n'est pas desservie par nos restaurants."
    }
}
