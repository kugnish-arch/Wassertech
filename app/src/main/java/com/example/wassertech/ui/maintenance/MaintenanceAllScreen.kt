
package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.wassertech.viewmodel.MaintenanceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceAllScreen(
    installationId: String,
    onDone: () -> Unit,
    vm: MaintenanceViewModel = viewModel()
) {
    // Load UI fields and component names
    LaunchedEffect(installationId) { vm.load(installationId) }
    val uiState by vm.uiFields.collectAsState()
    val names by vm.componentNames.collectAsState()

    // Date picker state
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var dateText by remember { mutableStateOf(sdf.format(Date())) }
    var notes by remember { mutableStateOf(TextFieldValue("")) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    // Expanded cards state
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var firstOpenApplied by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (!firstOpenApplied && uiState.isNotEmpty()) {
            expanded[uiState.keys.first()] = true
            firstOpenApplied = true
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) { Text("Отмена") }
                Button(
                    onClick = {
                        val millis = runCatching { sdf.parse(dateText)?.time ?: System.currentTimeMillis() }.getOrDefault(System.currentTimeMillis())
                        vm.saveSession(
                            installationId = installationId,
                            dateEpochMillis = millis,
                            valuesByComponent = uiState,
                            notes = notes.text.ifBlank { null }
                        )
                        onDone()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить") }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата ТО") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                            dateText = sdf.format(Date(millis))
                            showDatePicker = false
                        }) { Text("ОК") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
                    }
                ) {
                    DatePicker(state = dateState)
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Примечание (опц.)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов для ТО")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.keys.toList()) { componentId ->
                        val title = names[componentId] ?: "Компонент"
                        val isExpanded = expanded[componentId] == true
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(title) },
                                )
                                if (isExpanded) {
                                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        uiState[componentId]?.forEach { f ->
                                            when (f.type) {
                                                com.example.wassertech.data.types.FieldType.TEXT -> {
                                                    OutlinedTextField(
                                                        value = f.textValue,
                                                        onValueChange = { f.textValue = it },
                                                        label = { Text(f.label) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                com.example.wassertech.data.types.FieldType.NUMBER -> {
                                                    OutlinedTextField(
                                                        value = f.numberValue,
                                                        onValueChange = { f.numberValue = it },
                                                        label = { Text(f.label + (f.unit?.let { " ($it)" } ?: "")) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                com.example.wassertech.data.types.FieldType.CHECKBOX -> {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Checkbox(checked = f.boolValue, onCheckedChange = { f.boolValue = it })
                                                        Text(f.label)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Expand/collapse control
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { expanded[componentId] = !isExpanded }) {
                                        Text(if (isExpanded) "Свернуть" else "Развернуть")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
