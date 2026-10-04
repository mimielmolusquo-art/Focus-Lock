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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.delivery.viewmodel.DeliveryViewModel

@Composable
fun ProfileScreen(viewModel: DeliveryViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall)
        Text("Vos préférences, simplement.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        var accessCode by rememberSaveable { mutableStateOf("") }
        var accessMessage by rememberSaveable { mutableStateOf<String?>(null) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Code d'accès", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = {
                        accessCode = it
                        accessMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Saisir un code individuel") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        accessMessage = if (viewModel.unlockMenu(accessCode)) {
                            accessCode = ""
                            "Accès activé."
                        } else {
                            "Code invalide ou désactivé."
                        }
                    },
                    enabled = accessCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Activer l'accès")
                }
                accessMessage?.let { message ->
                    Text(
                        message,
                        color = if (message == "Accès activé.") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus Delivery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Application en mode local", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Aucun compte ni moyen de paiement n'est configuré. Vos favoris, votre panier et vos confirmations sont conservés sur cet appareil.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Votre activité locale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${viewModel.state.favoriteProductIds.size} favori(s)")
                Text("${viewModel.state.orders.size} commande(s) confirmée(s) sur cet appareil")
                Spacer(Modifier.height(2.dp))
                Text("Version 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
