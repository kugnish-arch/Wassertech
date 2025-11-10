package com.example.wassertech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.wassertech.ui.AppNavHost
import com.example.wassertech.ui.splash.SplashRouteFixed
import com.example.wassertech.ui.theme.WassertechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WassertechTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashRouteFixed(
                            onFinished = { showSplash = false },
                            totalMs = 1500
                        )
                    } else {
                        // Use internal AppNavHost with its own AppTopBar (shows back arrow automatically)
                        AppNavHost()
                    }
                }
            }
        }
    }
}
