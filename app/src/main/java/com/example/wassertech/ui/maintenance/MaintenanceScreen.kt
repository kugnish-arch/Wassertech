package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.MaintenanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    componentId: String,
    onDone: () -> Unit,
    vm: MaintenanceViewModel = viewModel()
) {
    LaunchedEffect(componentId) {
        vm.loadForComponent(componentId)
    }
    val fields by vm.fields.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (fields.isEmpty()) {
            Text("Шаблон не найден для этого компонента.", style = MaterialTheme.typography.bodyLarge)
        } else {
            fields.forEach { f ->
                when (f.type) {
                    com.example.wassertech.data.types.FieldType.CHECKBOX -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(f.label)
                            Checkbox(checked = f.boolValue == true, onCheckedChange = { f.boolValue = it })
                        }
                    }
                    com.example.wassertech.data.types.FieldType.NUMBER -> {
                        OutlinedTextField(
                            value = f.numberValue,
                            onValueChange = { f.numberValue = it },
                            label = { Text(labelWithUnit(f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = {
                                val warn = validateNumber(f.numberValue, f.min, f.max)
                                if (warn != null) Text(warn)
                            }
                        )
                    }
                    com.example.wassertech.data.types.FieldType.TEXT -> {
                        OutlinedTextField(
                            value = f.textValue,
                            onValueChange = { f.textValue = it },
                            label = { Text(f.label) },
                            singleLine = false,
                            minLines = 2
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDone) { Text("Отмена") }
            Button(onClick = {
                vm.saveSession(siteId = "unknown", installationId = null, technician = null, notes = null)
                onDone()
            }, enabled = fields.isNotEmpty()) { Text("Сохранить") }
        }
    }
}

private fun labelWithUnit(f: com.example.wassertech.viewmodel.ChecklistUiField): String {
    return if (f.unit != null) "${f.label}, ${f.unit}" else f.label
}

private fun validateNumber(v: String, min: Double?, max: Double?): String? {
    val d = v.toDoubleOrNull() ?: return null
    if (min != null && d < min) return "Минимум: $min"
    if (max != null && d > max) return "Максимум: $max"
    return null
}
