package com.example.wassertech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    // TODO: Реализовать навигацию для клиентского приложения
    androidx.compose.material3.Text(
        text = "Wassertech Client App",
        style = MaterialTheme.typography.headlineMedium
    )
}

