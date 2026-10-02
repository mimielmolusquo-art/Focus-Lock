package com.example.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusRose
import com.example.ui.theme.FocusSlate700
import com.example.ui.theme.FocusSlate800
import com.example.ui.theme.FocusSlate900
import com.example.ui.theme.FocusSlate950
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.util.Locale

class BlockedAppActivity : ComponentActivity() {

    private val blockedLabelState = androidx.compose.runtime.mutableStateOf("")
    private val objectiveState = androidx.compose.runtime.mutableStateOf("")
    private val endTimeState = androidx.compose.runtime.mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateFromIntent(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                // Prevent hardware back button from dropping straight back into the blocked app
                BackHandler {
                    returnToSession()
                }

                BlockedAppScreen(
                    blockedLabel = blockedLabelState.value,
                    objective = objectiveState.value,
                    endTimeMillis = endTimeState.longValue,
                    onReturnToSession = { returnToSession() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent?) {
        val blockedPackage = intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        blockedLabelState.value = intent?.getStringExtra(EXTRA_BLOCKED_LABEL) ?: blockedPackage
        objectiveState.value = intent?.getStringExtra(EXTRA_OBJECTIVE) ?: "Session de concentration"
        endTimeState.longValue = intent?.getLongExtra(EXTRA_END_TIME, System.currentTimeMillis() + 60_000L) ?: (System.currentTimeMillis() + 60_000L)
    }

    private fun returnToSession() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCKED_LABEL = "extra_blocked_label"
        const val EXTRA_OBJECTIVE = "extra_objective"
        const val EXTRA_END_TIME = "extra_end_time"
    }
}

@Composable
fun BlockedAppScreen(
    blockedLabel: String,
    objective: String,
    endTimeMillis: Long,
    onReturnToSession: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(500L)
        }
    }

    val remainingMillis = maxOf(0L, endTimeMillis - currentTime)
    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        containerColor = FocusSlate950
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Shield Icon
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    FocusRose.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(FocusSlate900)
                            .border(2.dp, FocusRose.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield Locked",
                            tint = FocusRose,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title
                Text(
                    text = "Application bloquée",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("blocked_title")
                )

                if (blockedLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = FocusSlate800.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = blockedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Objective reminder card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, FocusSlate700, RoundedCornerShape(20.dp)),
                    color = FocusSlate900
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tu avais choisi de travailler sur :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = objective,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = FocusCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("blocked_objective")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocusSlate800)
                                .padding(vertical = 10.dp, horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Temps restant : $formattedTime",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("blocked_remaining_time")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Return to Session Button
                Button(
                    onClick = onReturnToSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("return_to_session_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocusCyan,
                        contentColor = FocusSlate950
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "RETOURNER À MA SESSION",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
