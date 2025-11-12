package ru.wassertech.feature.auth

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.R
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.core.auth.UserInfo
import ru.wassertech.core.network.AuthApiService
import ru.wassertech.core.ui.components.IconDialog
import ru.wassertech.core.ui.theme.AccentButtonStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Режим регистрации
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // Диалоги оффлайн режима
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showOfflineNoAccountDialog by remember { mutableStateOf(false) }
    var showOfflineExpiredDialog by remember { mutableStateOf(false) }
    var showOfflineEmptyLoginDialog by remember { mutableStateOf(false) }
    var showOfflineUserNotFoundDialog by remember { mutableStateOf(false) }
    
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
                        text = if (isRegisterMode) "Регистрация" else "Вход",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isRegisterMode) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E) // Черный цвет для "Вход"
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
                                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль")
                                }
                            }
                        )
                        
                        // Дополнительные поля для регистрации
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Имя (необязательно)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                            
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail (необязательно)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                        }
                        
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
                        
                        // Кнопка "Войти" или "Зарегистрироваться"
                        Button(
                            onClick = {
                                if (isRegisterMode) {
                                    // Регистрация
                                    if (login.isBlank() || password.isBlank()) {
                                        errorMessage = "Заполните логин и пароль"
                                        return@Button
                                    }
                                    
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            // Переводим логин в нижний регистр для регистронезависимости
                                            val loginLower = login.trim().lowercase()
                                            
                                            val (success, message) = withContext(Dispatchers.IO) {
                                                AuthApiService.registerUser(
                                                    login = loginLower,
                                                    password = password,
                                                    name = name.takeIf { it.isNotBlank() },
                                                    email = email.takeIf { it.isNotBlank() }
                                                )
                                            }
                                            
                                            if (success) {
                                                // После успешной регистрации автоматически входим
                                                val (loginSuccess, user, errorMsg) = withContext(Dispatchers.IO) {
                                                    AuthApiService.loginUser(
                                                        login = loginLower,
                                                        password = password
                                                    )
                                                }
                                                
                                                if (loginSuccess && user != null) {
                                                    val userInfo = UserInfo(
                                                        userId = user.id,
                                                        login = user.login,
                                                        role = user.role,
                                                        permissions = user.permissions,
                                                        lastLoginAtEpoch = user.lastLoginAtEpoch
                                                    )
                                                    UserAuthService.saveLogin(context, userInfo, isOfflineMode = false)
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    onLoginSuccess()
                                                } else {
                                                    errorMessage = errorMsg ?: "Регистрация успешна, но не удалось войти. Попробуйте войти вручную."
                                                }
                                            } else {
                                                errorMessage = message
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Ошибка при регистрации: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    // Вход
                                    if (login.isBlank() || password.isBlank()) {
                                        errorMessage = "Заполните логин и пароль"
                                        return@Button
                                    }
                                    
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            // Переводим логин в нижний регистр для регистронезависимости
                                            val loginLower = login.trim().lowercase()
                                            
                                            val (success, user, errorMsg) = withContext(Dispatchers.IO) {
                                                AuthApiService.loginUser(
                                                    login = loginLower,
                                                    password = password
                                                )
                                            }
                                            
                                            if (success && user != null) {
                                                val userInfo = UserInfo(
                                                    userId = user.id,
                                                    login = user.login,
                                                    role = user.role,
                                                    permissions = user.permissions,
                                                    lastLoginAtEpoch = user.lastLoginAtEpoch
                                                )
                                                UserAuthService.saveLogin(context, userInfo, isOfflineMode = false)
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = errorMsg ?: "Неверный логин или пароль"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Ошибка при входе: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
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
                                Text(if (isRegisterMode) "Зарегистрироваться" else "Войти")
                            }
                        }
                        
                        // Кнопка переключения режима
                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = null
                                password = ""
                                name = ""
                                email = ""
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = if (isRegisterMode) "Уже есть аккаунт? Войти" else "Зарегистрироваться",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Нижняя часть со ссылкой "Оффлайн режим"
                TextButton(
                    onClick = {
                        // Проверка 1: Поле логина должно быть заполнено
                        if (login.isBlank()) {
                            showOfflineEmptyLoginDialog = true
                            return@TextButton
                        }
                        
                        val loginLower = login.trim().lowercase()
                        
                        // Проверка 2: Пользователь должен быть найден локально (регистронезависимый поиск)
                        val localUser = UserAuthService.getLocalUserByLogin(context, loginLower)
                        if (localUser == null) {
                            showOfflineUserNotFoundDialog = true
                            return@TextButton
                        }
                        
                        // Проверка 3: Проверяем срок оффлайн доступа (90 дней)
                        val (canUse, errorMsg) = UserAuthService.canUseOfflineAccess(context, loginLower)
                        if (!canUse) {
                            if (errorMsg?.contains("90 дней") == true) {
                                showOfflineExpiredDialog = true
                            } else {
                                showOfflineUserNotFoundDialog = true
                            }
                            return@TextButton
                        }
                        
                        // Все проверки пройдены - можно войти в оффлайн режим
                        // Устанавливаем текущего пользователя (обновляет время входа автоматически)
                        UserAuthService.setCurrentUser(context, localUser.userId)
                        
                        // Переходим в приложение
                        onLoginSuccess()
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
        
        // Диалог: поле логина пустое
        if (showOfflineEmptyLoginDialog) {
            IconDialog(
                title = "Оффлайн режим",
                message = "Введите имя пользователя, чтобы использовать оффлайн режим",
                iconResId = R.drawable.user_xmark,
                onDismissRequest = { showOfflineEmptyLoginDialog = false },
                confirmText = "Хорошо",
                onConfirm = {
                    showOfflineEmptyLoginDialog = false
                }
            )
        }
        
        // Диалог: пользователь не найден локально
        if (showOfflineUserNotFoundDialog) {
            IconDialog(
                title = "Оффлайн режим недоступен",
                message = "Такой пользователь не найден в локальной записи устройства. Произведите онлайн вход в приложение, чтобы иметь дальнейший доступ к оффлайн режиму.",
                iconResId = R.drawable.user_xmark,
                onDismissRequest = { showOfflineUserNotFoundDialog = false },
                confirmText = "Хорошо",
                onConfirm = {
                    showOfflineUserNotFoundDialog = false
                }
            )
        }
        
        // Диалог: срок оффлайн доступа истек (более 90 дней)
        if (showOfflineExpiredDialog) {
            IconDialog(
                title = "Оффлайн доступ недоступен",
                message = "Извините, но вы слишком давно не заходили в приложение. Вам понадобится повторно пройти авторизацию, чтобы получить возможность оффлайн доступа.",
                iconResId = R.drawable.sand_clock_empty,
                onDismissRequest = { showOfflineExpiredDialog = false },
                confirmText = "Хорошо",
                onConfirm = {
                    showOfflineExpiredDialog = false
                }
            )
        }
    }
}
