package com.example.delivery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.delivery.data.model.OrderItem
import com.example.delivery.ui.components.SectionTitle
import com.example.delivery.ui.components.formatPrice
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun CheckoutScreen(
    viewModel: DeliveryViewModel,
    onValidate: () -> Unit,
    onOpenCart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        if (viewModel.state.cartItems.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Votre commande")
                Text("Votre panier est vide.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onOpenCart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("Retour au panier")
                }
            }
        } else {
            Text("ÉTAPE 2 SUR 3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            SectionTitle("Adresse de livraison")
            Spacer(Modifier.height(16.dp))
            val addressError = viewModel.deliveryAddressError()
            OutlinedTextField(
                value = viewModel.state.deliveryAddress,
                onValueChange = viewModel::setDeliveryAddress,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("12 rue des Fleurs, 75001 Paris") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                isError = addressError != null,
                supportingText = {
                    Text(addressError ?: "Adresse complète, entre 5 et 200 caractères.")
                },
            )
            Spacer(Modifier.height(18.dp))
            SectionTitle("Récapitulatif")
            Spacer(Modifier.height(8.dp))
            viewModel.state.cartItems.forEach { item ->
                val product = viewModel.product(item.productId)
                if (product != null) {
                    CheckoutItem(
                        item = OrderItem(
                            productId = product.id,
                            productName = product.name,
                            quantity = item.quantity,
                            unitPriceCents = product.priceCents,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            OrderSummary(
                subtotalCents = viewModel.subtotalCents(),
                deliveryFeeCents = viewModel.deliveryFeeCents(),
                totalCents = viewModel.totalCents(),
            )
            Spacer(Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "Cette confirmation est enregistrée uniquement sur cet appareil. Aucune commande n'est envoyée à un service externe.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onValidate,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                enabled = viewModel.isDeliveryAddressValid(),
            ) {
                Text("Confirmer localement · ${formatPrice(viewModel.totalCents())}", fontWeight = FontWeight.Bold)
            }
            viewModel.state.orderErrorMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CheckoutItem(item: OrderItem) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${item.quantity} × ${item.productName}")
        Text(formatPrice(item.unitPriceCents * item.quantity), fontWeight = FontWeight.Medium)
    }
}
