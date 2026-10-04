package com.example.delivery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.delivery.ui.components.ProductCard
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun CatalogScreen(
    viewModel: DeliveryViewModel,
    onProductClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Trouver votre prochain favori", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text("Une sélection préparée pour vous.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher un plat") },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.headlineSmall) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = viewModel.state.selectedCategoryId == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("Tout") },
                )
            }
            items(viewModel.categories, key = { it.id }) { category ->
                FilterChip(
                    selected = viewModel.state.selectedCategoryId == category.id,
                    onClick = {
                        viewModel.selectCategory(
                            if (viewModel.state.selectedCategoryId == category.id) null else category.id,
                        )
                    },
                    label = { Text("${category.emoji}  ${category.name}") },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val products = viewModel.visibleProducts()
        if (products.isEmpty()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                EmptyMessage(
                    if (viewModel.products.isEmpty()) "Aucun plat disponible pour le moment."
                    else "Aucun résultat pour « ${viewModel.state.searchQuery} ».",
                )
                if (viewModel.state.searchQuery.isNotBlank() || viewModel.state.selectedCategoryId != null) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setSearchQuery("")
                            viewModel.selectCategory(null)
                        },
                        label = { Text("Effacer les filtres · Afficher tout") },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        onAdd = { viewModel.addToCart(product.id) },
                        isFavorite = product.id in viewModel.state.favoriteProductIds,
                        onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                    )
                }
            }
        }
    }
}
