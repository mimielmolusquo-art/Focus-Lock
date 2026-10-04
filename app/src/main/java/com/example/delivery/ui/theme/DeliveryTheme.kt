package com.example.delivery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeliveryColors = lightColorScheme(
    primary = Color(0xFF205A43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFE3),
    onPrimaryContainer = Color(0xFF153B2A),
    secondary = Color(0xFFB95C3B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E6DD),
    onSecondaryContainer = Color(0xFF4A2518),
    background = Color(0xFFF7F7F2),
    surface = Color.White,
    onSurface = Color(0xFF202822),
    onSurfaceVariant = Color(0xFF68736B),
    surfaceVariant = Color(0xFFEEF1EB),
    outline = Color(0xFFDCE2DA),
)

@Composable
fun DeliveryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeliveryColors,
        content = content,
    )
}
