// Opt-in once for the whole file to avoid ExperimentalMaterial3Api warnings-as-errors
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.templates

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.viewmodel.FieldDraft
import com.example.wassertech.viewmodel.TemplateEditorViewModel

@Composable
fun TemplateEditorScreen(templateId: String) {
    // Read LocalContext in @Composable scope and capture Application
    val app = LocalContext.current.applicationContext as Application

    val vm: TemplateEditorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TemplateEditorViewModel(app, templateId) }
        }
    )

    val fields by vm.fields.collectAsState()

    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FieldDraft?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Конструктор шаблона") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; dialogOpen = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("+ Поле") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (fields.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Нет полей. Нажми «+ Поле».")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fields, key = { it.id }) { f ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(f.label) },
                                supportingContent = {
                                    val unitSuffix = f.unit?.let { " • $it" } ?: ""
                                    Text("${f.type} • ключ: ${f.key}" + unitSuffix)
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = {
                                            editing = FieldDraft(
                                                id = f.id,
                                                key = f.key,
                                                label = f.label,
                                                type = f.type,
                                                unit = f.unit,
                                                min = f.min?.toString() ?: "",
                                                max = f.max?.toString() ?: ""
                                            )
                                            dialogOpen = true
                                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                        IconButton(onClick = { vm.delete(f.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        FieldEditDialog(
            initial = editing,
            onDismiss = { dialogOpen = false },
            onSave = { vm.addOrUpdate(it); dialogOpen = false }
        )
    }
}

@Composable
private fun FieldEditDialog(
    initial: FieldDraft?,
    onDismiss: () -> Unit,
    onSave: (FieldDraft) -> Unit
) {
    var key by remember { mutableStateOf(initial?.key ?: "") }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: FieldType.TEXT) }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var min by remember { mutableStateOf(initial?.min ?: "") }
    var max by remember { mutableStateOf(initial?.max ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    FieldDraft(
                        id = initial?.id,
                        key = key,
                        label = label,
                        type = type,
                        unit = unit.ifBlank { null },
                        min = min,
                        max = max
                    )
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text(if (initial == null) "Новое поле" else "Редактирование поля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Ключ (латиница)") }, singleLine = true)
                TypeSelector(type = type, onChange = { type = it })
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Ед. изм. (опц.)") }, singleLine = true)
                OutlinedTextField(value = min, onValueChange = { min = it }, label = { Text("Мин. (опц.)") }, singleLine = true)
                OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("Макс. (опц.)") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun TypeSelector(type: FieldType, onChange: (FieldType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = when (type) {
                FieldType.TEXT -> "TEXT"
                FieldType.NUMBER -> "NUMBER"
                FieldType.CHECKBOX -> "CHECKBOX"
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Тип") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("TEXT") }, onClick = { onChange(FieldType.TEXT); expanded = false })
            DropdownMenuItem(text = { Text("NUMBER") }, onClick = { onChange(FieldType.NUMBER); expanded = false })
            DropdownMenuItem(text = { Text("CHECKBOX") }, onClick = { onChange(FieldType.CHECKBOX); expanded = false })
        }
    }
}
