package com.example.wassertech

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.wassertech.auth.UserAuthService
import com.example.wassertech.ui.AppNavHost
import com.example.wassertech.ui.auth.LoginScreen
import com.example.wassertech.ui.splash.SplashRouteFixed
import com.example.wassertech.ui.theme.WassertechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настройка системных баров ДО setContent для немедленного применения
        setupSystemBars()
        
        setContent {
            WassertechTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainContent()
                }
            }
        }
    }
    
    private fun setupSystemBars() {
        val window = window
        val view = window.decorView
        
        // Установка белого цвета для системных баров (чтобы темные иконки были видны)
        val whiteColor = Color(0xFFFFFFFF).toArgb()
        
        // Установка белого цвета статус-бара
        window.statusBarColor = whiteColor
        
        // Установка белого цвета навигационной панели
        window.navigationBarColor = whiteColor
        
        // Настройка WindowInsetsController для темных иконок
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        
        // Устанавливаем темные иконки для системных баров
        // false = темные иконки на светлом фоне (белый фон + темные иконки)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
    }
}

@Composable
private fun MainContent() {
    var showSplash by remember { mutableStateOf(true) }
    var showLogin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Проверяем состояние входа после сплэша
    LaunchedEffect(showSplash) {
        if (!showSplash) {
            showLogin = !UserAuthService.isLoggedIn(context)
        }
    }
    
    if (showSplash) {
        SplashRouteFixed(
            onFinished = { showSplash = false },
            totalMs = 1500
        )
    } else if (showLogin) {
        LoginScreen(
            onLoginSuccess = {
                showLogin = false
            }
        )
    } else {
        // Use internal AppNavHost with its own AppTopBar (shows back arrow automatically)
        AppNavHost(
            onLogout = {
                UserAuthService.logout(context)
                showLogin = true
            }
        )
    }
}
