package ru.wassertech.client.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.wassertech.client.auth.AuthRepository
import ru.wassertech.client.auth.LoginResult
import ru.wassertech.client.data.OfflineModeManager
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.theme.AccentButtonStyle

/**
 * Экран логина для Wassertech Client
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val authRepository = remember { AuthRepository(context) }
    val offlineModeManager = remember { OfflineModeManager.getInstance(context) }
    
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Верхняя часть с логотипом
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Логотип
                    Image(
                        painter = painterResource(id = R.drawable.logo_wassertech),
                        contentDescription = "Wassertech Logo",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 32.dp),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Заголовок
                    Text(
                        text = "Вход",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Форма входа
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Поле логина
                        OutlinedTextField(
                            value = login,
                            onValueChange = {
                                login = it
                                errorMessage = null
                            },
                            label = { Text("Логин") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            isError = errorMessage != null
                        )
                        
                        // Поле пароля
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Пароль") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            isError = errorMessage != null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff
                                
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = image,
                                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                                    )
                                }
                            }
                        )
                        
                        // Сообщение об ошибке
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Кнопка "Войти"
                        Button(
                            onClick = {
                                if (login.isBlank() || password.isBlank()) {
                                    errorMessage = "Заполните логин и пароль"
                                    return@Button
                                }
                                
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    when (val result = authRepository.login(login.trim(), password)) {
                                        is LoginResult.Success -> {
                                            // Успешный вход: отключаем оффлайн режим
                                            offlineModeManager.setOfflineMode(false)
                                            onLoginSuccess()
                                        }
                                        is LoginResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                    
                                    isLoading = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading,
                            colors = AccentButtonStyle.buttonColors()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Войти")
                            }
                        }
                    }
                }
                
                // Нижняя часть с опцией "Оффлайн режим"
                TextButton(
                    onClick = {
                        scope.launch {
                            // Включаем оффлайн режим
                            offlineModeManager.setOfflineMode(true)
                            // Переходим в приложение без авторизации
                            onLoginSuccess()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Оффлайн режим",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

