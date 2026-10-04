package com.example.delivery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.delivery.ui.components.QuantityStepper
import com.example.delivery.ui.components.formatPrice
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun ProductDetailsScreen(
    viewModel: DeliveryViewModel,
    productId: String,
    onAdded: () -> Unit,
) {
    val product = viewModel.product(productId)
    if (product == null) {
        Column(Modifier.padding(24.dp)) {
            Text("Ce produit n'est plus disponible.")
        }
        return
    }

    var quantity by rememberSaveable(productId) { mutableIntStateOf(1) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(product.imagePlaceholder, fontSize = MaterialTheme.typography.displayLarge.fontSize)
            }
            Spacer(Modifier.height(22.dp))
            Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                product.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                formatPrice(product.priceCents),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuantityStepper(
                quantity = quantity,
                onQuantityChange = {
                    quantity = it.coerceIn(1, DeliveryViewModel.MAX_ITEM_QUANTITY)
                },
                minimumQuantity = 1,
            )
            Button(
                onClick = {
                    viewModel.addToCart(product.id, quantity)
                    onAdded()
                },
                modifier = Modifier.weight(1f).padding(start = 12.dp).height(52.dp),
            ) {
                Text("Ajouter au panier · ${formatPrice(product.priceCents * quantity)}")
            }
        }
    }
}
