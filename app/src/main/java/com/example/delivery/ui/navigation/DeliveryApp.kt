package com.example.delivery.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.delivery.ui.screens.CartScreen
import com.example.delivery.ui.screens.CatalogScreen
import com.example.delivery.ui.screens.CheckoutScreen
import com.example.delivery.ui.screens.ConfirmationScreen
import com.example.delivery.ui.screens.FavoritesScreen
import com.example.delivery.ui.screens.HomeScreen
import com.example.delivery.ui.screens.OrderDetailScreen
import com.example.delivery.ui.screens.OrdersScreen
import com.example.delivery.ui.screens.ProductDetailsScreen
import com.example.delivery.ui.screens.ProfileScreen
import com.example.delivery.viewmodel.DeliveryViewModel

private const val HOME_ROUTE = "home"
private const val CATALOG_ROUTE = "catalog"
private const val CART_ROUTE = "cart"
private const val PRODUCT_ROUTE = "product/{productId}"
private const val CHECKOUT_ROUTE = "checkout"
private const val CONFIRMATION_ROUTE = "confirmation"
private const val FAVORITES_ROUTE = "favorites"
private const val ORDERS_ROUTE = "orders"
private const val ORDER_DETAIL_ROUTE = "orders/{orderId}"
private const val PROFILE_ROUTE = "profile"

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: String,
)

private val bottomDestinations = listOf(
    BottomDestination(HOME_ROUTE, "Accueil", "⌂"),
    BottomDestination(CATALOG_ROUTE, "Recherche", "⌕"),
    BottomDestination(FAVORITES_ROUTE, "Favoris", "♡"),
    BottomDestination(ORDERS_ROUTE, "Commandes", "≡"),
    BottomDestination(PROFILE_ROUTE, "Profil", "○"),
)

@Composable
fun DeliveryApp(
    viewModel: DeliveryViewModel,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in bottomDestinations.map { it.route }
    val showBackNavigation = currentRoute in listOf(CART_ROUTE, PRODUCT_ROUTE, CHECKOUT_ROUTE, ORDER_DETAIL_ROUTE)
    val title = when (currentRoute) {
        HOME_ROUTE -> "FOCUS DELIVERY"
        CATALOG_ROUTE -> "EXPLORER"
        CART_ROUTE -> "MON PANIER"
        FAVORITES_ROUTE -> "MES FAVORIS"
        ORDERS_ROUTE -> "MES COMMANDES"
        PROFILE_ROUTE -> "MON ESPACE"
        PRODUCT_ROUTE -> "LE MENU"
        CHECKOUT_ROUTE -> "VOTRE COMMANDE"
        CONFIRMATION_ROUTE -> "CONFIRMATION"
        ORDER_DETAIL_ROUTE -> "DÉTAIL"
        else -> "FOCUS DELIVERY"
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showBackNavigation) {
                    TextButton(
                        onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(HOME_ROUTE) {
                                    popUpTo(HOME_ROUTE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("← Retour") }
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentRoute != CART_ROUTE && currentRoute != CONFIRMATION_ROUTE) {
                    TextButton(
                        onClick = {
                            navController.navigate(CART_ROUTE) { launchSingleTop = true }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(if (viewModel.cartItemCount() > 0) "Panier · ${viewModel.cartItemCount()}" else "Panier")
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.popBackStack(HOME_ROUTE, inclusive = false)
                                if (destination.route != HOME_ROUTE) {
                                    navController.navigate(destination.route) { launchSingleTop = true }
                                }
                            },
                            icon = { Text(destination.icon, style = androidx.compose.material3.MaterialTheme.typography.titleLarge) },
                            label = { Text(destination.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HOME_ROUTE) {
                HomeScreen(
                    viewModel = viewModel,
                    onProductClick = { productId -> navController.navigate("product/$productId") },
                    onOpenCatalog = { categoryId ->
                        viewModel.selectCategory(categoryId)
                        navController.navigate(CATALOG_ROUTE) { launchSingleTop = true }
                    },
                )
            }
            composable(CATALOG_ROUTE) {
                CatalogScreen(
                    viewModel = viewModel,
                    onProductClick = { productId -> navController.navigate("product/$productId") },
                )
            }
            composable(CART_ROUTE) {
                CartScreen(
                    viewModel = viewModel,
                    onOpenCatalog = {
                        viewModel.selectCategory(null)
                        navController.navigate(CATALOG_ROUTE)
                    },
                    onCheckout = {
                        if (viewModel.state.cartItems.isNotEmpty()) navController.navigate(CHECKOUT_ROUTE)
                    },
                )
            }
            composable(PRODUCT_ROUTE) { entry ->
                ProductDetailsScreen(
                    viewModel = viewModel,
                    productId = entry.arguments?.getString("productId").orEmpty(),
                    onAdded = {
                        navController.navigate(CART_ROUTE) {
                            popUpTo(HOME_ROUTE)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(CHECKOUT_ROUTE) {
                CheckoutScreen(
                    viewModel = viewModel,
                    onOpenCart = { navController.popBackStack(CART_ROUTE, inclusive = false) },
                    onValidate = {
                        if (viewModel.placeOrder() != null) {
                            navController.navigate(CONFIRMATION_ROUTE) {
                                popUpTo(HOME_ROUTE)
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(CONFIRMATION_ROUTE) {
                ConfirmationScreen(
                    order = viewModel.state.currentOrder,
                    onHome = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                )
            }
            composable(FAVORITES_ROUTE) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onProductClick = { productId -> navController.navigate("product/$productId") },
                )
            }
            composable(ORDERS_ROUTE) {
                OrdersScreen(viewModel = viewModel) { orderId ->
                    navController.navigate("orders/$orderId")
                }
            }
            composable(ORDER_DETAIL_ROUTE) { entry ->
                OrderDetailScreen(viewModel.order(entry.arguments?.getString("orderId").orEmpty()))
            }
            composable(PROFILE_ROUTE) {
                ProfileScreen(viewModel)
            }
        }
    }
}
