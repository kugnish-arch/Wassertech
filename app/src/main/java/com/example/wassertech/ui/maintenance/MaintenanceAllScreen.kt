
package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.MaintenanceViewModel
import com.example.wassertech.data.types.FieldType
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
    val uiState by vm.uiFields.collectAsState()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var dateText by remember { mutableStateOf(sdf.format(Date())) }
    var notes by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(installationId) {
        vm.load(installationId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Техническое обслуживание") }) },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Отмена") }
                Button(
                    onClick = {
                        val epoch = runCatching { sdf.parse(dateText)?.time ?: System.currentTimeMillis() }
                            .getOrElse { System.currentTimeMillis() }
                        vm.saveSession(installationId, epoch, uiState, notes.ifBlank { null })
                        onDone()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата ТО (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примечание (опц.)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.keys.toList()) { componentId ->
                    val fields = uiState[componentId].orEmpty()
                    val isOpen = expanded[componentId] ?: false
                    ElevatedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Компонент: ${'$'}componentId", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { expanded[componentId] = !isOpen }) {
                                Icon(if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            }
                        }
                        if (isOpen) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                fields.forEach { f ->
                                    when (f.type) {
                                        FieldType.CHECKBOX -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(checked = f.boolValue, onCheckedChange = { f.boolValue = it })
                                                Spacer(Modifier.width(8.dp))
                                                Text(f.label)
                                            }
                                        }
                                        FieldType.NUMBER -> {
                                            OutlinedTextField(
                                                value = f.numberValue,
                                                onValueChange = { f.numberValue = it },
                                                label = { Text(f.label + (f.unit?.let { " (${ '$'}it)" } ?: "")) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(6.dp))
                                        }
                                        FieldType.TEXT -> {
                                            OutlinedTextField(
                                                value = f.textValue,
                                                onValueChange = { f.textValue = it },
                                                label = { Text(f.label) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(6.dp))
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
}
