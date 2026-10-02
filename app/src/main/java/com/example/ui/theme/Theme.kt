package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FocusCyanLight,
    onPrimary = FocusSlate950,
    primaryContainer = FocusSlate800,
    onPrimaryContainer = FocusCyanLight,
    secondary = FocusEmeraldLight,
    onSecondary = FocusSlate950,
    secondaryContainer = FocusSlate800,
    onSecondaryContainer = FocusEmeraldLight,
    tertiary = FocusAmber,
    onTertiary = FocusSlate950,
    background = FocusSlate950,
    onBackground = FocusSlate100,
    surface = FocusSlate900,
    onSurface = FocusSlate100,
    surfaceVariant = FocusSlate800,
    onSurfaceVariant = FocusSlate400,
    outline = FocusSlate700,
    outlineVariant = FocusSlate800,
    error = FocusRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FocusCyanDark,
    onPrimary = Color.White,
    primaryContainer = FocusSlate100,
    onPrimaryContainer = FocusSlate900,
    secondary = FocusEmerald,
    onSecondary = Color.White,
    secondaryContainer = FocusSlate100,
    onSecondaryContainer = FocusSlate900,
    tertiary = FocusAmber,
    onTertiary = Color.White,
    background = FocusSlate50,
    onBackground = FocusSlate900,
    surface = Color.White,
    onSurface = FocusSlate900,
    surfaceVariant = FocusSlate100,
    onSurfaceVariant = FocusSlate600,
    outline = FocusSlate200,
    outlineVariant = FocusSlate100,
    error = FocusRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep bespoke calm palette by default for strong identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
