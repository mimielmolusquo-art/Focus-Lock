package com.example.delivery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.delivery.ui.components.ProductCard
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun FavoritesScreen(
    viewModel: DeliveryViewModel,
    onProductClick: (String) -> Unit,
) {
    val products = viewModel.favoriteProducts()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Vos favoris", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(5.dp))
        Text("Retrouvez ici les plats que vous aimez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        if (products.isEmpty()) {
            EmptyMessage("Pas encore de favoris. Touchez le cœur sur un plat pour le retrouver ici.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        onAdd = { viewModel.addToCart(product.id) },
                        isFavorite = true,
                        onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                    )
                }
            }
        }
    }
}
