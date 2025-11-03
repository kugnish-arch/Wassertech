package com.example.wassertech.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TemplatesScreen(
    onOpenTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.templatesDao() }
    val scope = rememberCoroutineScope()

    val templatesFlow: Flow<List<ChecklistTemplateEntity>> =
        remember { dao.observeAllTemplates() }
    val templates by templatesFlow.collectAsState(initial = emptyList())

    // состояние диалога «Создать шаблон»
    var showCreate by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newTitle = ""
                    showCreate = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Шаблон") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 12.dp
            )
        ) {
            items(templates, key = { it.id }) { t ->
                ElevatedCard(
                    modifier = Modifier.padding(bottom = 4.dp),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    ListItem(
                        headlineContent = { Text(t.title) },
                        supportingContent = { Text(t.componentType.name) },
                        modifier = Modifier.clickable { onOpenTemplate(t.id) }
                    )
                }
            }
        }
    }

    // Диалог создания шаблона
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Новый шаблон") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Название шаблона") }
                )
            },
            confirmButton = {
                val canSave = newTitle.trim().isNotEmpty()
                TextButton(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            scope.launch {
                                val id = UUID.randomUUID().toString()
                                val entity = ChecklistTemplateEntity(
                                    id = id,
                                    title = title,
                                    componentType = ComponentType.FILTER // можешь поменять дефолтный тип
                                )
                                dao.upsertTemplate(entity) // suspend-вызов внутри корутины
                                onOpenTemplate(id)
                            }
                            showCreate = false
                        }
                    },
                    enabled = canSave
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Отмена") }
            }
        )
    }
}
