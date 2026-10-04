package com.example.delivery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.delivery.ui.components.QuantityStepper
import com.example.delivery.ui.components.SectionTitle
import com.example.delivery.ui.components.formatPrice
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun CartScreen(
    viewModel: DeliveryViewModel,
    onOpenCatalog: () -> Unit,
    onCheckout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        SectionTitle("Votre panier")
        Spacer(Modifier.height(5.dp))
        Text("Tout est prêt pour votre prochaine pause.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (viewModel.state.cartItems.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🛍️", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text("Votre panier est vide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Les bons petits plats vous attendent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onOpenCatalog, modifier = Modifier.height(52.dp)) {
                    Text("Découvrir le menu")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                items(viewModel.state.cartItems, key = { it.productId }) { item ->
                    val product = viewModel.product(item.productId)
                    if (product != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(product.imagePlaceholder, style = MaterialTheme.typography.headlineMedium)
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${formatPrice(product.priceCents)} / unité",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        formatPrice(product.priceCents * item.quantity),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    QuantityStepper(
                                        quantity = item.quantity,
                                        onQuantityChange = { viewModel.updateQuantity(item.productId, it) },
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.removeFromCart(item.productId) },
                                        modifier = Modifier.height(48.dp),
                                    ) { Text("Supprimer") }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OrderSummary(
                subtotalCents = viewModel.subtotalCents(),
                deliveryFeeCents = viewModel.deliveryFeeCents(),
                totalCents = viewModel.totalCents(),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = viewModel.state.cartItems.isNotEmpty(),
            ) {
                Text("Continuer · ${formatPrice(viewModel.totalCents())}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderSummary(
    subtotalCents: Long,
    deliveryFeeCents: Long,
    totalCents: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PriceRow("Sous-total", subtotalCents)
        PriceRow("Livraison", deliveryFeeCents)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                formatPrice(totalCents),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PriceRow(label: String, priceCents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatPrice(priceCents))
    }
}
