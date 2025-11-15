package ru.wassertech.ui.templates

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
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
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState


private const val TAG = "TemplatesScreen"

@OptIn(ExperimentalFoundationApi::class)
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
    
    // Сохранение порядка при изменении localOrder в режиме редактирования
    fun onReorderTemplates(fromIndex: Int, toIndex: Int) {
        val mutable = localOrder.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        localOrder = mutable
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
    
    // Словарь для быстрого доступа по id
    val templatesById = remember(visibleTemplates) {
        visibleTemplates.associateBy { it.id }
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

        Box(modifier = Modifier.fillMaxSize()) {
            if (visibleTemplates.isEmpty() && !isEditing) {
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Начните с шаблонов",
                    description = "Создайте шаблон компонента, чтобы определить его поля и характеристики. После этого вы сможете использовать этот шаблон при создании компонентов в установках."
                )
            } else if (isEditing && localOrder.isNotEmpty()) {
                // В режиме редактирования используем ReorderableLazyColumn
                ReorderableLazyColumn(
                    items = localOrder,
                    onMove = { fromIndex, toIndex ->
                        val mutable = localOrder.toMutableList()
                        val item = mutable.removeAt(fromIndex)
                        mutable.add(toIndex, item)
                        localOrder = mutable
                    },
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) { templateId, isDragging, reorderableState ->
                    val template = templatesById[templateId] ?: return@ReorderableLazyColumn
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TemplateRowWrapper(
                            template = template,
                            isEditing = isEditing,
                            onArchive = {
                                scope.launch {
                                    Log.d(TAG, "Архивирование шаблона: id=${template.id}, name=${template.name}")
                                    dao.setArchived(template.id, true)
                                }
                            },
                            onRestore = {
                                scope.launch {
                                    Log.d(TAG, "Разархивирование шаблона: id=${template.id}, name=${template.name}")
                                    dao.setArchived(template.id, false)
                                }
                            },
                            onDelete = {
                                deleteDialogState = template
                            },
                            onClick = {
                                if (!isEditing) {
                                    onOpenTemplate(template.id)
                                }
                            },
                            reorderableState = reorderableState,
                            modifier = Modifier.background(Color.White)
                        )
                    }
                }
            } else {
                // В обычном режиме используем обычный LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visibleTemplates, key = { it.id }) { template ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TemplateRowWrapper(
                                template = template,
                                isEditing = isEditing,
                                onArchive = {
                                    scope.launch {
                                        Log.d(TAG, "Архивирование шаблона: id=${template.id}, name=${template.name}")
                                        dao.setArchived(template.id, true)
                                    }
                                },
                                onRestore = {
                                    scope.launch {
                                        Log.d(TAG, "Разархивирование шаблона: id=${template.id}, name=${template.name}")
                                        dao.setArchived(template.id, false)
                                    }
                                },
                                onDelete = {
                                    deleteDialogState = template
                                },
                                onClick = {
                                    if (!isEditing) {
                                        onOpenTemplate(template.id)
                                    }
                                },
                                reorderableState = null,
                                modifier = Modifier.background(Color.White)
                            )
                        }
                    }
                }
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

// Wrapper для строки шаблона, использующий EntityRowWithMenu
@Composable
private fun TemplateRowWrapper(
    template: ComponentTemplateEntity,
    isEditing: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    reorderableState: ReorderableState?,
    modifier: Modifier = Modifier
) {
    val isArchived = template.isArchived == true

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        EntityRowWithMenu(
            title = template.name,
            subtitle = template.category?.takeIf { it.isNotBlank() },
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ui_template_component),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            },
            trailingIcon = if (!isEditing) {
                {
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                        contentDescription = "Открыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else null,
            isEditMode = isEditing,
            isArchived = isArchived,
            onClick = onClick,
            onRestore = onRestore,
            onArchive = onArchive,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth(),
            reorderableState = reorderableState,
            showDragHandle = isEditing && !isArchived
        )
    }
}
