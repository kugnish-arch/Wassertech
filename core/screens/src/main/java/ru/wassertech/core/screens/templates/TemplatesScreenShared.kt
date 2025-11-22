package ru.wassertech.core.screens.templates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.screens.templates.ui.TemplatesUiState
import ru.wassertech.core.screens.templates.ui.TemplateItemUi

/**
 * Shared-экран для отображения списка шаблонов компонентов.
 * Используется как в app-crm, так и в app-client.
 * 
 * @param state UI State с данными шаблонов
 * @param onTemplateClick Коллбек при клике на шаблон
 * @param onCreateTemplateClick Коллбек при клике на создание шаблона (старая сигнатура для обратной совместимости)
 * @param onCreateTemplateWithCategory Коллбек при создании шаблона с указанной категорией (принимает category: String - "COMPONENT" или "SENSOR"). Если null, используется обычная FAB.
 * @param onTemplateArchive Коллбек для архивации шаблона
 * @param onTemplateRestore Коллбек для восстановления шаблона
 * @param onTemplateDelete Коллбек для удаления шаблона
 * @param onTemplatesReordered Коллбек при изменении порядка шаблонов (drag-and-drop)
 * @param onToggleEdit Коллбек для переключения режима редактирования
 * @param showCreateDialog Флаг показа диалога создания шаблона
 * @param onCreateDialogDismiss Коллбек для закрытия диалога создания
 * @param onCreateDialogConfirm Коллбек для подтверждения создания (принимает имя шаблона)
 * @param showDeleteDialog Флаг показа диалога удаления
 * @param deleteDialogTemplate Шаблон для удаления (если null, диалог не показывается)
 * @param onDeleteDialogDismiss Коллбек для закрытия диалога удаления
 * @param onDeleteDialogConfirm Коллбек для подтверждения удаления
 * @param installationsUsingTemplate Список установок, использующих шаблон (для диалога удаления)
 * @param isLoadingDeleteCheck Флаг загрузки проверки использования шаблона
 * @param externalPaddingValues Внешние отступы (например, от bottomBar), используемые для позиционирования FAB
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesScreenShared(
    state: TemplatesUiState,
    onTemplateClick: (String) -> Unit,
    onCreateTemplateClick: () -> Unit = {},
    onCreateTemplateWithCategory: ((String) -> Unit)? = null, // category: "COMPONENT" или "SENSOR"
    onTemplateArchive: (String) -> Unit = {},
    onTemplateRestore: (String) -> Unit = {},
    onTemplateDelete: (String) -> Unit = {},
    onTemplatesReordered: ((List<String>) -> Unit)? = null,
    onToggleEdit: (() -> Unit)? = null,
    showCreateDialog: Boolean = false,
    onCreateDialogDismiss: () -> Unit = {},
    onCreateDialogConfirm: (String) -> Unit = {},
    showDeleteDialog: Boolean = false,
    deleteDialogTemplate: TemplateItemUi? = null,
    onDeleteDialogDismiss: () -> Unit = {},
    onDeleteDialogConfirm: () -> Unit = {},
    installationsUsingTemplate: List<String> = emptyList(),
    isLoadingDeleteCheck: Boolean = false,
    externalPaddingValues: PaddingValues? = null
) {
    var newTitle by remember { mutableStateOf("") }
    
    // Словарь для быстрого доступа по id
    val templatesById = remember(state.templates) {
        state.templates.associateBy { it.id }
    }
    
    // Видимые шаблоны с учетом архива и порядка
    val visibleTemplates = remember(state.templates, state.localOrder, state.isEditing) {
        val base = if (state.isEditing) {
            state.templates
        } else {
            state.templates.filter { !it.isArchived }
        }
        if (state.isEditing && state.localOrder.isNotEmpty()) {
            base.sortedBy { t ->
                val idx = state.localOrder.indexOf(t.id)
                if (idx == -1) Int.MAX_VALUE else idx
            }
        } else {
            base
        }
    }

    // Вычисляем отступы для контента и FAB с учетом внешних отступов
    // В app-client externalPaddingValues содержит отступы от AppScaffold (topBar и bottomBar)
    // В app-crm externalPaddingValues может быть null, так как padding применяется к NavHost
    val topPadding = externalPaddingValues?.calculateTopPadding() ?: 0.dp
    val bottomPadding = externalPaddingValues?.calculateBottomPadding() ?: 0.dp
    val fabBottomPadding = bottomPadding + 16.dp
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!state.isEditing) {
                // Используем ExpandableFAB, если есть onCreateTemplateWithCategory, иначе обычную FAB
                if (onCreateTemplateWithCategory != null) {
                    ExpandableFAB(
                        template = ExpandableFABTemplate(
                            icon = Icons.Filled.Add,
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White,
                            onClick = {
                                // Если используется ExpandableFAB, основной onClick не используется
                            },
                            options = listOf(
                                ExpandableFABOption(
                                    label = "Компонент",
                                    icon = Icons.Filled.Settings,
                                    onClick = {
                                        onCreateTemplateWithCategory("COMPONENT")
                                    }
                                ),
                                ExpandableFABOption(
                                    label = "Датчик",
                                    icon = Icons.Filled.Science,
                                    onClick = {
                                        onCreateTemplateWithCategory("SENSOR")
                                    }
                                )
                            ),
                            optionsColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.padding(bottom = fabBottomPadding)
                    )
                } else {
                    // Обратная совместимость: обычная FAB
                    FloatingActionButton(
                        onClick = {
                            newTitle = ""
                            onCreateTemplateClick()
                        },
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = fabBottomPadding)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить шаблон")
                    }
                }
            }
        }
    ) { padding ->
        val layoutDir = LocalLayoutDirection.current

        Box(modifier = Modifier.fillMaxSize()) {
            if (visibleTemplates.isEmpty() && !state.isEditing) {
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Начните с шаблонов",
                    description = "Создайте шаблон компонента, чтобы определить его поля и характеристики. После этого вы сможете использовать этот шаблон при создании компонентов в установках."
                )
            } else if (visibleTemplates.isNotEmpty()) {
                // Используем localOrder если он не пустой, иначе используем порядок из visibleTemplates
                val orderToUse = if (state.localOrder.isNotEmpty() && state.localOrder.all { it in templatesById }) {
                    state.localOrder
                } else {
                    visibleTemplates.map { it.id }
                }
                
                ReorderableLazyColumn(
                    items = orderToUse,
                    onMove = { fromIndex, toIndex ->
                        val mutable = orderToUse.toMutableList()
                        val item = mutable.removeAt(fromIndex)
                        mutable.add(toIndex, item)
                        onTemplatesReordered?.invoke(mutable)
                    },
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = topPadding + 8.dp, // Используем externalPaddingValues для topPadding
                        bottom = bottomPadding + 80.dp // Используем externalPaddingValues для bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) { templateId, isDragging, reorderableState ->
                    val template = templatesById[templateId] ?: return@ReorderableLazyColumn
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TemplateRowWrapper(
                            template = template,
                            isEditing = state.isEditing,
                            onArchive = { onTemplateArchive(template.id) },
                            onRestore = { onTemplateRestore(template.id) },
                            onDelete = { onTemplateDelete(template.id) },
                            onClick = {
                                if (!state.isEditing) {
                                    onTemplateClick(template.id)
                                }
                            },
                            reorderableState = reorderableState,
                            isDragging = isDragging,
                            onToggleEdit = onToggleEdit,
                            modifier = Modifier.background(Color.White)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppEmptyState(
                        icon = Icons.Filled.Lightbulb,
                        title = "Начните с шаблонов",
                        description = "Создайте шаблон компонента, чтобы определить его поля и характеристики. После этого вы сможете использовать этот шаблон при создании компонентов в установках."
                    )
                }
            }
        }
    }

    // Диалог создания шаблона
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = onCreateDialogDismiss,
            title = { Text("Новый шаблон") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Название шаблона") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            onCreateDialogConfirm(title)
                            newTitle = ""
                        }
                    },
                    enabled = newTitle.trim().isNotEmpty()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = onCreateDialogDismiss) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подтверждения удаления
    deleteDialogTemplate?.let { template ->
        if (isLoadingDeleteCheck) {
            AlertDialog(
                onDismissRequest = onDeleteDialogDismiss,
                title = { Text("Проверка использования") },
                text = {
                    Column {
                        Text("Проверяем использование шаблона...")
                        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDeleteDialogDismiss) {
                        Text("Отмена")
                    }
                }
            )
        } else if (installationsUsingTemplate.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = onDeleteDialogDismiss,
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
                    TextButton(onClick = onDeleteDialogDismiss) {
                        Text("Понятно")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = onDeleteDialogDismiss,
                title = { Text("Подтверждение удаления") },
                text = {
                    Text(
                        "Вы уверены, что хотите удалить шаблон \"${template.name}\"?\n\nЭто действие нельзя отменить."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onDeleteDialogConfirm,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDeleteDialogDismiss) {
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
    template: TemplateItemUi,
    isEditing: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    reorderableState: ReorderableState?,
    isDragging: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            isArchived = template.isArchived,
            onClick = onClick,
            onRestore = onRestore,
            onArchive = onArchive,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth(),
            reorderableState = reorderableState,
            showDragHandle = isEditing && !template.isArchived,
            isDragging = isDragging,
            onToggleEdit = onToggleEdit
        )
    }
}

