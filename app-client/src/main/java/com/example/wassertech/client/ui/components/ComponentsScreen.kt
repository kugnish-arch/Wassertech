package ru.wassertech.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ComponentEntity
import ru.wassertech.client.data.entities.ComponentTemplateEntity
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.client.data.types.ComponentType
import ru.wassertech.client.auth.UserSessionManager
import ru.wassertech.client.auth.OriginType
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditComponent
import ru.wassertech.client.permissions.canDeleteComponent
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.components.ScreenTitleWithSubtitle
import ru.wassertech.core.ui.theme.ClientsRowDivider
import java.util.UUID

/**
 * Экран компонентов установки для app-client.
 * Показывает список компонентов установки с ограничениями по правам доступа.
 * БЕЗ кнопок "Провести ТО" и генерации PDF (только просмотр истории ТО).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    installationId: String,
    onOpenMaintenanceHistory: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val layoutDir = LocalLayoutDirection.current
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { UserSessionManager.getCurrentSession() }
    
    // Получаем данные установки
    val installation by db.hierarchyDao().observeInstallation(installationId).collectAsState(initial = null)
    val installationName = installation?.name ?: "Установка"
    
    // Получаем список компонентов
    val components by db.hierarchyDao().observeComponents(installationId).collectAsState(initial = emptyList())
    
    // Получаем данные объекта и клиента для подзаголовка
    val siteId = installation?.siteId
    val site by remember(siteId) {
        if (siteId != null) {
            db.hierarchyDao().observeSite(siteId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)
    
    val clientId = site?.effectiveOwnerClientId() ?: site?.clientId
    val client by remember(clientId) {
        if (clientId != null) {
            db.clientDao().observeClientRaw(clientId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val clientName = client.firstOrNull()?.name
    
    // Формируем подзаголовок
    val siteSubtitle: String? = when {
        clientName != null && site?.name != null -> "Объект: $clientName — ${site!!.name}"
        site?.name != null -> "Объект: ${site!!.name}"
        else -> null
    }
    
    // Получаем шаблоны компонентов для отображения названий
    val templates by db.componentTemplatesDao().observeAll().collectAsState(initial = emptyList())
    val templateTitleById = remember(templates) {
        templates.associate { it.id to it.name }
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newComponentName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<ComponentTemplateEntity?>(null) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    
    var deleteComponentId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (currentUser != null && canCreateEntity(currentUser) && installation != null) {
                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                        newComponentName = ""
                        selectedTemplate = templates.firstOrNull()
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить компонент")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDir),
                    end = padding.calculateEndPadding(layoutDir),
                    top = 0.dp,
                    bottom = padding.calculateBottomPadding()
                )
        ) {
            // Заголовок установки
            Column(modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                    ),
                    shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
                ) {
                    Column(Modifier.padding(ru.wassertech.core.ui.theme.HeaderCardStyle.padding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.equipment_filter_triple),
                                contentDescription = null,
                                modifier = Modifier.size(ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(8.dp))
                            ScreenTitleWithSubtitle(
                                title = installationName,
                                subtitle = siteSubtitle,
                                titleStyle = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                                subtitleStyle = MaterialTheme.typography.bodySmall,
                                titleColor = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor,
                                subtitleColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }
            
            // Кнопка просмотра истории ТО (без кнопки "Провести ТО")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onOpenMaintenanceHistory(installationId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = installation != null
            ) {
                Text("История ТО")
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Список компонентов
            if (components.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Нет компонентов",
                    description = "Нажмите кнопку «+», чтобы добавить компонент к этой установке."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(components, key = { it.id }) { component ->
                        val templateTitle = component.templateId?.let { templateTitleById[it] } ?: "Без шаблона"
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ComponentRow(
                                component = component,
                                templateTitle = templateTitle,
                                onDelete = if (currentUser != null && canDeleteComponent(currentUser, component)) {
                                    { deleteComponentId = component.id }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между компонентами (кроме последнего)
                            val index = components.indexOf(component)
                            if (index >= 0 && index < components.size - 1) {
                                HorizontalDivider(
                                    color = ClientsRowDivider,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог добавления компонента
    if (showAddDialog && installation != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newComponentName,
                        onValueChange = { newComponentName = it },
                        label = { Text("Название (опционально)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Выбор шаблона
                    ExposedDropdownMenuBox(
                        expanded = templateMenuExpanded,
                        onExpandedChange = { templateMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.name ?: if (templates.isEmpty()) "Нет шаблонов" else "Выберите шаблон",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Шаблон") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = templateMenuExpanded,
                            onDismissRequest = { templateMenuExpanded = false },
                            modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                        ) {
                            if (templates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Шаблонов нет") },
                                    onClick = { templateMenuExpanded = false }
                                )
                            } else {
                                templates.forEach { template ->
                                    DropdownMenuItem(
                                        text = { Text(template.name) },
                                        onClick = {
                                            selectedTemplate = template
                                            templateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tmpl = selectedTemplate
                        val compName = if (newComponentName.isNotBlank()) {
                            newComponentName.trim()
                        } else {
                            tmpl?.name ?: "Компонент"
                        }
                        if (tmpl != null || templates.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                val newComponent = ComponentEntity(
                                    id = UUID.randomUUID().toString(),
                                    installationId = installationId,
                                    name = compName,
                                    type = ComponentType.COMMON,
                                    orderIndex = components.size,
                                    templateId = tmpl?.id,
                                    ownerClientId = currentUser?.clientId,
                                    origin = OriginType.CLIENT.name
                                )
                                db.hierarchyDao().upsertComponent(newComponent)
                            }
                            showAddDialog = false
                            newComponentName = ""
                            selectedTemplate = null
                        }
                    },
                    enabled = selectedTemplate != null || templates.isNotEmpty()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог подтверждения удаления компонента
    deleteComponentId?.let { componentId ->
        AlertDialog(
            onDismissRequest = { deleteComponentId = null },
            title = { Text("Удалить компонент?") },
            text = {
                Text("Вы уверены, что хотите удалить этот компонент? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteComponent(componentId)
                        }
                        deleteComponentId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteComponentId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ComponentRow(
    component: ComponentEntity,
    templateTitle: String,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EntityRowWithMenu(
        title = component.name,
        subtitle = templateTitle,
        leadingIcon = {
            Image(
                painter = painterResource(id = R.drawable.ui_gear),
                contentDescription = "Компонент",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
        },
        trailingIcon = null,
        isEditMode = false,
        isArchived = false, // TODO: ComponentEntity не имеет поля isArchived, добавить после миграции БД
        onClick = null, // Компоненты не кликабельны
        onRestore = null,
        onArchive = null,
        onDelete = onDelete,
        onEdit = null,
        onMoveToGroup = null,
        availableGroups = emptyList(),
        modifier = modifier,
        reorderableState = null,
        showDragHandle = false
    )
}

