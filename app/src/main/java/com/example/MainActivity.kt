package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.screens.ActiveSessionScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SessionCompletionScreen
import com.example.ui.screens.SessionCreationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current

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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FocusLockApp(
                        viewModel = viewModel,
                        currentScreen = currentScreen
                    )
                }
            }
        }
    }
}

@Composable
fun FocusLockApp(
    viewModel: FocusViewModel,
    currentScreen: Screen,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 250),
        label = "screen_transition",
        modifier = modifier
    ) { screen ->
        when (screen) {
            Screen.ONBOARDING -> {
                OnboardingScreen(viewModel = viewModel)
            }
            Screen.HOME -> {
                HomeScreen(viewModel = viewModel)
            }
            Screen.SESSION_CREATION -> {
                BackHandler { viewModel.handleBack() }
                SessionCreationScreen(viewModel = viewModel)
            }
            Screen.ACTIVE_SESSION -> {
                ActiveSessionScreen(viewModel = viewModel)
            }
            Screen.COMPLETION -> {
                BackHandler { viewModel.finishSessionDone() }
                SessionCompletionScreen(viewModel = viewModel)
            }
            Screen.HISTORY -> {
                BackHandler { viewModel.handleBack() }
                HistoryScreen(viewModel = viewModel)
            }
            Screen.STATISTICS -> {
                BackHandler { viewModel.handleBack() }
                StatisticsScreen(viewModel = viewModel)
            }
            Screen.SETTINGS -> {
                BackHandler { viewModel.handleBack() }
                SettingsScreen(viewModel = viewModel)
            }
            Screen.PERMISSIONS -> {
                BackHandler { viewModel.handleBack() }
                PermissionsScreen(viewModel = viewModel)
            }
        }
    }
}
