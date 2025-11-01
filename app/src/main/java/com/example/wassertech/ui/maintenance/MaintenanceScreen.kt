package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.viewmodel.ChecklistUiField
import com.example.wassertech.viewmodel.MaintenanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    // для сохранения
    siteId: String,
    installationId: String,
    installationName: String,
    // пока работаем от компонента (как сейчас)
    componentId: String,
    // навигация
    onNavigateBack: () -> Unit,               // остаётся на будущее (иконку назад в этом экране мы не рисуем)
    onNavigateToHistory: (installationId: String) -> Unit
) {
    val vm: MaintenanceViewModel = viewModel()

    // загрузка полей для выбранного компонента
    LaunchedEffect(componentId) { vm.loadForComponent(componentId) }

    val fields by vm.fields.collectAsState()
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var technician by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<String?>(null) }

    // ВАЖНО: не рисуем TopAppBar здесь — он уже есть в общем Scaffold приложения.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ /* игнорируем внутренний padding, чтобы не было лишних отступов сверху */ ->

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            // Узкая полоска-навигация под AppBar — как на остальных экранах
            Surface(tonalElevation = 1.dp) {
                Text(
                    text = "Проведение обслуживания",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Плашка заголовка «ТО установки <имя>» в фирменном стиле
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SettingsApplications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ТО установки $installationName",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Контент формы
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fields.isEmpty()) {
                    Text(
                        "Шаблон не найден для этого компонента.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    fields.forEach { f ->
                        when (f.type) {
                            FieldType.CHECKBOX -> {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(f.label)
                                    Checkbox(
                                        checked = f.boolValue == true,
                                        onCheckedChange = { f.boolValue = it }
                                    )
                                }
                            }

                            FieldType.NUMBER -> {
                                OutlinedTextField(
                                    value = f.numberValue,
                                    onValueChange = { f.numberValue = it },
                                    label = { Text(labelWithUnit(f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    supportingText = {
                                        val warn = validateNumber(f.numberValue, f.min, f.max)
                                        if (warn != null) Text(warn)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            FieldType.TEXT -> {
                                OutlinedTextField(
                                    value = f.textValue,
                                    onValueChange = { f.textValue = it },
                                    label = { Text(f.label) },
                                    singleLine = false,
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Кнопки
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            vm.saveSession(
                                siteId = siteId,
                                installationId = installationId,
                                technician = technician,
                                notes = notes
                            )
                            scope.launch { snackbarHostState.showSnackbar("ТО сохранено") }
                            // По твоему последнему пожеланию — остаёмся на экране (в историю не уходим).
                            // onNavigateToHistory(installationId)
                        },
                        enabled = fields.isNotEmpty()
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

// ------- helpers -------

private fun labelWithUnit(f: ChecklistUiField): String =
    if (f.unit != null) "${f.label}, ${f.unit}" else f.label

private fun validateNumber(v: String, min: Double?, max: Double?): String? {
    val d = v.toDoubleOrNull() ?: return null
    if (min != null && d < min) return "Минимум: $min"
    if (max != null && d > max) return "Максимум: $max"
    return null
}
