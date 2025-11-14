package ru.wassertech.ui.templates

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.sync.SafeDeletionHelper
import ru.wassertech.sync.markCreatedForSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.CommonAddDialog


private const val TAG = "TemplatesScreen"

@Composable
fun TemplatesScreen(
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onOpenTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.componentTemplatesDao() }
    val scope = rememberCoroutineScope()

    val templatesFlow: Flow<List<ComponentTemplateEntity>> =
        remember { dao.observeAll() }
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
                compareBy<ComponentTemplateEntity> { it.sortOrder }
                    .thenBy { it.name.lowercase() }
            )
            localOrder = allTemplatesOrdered.map { it.id }
        }
    }
    
    // Сохранение порядка при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty()) {
            // сохранить локальный порядок в БД
            scope.launch {
                Log.d(TAG, "Сохранение порядка шаблонов: количество=${localOrder.size}")
                localOrder.forEachIndexed { index, id ->
                    dao.setSortOrder(id, index)
                }
            }
        }
    }

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<ComponentTemplateEntity?>(null) }

    // Базовый порядок — как в БД (sortOrder ↑, затем name для стабильности)
    val dbOrdered = remember(templates) {
        templates.sortedWith(
            compareBy<ComponentTemplateEntity> { it.sortOrder }
                .thenBy { it.name.lowercase() }
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
                            Log.d(TAG, "Архивирование шаблона: id=${t.id}, name=${t.name}")
                            dao.setArchived(t.id, true)
                        }
                    },
                    onRestore = {
                        scope.launch {
                            Log.d(TAG, "Разархивирование шаблона: id=${t.id}, name=${t.name}")
                            dao.setArchived(t.id, false)
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
                        val nextOrder =
                            (templates.maxOfOrNull { it.sortOrder } ?: -1) + 1
                        val entity = ComponentTemplateEntity(
                            id = id,
                            name = title,
                            category = null,
                            defaultParamsJson = null,
                            sortOrder = nextOrder
                        ).markCreatedForSync()
                        Log.d(TAG, "Создание шаблона: id=$id, name=$title, " +
                                "dirtyFlag=${entity.dirtyFlag}, syncStatus=${entity.syncStatus}, " +
                                "createdAtEpoch=${entity.createdAtEpoch}, updatedAtEpoch=${entity.updatedAtEpoch}")
                        dao.upsert(entity)
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
        var installationsUsingTemplate by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        
        LaunchedEffect(template.id) {
            withContext(Dispatchers.IO) {
                val components = db.hierarchyDao().getComponentsByTemplate(template.id)
                val installationIds = components.map { it.installationId }.distinct()
                val installations = installationIds.mapNotNull { id ->
                    db.hierarchyDao().getInstallationNow(id)
                }
                installationsUsingTemplate = installations.map { "${it.name} (ID: ${it.id})" }
                isLoading = false
            }
        }
        
        if (isLoading) {
            AlertDialog(
                onDismissRequest = { deleteDialogState = null },
                title = { Text("Проверка использования") },
                text = {
                    Column {
                        Text("Проверяем использование шаблона...")
                        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { deleteDialogState = null }) {
                        Text("Отмена")
                    }
                }
            )
        } else if (installationsUsingTemplate.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { deleteDialogState = null },
                title = { Text("Невозможно удалить шаблон") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Шаблон \"${template.name}\" используется в следующих установках:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        installationsUsingTemplate.forEach { installationName ->
                            Text(
                                "• $installationName",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Сначала удалите компоненты из этих установок, затем можно будет удалить шаблон.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { deleteDialogState = null }) {
                        Text("Понятно")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { deleteDialogState = null },
                title = { Text("Подтверждение удаления") },
                text = {
                    Text(
                        "Вы уверены, что хотите удалить шаблон \"${template.name}\"?\n\nЭто действие нельзя отменить."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    SafeDeletionHelper.deleteComponentTemplate(db, template.id)
                                    deleteDialogState = null
                                } catch (e: IllegalStateException) {
                                    // Ошибка уже обработана выше
                                    Log.e(TAG, "Ошибка удаления шаблона", e)
                                }
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
}

// Компонент для строки шаблона с drag-and-drop
@Composable
private fun TemplateRowWithDrag(
    template: ComponentTemplateEntity,
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
    var dragOffset by remember { mutableStateOf(0.dp) }
    var isDragging by remember { mutableStateOf(false) }
    val isArchived = template.isArchived == true
    val density = LocalDensity.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .then(
                if (isEditing && !isArchived) {
                    Modifier
                        .offset(y = dragOffset)
                        .zIndex(if (isDragging) 1f else 0f) // Перетаскиваемый шаблон поверх всех
                        .pointerInput(template.id, index, density) {
                            detectDragGestures(
                                onDragStart = {
                                    lastMoveThreshold = 0f
                                    dragOffset = 0.dp
                                    isDragging = true
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Обновляем визуальное смещение элемента (dragAmount.y в пикселях, преобразуем в dp)
                                    dragOffset += with(density) { dragAmount.y.toDp() }
                                    
                                    // Еще больше уменьшаем порог для физических устройств
                                    val threshold = 10f
                                    if (dragAmount.y < -threshold && lastMoveThreshold >= -threshold) {
                                        onMoveUp()
                                        lastMoveThreshold = -threshold
                                        dragOffset = 0.dp // Сбрасываем смещение после перемещения
                                    } else if (dragAmount.y > threshold && lastMoveThreshold <= threshold) {
                                        onMoveDown()
                                        lastMoveThreshold = threshold
                                        dragOffset = 0.dp // Сбрасываем смещение после перемещения
                                    }
                                    if (dragAmount.y in -threshold..threshold) {
                                        lastMoveThreshold = dragAmount.y
                                    }
                                },
                                onDragEnd = {
                                    lastMoveThreshold = 0f
                                    dragOffset = 0.dp
                                    isDragging = false
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ручка для перетаскивания (показываем вместе с иконкой в режиме редактирования)
                    if (isEditing && !isArchived) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Перетащить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Иконка шаблона (всегда показываем)
                    Image(
                        painter = painterResource(id = R.drawable.ui_template_component),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            headlineContent = {
                Text(
                    template.name,
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
                } else {
                    // Иконка шеврона справа в обычном режиме
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                        contentDescription = "Открыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            modifier = Modifier.clickable(enabled = !isEditing) { onClick() }
        )
    }
}
