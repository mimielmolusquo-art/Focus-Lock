package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusRose
import com.example.ui.viewmodel.FocusViewModel
import com.example.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()

    // Request notification launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    // Refresh permissions live on resume when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Sécurité") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.handleBack() },
                        modifier = Modifier.testTag("permissions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPermissions() },
                        modifier = Modifier.testTag("refresh_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Vérifier à nouveau"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Protection de ton attention",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Focus Lock a besoin d'autorisations Android spécifiques pour détecter quand une application distrayante est ouverte et bloquer son accès durant votre session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PERMISSION 1: ACCESSIBILITY SERVICE (MANDATORY)
            item {
                PermissionCard(
                    icon = Icons.Default.Accessibility,
                    title = "Service d'Accessibilité",
                    badgeText = "INDISPENSABLE",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    description = "Focus Lock utilise ce service pour détecter en temps réel le paquet de l'application au premier plan afin d'interrompre l'accès aux apps non autorisées.",
                    instruction = "Dans les paramètres : accédez à 'Applications téléchargées' ou 'Services installés' > activez 'Focus Lock Service'.",
                    isGranted = isAccessibilityEnabled,
                    buttonLabel = "Ouvrir les Paramètres d'Accessibilité",
                    onButtonClick = {
                        PermissionUtils.openAccessibilitySettings(context)
                    },
                    testTag = "perm_accessibility"
                )
            }

            // PERMISSION 2: NOTIFICATIONS
            item {
                PermissionCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications en arrière-plan",
                    badgeText = "RECOMMANDÉ",
                    badgeColor = FocusEmerald,
                    description = "Affiche le compte à rebours en temps réel dans votre barre d'état et empêche le système d'arrêter prématurément votre session de concentration.",
                    instruction = "Nécessaire sur Android 13 et versions plus récentes.",
                    isGranted = isNotificationEnabled,
                    buttonLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Autoriser les notifications" else "Ouvrir les paramètres",
                    onButtonClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionUtils.openNotificationSettings(context)
                        }
                    },
                    testTag = "perm_notifications"
                )
            }

            // PERMISSION 3: DRAW OVER OTHER APPS (OVERLAY)
            item {
                PermissionCard(
                    icon = Icons.Default.Visibility,
                    title = "Superposition sur les autres apps",
                    badgeText = "CONFORT",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    description = "Permet d'afficher instantanément l'écran d'alerte de verrouillage par-dessus n'importe quelle application restreinte que vous tenteriez d'ouvrir.",
                    instruction = "Activez 'Autoriser la superposition sur d'autres applications'.",
                    isGranted = canDrawOverlays,
                    buttonLabel = "Ouvrir les Paramètres de superposition",
                    onButtonClick = {
                        PermissionUtils.openOverlaySettings(context)
                    },
                    testTag = "perm_overlay"
                )
            }

            // REFRESH BUTTON
            item {
                OutlinedButton(
                    onClick = { viewModel.refreshPermissions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("manual_verify_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vérifier à nouveau l'état des autorisations")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    badgeText: String,
    badgeColor: Color,
    description: String,
    instruction: String,
    isGranted: Boolean,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isGranted) {
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(FocusEmerald.copy(alpha = 0.5f))
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Icon + Title + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) FocusEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) FocusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Live status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGranted) FocusEmerald.copy(alpha = 0.15f) else FocusRose.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isGranted) FocusEmerald else FocusRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) "Actif" else "Inactif",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isGranted) FocusEmerald else FocusRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 $instruction",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isGranted) "Modifier dans les paramètres" else buttonLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
