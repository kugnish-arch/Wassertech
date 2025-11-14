package ru.wassertech.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ChecklistTemplateEntity
import ru.wassertech.data.types.ComponentType
import ru.wassertech.sync.SafeDeletionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.CommonAddDialog


@Composable
fun TemplatesScreen(
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
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

    // Локальный порядок для live-перестановки
    var localOrder by remember(templates, isEditing) { 
        mutableStateOf(templates.map { it.id }) 
    }
    
    // Синхронизируем локальный порядок при изменении состояния редактирования
    LaunchedEffect(isEditing, templates) {
        if (!isEditing) {
            localOrder = templates.map { it.id }
        } else {
            // При входе в режим редактирования фиксируем текущий порядок
            val allTemplatesOrdered = templates.sortedWith(
                compareBy<ChecklistTemplateEntity> { it.sortOrder ?: Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            localOrder = allTemplatesOrdered.map { it.id }
        }
    }
    
    // Сохранение порядка при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty()) {
            // сохранить локальный порядок в БД
            scope.launch {
                val now = System.currentTimeMillis()
                localOrder.forEachIndexed { index, id ->
                    dao.updateTemplateOrder(id, index, now)
                }
            }
        }
    }

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем системные отступы
        floatingActionButton = {
            AppFloatingActionButton(
                template = FABTemplate(
                    icon = Icons.Filled.Add,
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    onClick = {
                        newTitle = ""
                        showCreate = true
                    }
                )
            )
        }
        // Ботомбар убран - используется переключатель в топбаре
    ) { padding ->
        val layoutDir = LocalLayoutDirection.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp, // стандартный отступ от заголовка до контента
                bottom = padding.calculateBottomPadding() + 12.dp
            )
        ) {
            items(visibleTemplates, key = { it.id }) { t ->
                // Используем индекс из visibleTemplates, если шаблон не найден в localOrder
                val orderIndex = localOrder.indexOf(t.id)
                val index = if (orderIndex >= 0) orderIndex else visibleTemplates.indexOf(t)
                TemplateRowWithDrag(
                    template = t,
                    index = index,
                    isEditing = isEditing,
                    onMoveUp = {
                        val i = localOrder.indexOf(t.id)
                        if (i > 0) {
                            val m = localOrder.toMutableList()
                            m[i - 1] = m[i].also { m[i] = m[i - 1] }
                            localOrder = m
                        }
                    },
                    onMoveDown = {
                        val i = localOrder.indexOf(t.id)
                        if (i != -1 && i < localOrder.lastIndex) {
                            val m = localOrder.toMutableList()
                            m[i + 1] = m[i].also { m[i] = m[i + 1] }
                            localOrder = m
                        }
                    },
                    onArchive = {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            dao.setTemplateArchived(t.id, true, now, now)
                        }
                    },
                    onRestore = {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            dao.setTemplateArchived(t.id, false, now, now)
                        }
                    },
                    onDelete = {
                        deleteDialogState = t
                    },
                    onClick = {
                        if (!isEditing) {
                            onOpenTemplate(t.id)
                        }
                    }
                )
            }
        }
    }

    // Диалог создания шаблона
    if (showCreate) {
        CommonAddDialog(
            title = "Новый шаблон",
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Название шаблона") }
                )
            },
            onDismissRequest = { showCreate = false },
            confirmText = "Создать",
            dismissText = "Отмена",
            confirmEnabled = newTitle.trim().isNotEmpty(),
            onConfirm = {
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
                            componentType = ComponentType.COMMON, // дефолт
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
            onDismiss = { showCreate = false }
        )
    }

    // Диалог подтверждения удаления
    deleteDialogState?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteDialogState = null },
            title = { Text("Подтверждение удаления") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить шаблон \"${template.title}\"?\n\nЭто действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            SafeDeletionHelper.deleteTemplate(db, template.id)
                            deleteDialogState = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogState = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// Компонент для строки шаблона с drag-and-drop
@Composable
private fun TemplateRowWithDrag(
    template: ChecklistTemplateEntity,
    index: Int,
    isEditing: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var lastMoveThreshold by remember { mutableStateOf(0f) }
    val isArchived = template.isArchived == true

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .then(
                if (isEditing && !isArchived) {
                    Modifier.pointerInput(template.id, index) {
                        detectDragGestures(
                            onDragStart = {
                                lastMoveThreshold = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Еще больше уменьшаем порог для физических устройств
                                val threshold = 10f
                                if (dragAmount.y < -threshold && lastMoveThreshold >= -threshold) {
                                    onMoveUp()
                                    lastMoveThreshold = -threshold
                                } else if (dragAmount.y > threshold && lastMoveThreshold <= threshold) {
                                    onMoveDown()
                                    lastMoveThreshold = threshold
                                }
                                if (dragAmount.y in -threshold..threshold) {
                                    lastMoveThreshold = dragAmount.y
                                }
                            },
                            onDragEnd = {
                                lastMoveThreshold = 0f
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.elevatedCardColors()
    ) {
        ListItem(
            leadingContent = {
                if (isEditing && !isArchived) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Перетащить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ui_template_component),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            headlineContent = {
                Text(
                    template.title,
                    color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
            },
            trailingContent = {
                if (isEditing) {
                    Row {
                        // Архив / Разархивировать
                        if (isArchived) {
                            IconButton(
                                onClick = onRestore
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Unarchive,
                                    contentDescription = "Восстановить",
                                    tint = Color(0xFF2E7D32) // зелёный
                                )
                            }
                            // Кнопка удаления для заархивированных
                            IconButton(
                                onClick = onDelete
                            ) {
                                Icon(
                                    imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onArchive
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
            modifier = Modifier.clickable(enabled = !isEditing) { onClick() }
        )
    }
}
