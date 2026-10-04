package com.example.delivery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.delivery.ui.components.ProductCard
import com.example.delivery.ui.components.SectionTitle
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun HomeScreen(
    viewModel: DeliveryViewModel,
    onProductClick: (String) -> Unit,
    onOpenCatalog: (String?) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text("BONJOUR 👋", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Qu'est-ce qui vous\nfait envie aujourd'hui ?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = viewModel.state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Un plat, une envie…") },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.headlineSmall) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onOpenCatalog(null) }),
        )
        Spacer(Modifier.height(18.dp))
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Le bon goût,\nau quotidien.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Parcourez notre sélection locale.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onOpenCatalog(null) },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Découvrir le menu")
                    }
                }
                Box(
                    modifier = Modifier.size(82.dp).background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🍽️", style = MaterialTheme.typography.displaySmall)
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        SectionTitle("Explorer par envie", actionLabel = "Tout voir") { onOpenCatalog(null) }
        Spacer(Modifier.height(10.dp))
        if (viewModel.categories.isEmpty()) {
            EmptyMessage("Aucune catégorie disponible pour le moment.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(viewModel.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = false,
                        onClick = { onOpenCatalog(category.id) },
                        label = { Text("${category.emoji}  ${category.name}") },
                    )
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        SectionTitle("Les incontournables", actionLabel = "Voir tout") { onOpenCatalog(null) }
        Spacer(Modifier.height(12.dp))
        val popularProducts = viewModel.featuredProducts()
        if (popularProducts.isEmpty()) {
            EmptyMessage("Aucun plat disponible pour le moment.")
        } else {
            popularProducts.forEach { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onAdd = { viewModel.addToCart(product.id) },
                    isFavorite = product.id in viewModel.state.favoriteProductIds,
                    onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun EmptyMessage(message: String) {
    Text(
        message,
        modifier = Modifier.padding(vertical = 18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
