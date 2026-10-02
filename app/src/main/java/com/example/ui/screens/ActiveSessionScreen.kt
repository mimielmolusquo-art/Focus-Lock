package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppIconImage
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusRose
import com.example.ui.viewmodel.FocusViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ActiveSessionScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSessionState.collectAsState()
    val allApps by viewModel.allInstalledApps.collectAsState()

    var showAbandonDialog by remember { mutableStateOf(false) }
    var showAllowedAppsSheet by remember { mutableStateOf(false) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Intercept back button to confirm session abandonment
    BackHandler {
        showAbandonDialog = true
    }

    // Reliable countdown ticker based on real epoch timestamps
    LaunchedEffect(activeSession) {
        while (activeSession != null) {
            currentTimeMillis = System.currentTimeMillis()
            val state = activeSession
            if (state != null && currentTimeMillis >= state.endTimeMillis) {
                viewModel.sessionManager.completeSession()
                break
            }
            delay(500L)
        }
    }

    if (activeSession == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune session active")
        }
        return
    }

    val session = activeSession!!
    val remainingMillis = maxOf(0L, session.endTimeMillis - currentTimeMillis)
    val totalDurationMillis = maxOf(1000L, session.endTimeMillis - session.startTimeMillis)
    val elapsedMillis = totalDurationMillis - remainingMillis
    val progress = (elapsedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)

    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val allowedAppsList = remember(allApps, session.allowedPackages) {
        allApps.filter { session.allowedPackages.contains(it.packageName) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar State Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = FocusEmerald.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(FocusEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FOCUS ACTIF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = FocusEmerald
                            )
                        }
                    }

                    if (session.isStrictMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "STRICT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Attempts indicator
                if (session.restrictedAttemptsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = FocusRose.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = FocusRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${session.restrictedAttemptsCount} bloquée(s)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FocusRose
                            )
                        }
                    }
                }
            }

            // Center Content: Objective, Timer, Progress Bar, Apps count
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Focus Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Session active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Objective Title
                Text(
                    text = session.objective,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("active_objective_text")
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Huge Legible Timer (e.g. 42:18)
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("active_timer_display")
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Progress Indicator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .testTag("active_progress_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% accompli",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${session.durationMinutes} min au total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // "4 apps allowed" Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showAllowedAppsSheet = !showAllowedAppsSheet }
                        .testTag("allowed_apps_toggle"),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (session.allowedPackages.isEmpty()) "0 application autorisée (Mode strict)" else "${session.allowedPackages.size} application(s) autorisée(s)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (showAllowedAppsSheet) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showAllowedAppsSheet) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (allowedAppsList.isEmpty()) {
                                    Text(
                                        text = "Aucune application sélectionnée. Toutes les applications hors système vital sont bloquées.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    allowedAppsList.forEach { app ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            AppIconImage(
                                                drawable = app.icon,
                                                contentDescription = app.appName,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "✓",
                                                color = FocusEmerald,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action: Abandon button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = { showAbandonDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("abandon_session_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FocusRose
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FocusRose.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Abandonner la session",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }

    // Abandon Confirmation Dialog
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = {
                Text(
                    text = if (session.isStrictMode) "Rompre le mode strict ?" else "Interrompre la session ?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (session.isStrictMode) {
                        "Le mode strict est actif. Es-tu certain de vouloir abandonner avant la fin de ton objectif (${session.objective}) ?"
                    } else {
                        "Abandonner maintenant annulera la session en cours et lèvera les restrictions d'applications."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        viewModel.interruptCurrentSession()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocusRose,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_abandon_button")
                ) {
                    Text(if (session.isStrictMode) "Oui, rompre et quitter" else "Oui, interrompre")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAbandonDialog = false },
                    modifier = Modifier.testTag("cancel_abandon_button")
                ) {
                    Text("Continuer mon focus")
                }
            }
        )
    }
}
