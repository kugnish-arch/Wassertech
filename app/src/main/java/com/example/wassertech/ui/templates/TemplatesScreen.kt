package com.example.wassertech.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.ui.common.EditDoneBottomBar
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

    // Диалог «Создать шаблон»
    var showCreate by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    // Режим редактирования + локальный порядок для live-перестановки
    var isEditing by remember { mutableStateOf(false) }
    var localOrder by remember(templates) { mutableStateOf(templates.map { it.id }) }

    // Базовый порядок — как в БД (sortOrder ↑, затем title для стабильности)
    val dbOrdered = remember(templates) {
        templates.sortedWith(
            compareBy<ChecklistTemplateEntity> { it.sortOrder ?: Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    // Применяем фильтрацию по архиву и порядок
    // - в обычном режиме: только активные, порядок из БД
    // - в режиме редактирования: все (включая архив), но если локальный порядок непустой — показываем его
    val visibleTemplates = remember(dbOrdered, localOrder, isEditing) {
        val base = if (isEditing) dbOrdered else dbOrdered.filter { it.isArchived != true }
        if (isEditing && localOrder.isNotEmpty()) {
            // наложим локальный порядок, сохраняя элементы, которых вдруг нет в localOrder, в конец
            base.sortedBy { t ->
                val idx = localOrder.indexOf(t.id)
                if (idx == -1) Int.MAX_VALUE else idx
            }
        } else {
            base
        }
    }

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
        },
        bottomBar = {
            EditDoneBottomBar(
                isEditing = isEditing,
                onEdit = {
                    isEditing = true
                    // зафиксировать актуальный порядок из БД в локальный
                    localOrder = dbOrdered.map { it.id }
                },
                onDone = {
                    // сохранить локальный порядок в БД
                    scope.launch {
                        val now = System.currentTimeMillis()
                        localOrder.forEachIndexed { index, id ->
                            dao.updateTemplateOrder(id, index, now)
                        }
                    }
                    isEditing = false
                }
            )
        }
    ) { padding ->
        val layoutDir = LocalLayoutDirection.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 0.dp, // минимальный зазор под сабнавбаром
                bottom = padding.calculateBottomPadding() + 12.dp
            )
        ) {
            items(visibleTemplates, key = { it.id }) { t ->
                ElevatedCard(
                    modifier = Modifier.padding(bottom = 4.dp),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null
                            )
                        },
                        headlineContent = { Text(t.title) },
                        trailingContent = {
                            if (isEditing) {
                                Row {
                                    // Вверх
                                    IconButton(
                                        onClick = {
                                            val i = localOrder.indexOf(t.id)
                                            if (i > 0) {
                                                val m = localOrder.toMutableList()
                                                m[i - 1] = m[i].also { m[i] = m[i - 1] }
                                                localOrder = m
                                            }
                                        }
                                    ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Вверх") }

                                    // Вниз
                                    IconButton(
                                        onClick = {
                                            val i = localOrder.indexOf(t.id)
                                            if (i != -1 && i < localOrder.lastIndex) {
                                                val m = localOrder.toMutableList()
                                                m[i + 1] = m[i].also { m[i] = m[i + 1] }
                                                localOrder = m
                                            }
                                        }
                                    ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Вниз") }

                                    // Архив / Разархивировать
                                    if (t.isArchived == true) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val now = System.currentTimeMillis()
                                                    dao.setTemplateArchived(t.id, false, now, now)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Unarchive,
                                                contentDescription = "Восстановить",
                                                tint = Color(0xFF2E7D32) // зелёный
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val now = System.currentTimeMillis()
                                                    dao.setTemplateArchived(t.id, true, now, now)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Archive,
                                                contentDescription = "Архивировать",
                                                tint = MaterialTheme.colorScheme.error // красный
                                            )
                                        }
                                    }
                                }
                            }
                        },
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
                                val now = System.currentTimeMillis()
                                val nextOrder =
                                    (templates.maxOfOrNull { it.sortOrder ?: -1 } ?: -1) + 1
                                val entity = ChecklistTemplateEntity(
                                    id = id,
                                    title = title,
                                    componentType = ComponentType.FILTER, // дефолт
                                    sortOrder = nextOrder,
                                    isArchived = false,
                                    archivedAtEpoch = null,
                                    updatedAtEpoch = now
                                )
                                dao.upsertTemplate(entity)
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
