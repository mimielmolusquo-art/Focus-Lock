package com.example.delivery.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.delivery.data.model.Order
import com.example.delivery.ui.components.formatPrice
import com.example.delivery.viewmodel.DeliveryViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(
    viewModel: DeliveryViewModel,
    onOrderClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Mes commandes", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(5.dp))
        Text("Vos confirmations enregistrées sur cet appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        if (viewModel.state.orders.isEmpty()) {
            EmptyMessage("Aucune commande pour le moment. Vos prochaines confirmations apparaîtront ici.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.state.orders, key = { it.id }) { order ->
                    OrderCard(order = order, onClick = { onOrderClick(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.id, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(formatPrice(order.totalCents), fontWeight = FontWeight.ExtraBold)
            }
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE)
                    .format(Date(order.createdAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${order.items.sumOf { it.quantity }} article(s) · ${order.status}")
        }
    }
}

@Composable
fun OrderDetailScreen(order: Order?) {
    if (order == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Cette commande n'est plus disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Détail de la commande", style = MaterialTheme.typography.headlineSmall)
        Text(order.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE)
                .format(Date(order.createdAtEpochMillis)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(order.status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text("Adresse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(order.address)
        Text("Articles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        order.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.quantity} × ${item.productName}", modifier = Modifier.weight(1f))
                Text(formatPrice(item.unitPriceCents * item.quantity), fontWeight = FontWeight.Medium)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sous-total")
            Text(formatPrice(order.subtotalCents))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Livraison")
            Text(formatPrice(order.deliveryFeeCents))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", fontWeight = FontWeight.Bold)
            Text(formatPrice(order.totalCents), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "Confirmation locale uniquement : cette commande n'a pas été transmise à un service externe.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
