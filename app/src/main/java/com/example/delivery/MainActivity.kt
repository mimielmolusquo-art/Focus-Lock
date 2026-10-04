package com.example.delivery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.delivery.ui.navigation.DeliveryApp
import com.example.delivery.ui.theme.DeliveryTheme
import com.example.delivery.data.local.SharedPreferencesDeliveryLocalStore
import com.example.delivery.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DeliveryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (!modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
                    throw IllegalArgumentException("ViewModel non pris en charge : ${modelClass.name}")
                }
                return DeliveryViewModel(localStore = SharedPreferencesDeliveryLocalStore(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeliveryTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DeliveryApp(viewModel = viewModel, modifier = Modifier)
                }
            }
        }
    }
}
