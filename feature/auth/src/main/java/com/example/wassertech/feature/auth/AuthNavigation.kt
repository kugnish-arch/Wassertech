package ru.wassertech.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Маршруты для модуля авторизации
 */
object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot_password"
}

/**
 * Граф навигации для модуля авторизации
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onLoginSuccess: () -> Unit
) {
    composable(AuthRoutes.LOGIN) {
        LoginScreen(
            onLoginSuccess = onLoginSuccess
        )
    }
    
    composable(AuthRoutes.REGISTER) {
        // TODO: Реализовать RegisterScreen
        LoginScreen(
            onLoginSuccess = onLoginSuccess
        )
    }
    
    composable(AuthRoutes.FORGOT_PASSWORD) {
        // TODO: Реализовать ForgotPasswordScreen
        LoginScreen(
            onLoginSuccess = onLoginSuccess
        )
    }
}

