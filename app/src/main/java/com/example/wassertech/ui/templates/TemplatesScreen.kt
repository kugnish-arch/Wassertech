
package com.example.wassertech.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.entities.ComponentTemplateEntity
import com.example.wassertech.viewmodel.TemplatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(vm: TemplatesViewModel = viewModel()) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val items by vm.templates.collectAsState()

    val filtered = remember(items, query) {
        val q = query.text.trim().lowercase()
        if (q.isEmpty()) items
        else items.filter { (it.name + " " + (it.category ?: "")).lowercase().contains(q) }
    }.sortedWith(compareBy<ComponentTemplateEntity> { it.isArchived }
        .thenBy { it.sortOrder }
        .thenBy { it.name })

    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ComponentTemplateEntity?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Шаблоны компонентов") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; dialogOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Нет шаблонов")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filtered, key = { _, it -> it.id }) { index, item ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(item.category ?: "Без категории") },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { vm.moveUp(filtered, index) }) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                                        }
                                        IconButton(onClick = { vm.moveDown(filtered, index) }) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Down")
                                        }
                                        IconButton(onClick = {
                                            vm.archive(item.id, !item.isArchived)
                                        }) {
                                            Icon(
                                                if (item.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                contentDescription = "Archive toggle"
                                            )
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
        TemplateEditDialog(
            initial = editing,
            onDismiss = { dialogOpen = false },
            onSave = { name, category, defaults ->
                if (editing == null) vm.create(name, category, defaults) else
                    vm.update(editing!!.copy(name = name, category = category, defaultParamsJson = defaults))
                dialogOpen = false
            }
        )
    }
}

@Composable
private fun TemplateEditDialog(
    initial: ComponentTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String?, defaultParamsJson: String?) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue(initial?.name ?: "")) }
    var category by remember { mutableStateOf(TextFieldValue(initial?.category ?: "")) }
    var defaults by remember { mutableStateOf(TextFieldValue(initial?.defaultParamsJson ?: "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(name.text, category.text.takeIf { it.isNotBlank() }, defaults.text.takeIf { it.isNotBlank() }) }) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text(if (initial == null) "Новый шаблон" else "Редактирование") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") }, singleLine = true)
                OutlinedTextField(
                    value = defaults,
                    onValueChange = { defaults = it },
                    label = { Text("Параметры по умолчанию (JSON)") },
                    placeholder = { Text("{\"size\":\"4040\",\"brand\":\"Filmtec\"}") },
                    minLines = 3
                )
            }
        }
    )
}
