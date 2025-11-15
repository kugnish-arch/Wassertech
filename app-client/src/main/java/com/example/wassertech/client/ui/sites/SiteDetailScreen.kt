package ru.wassertech.client.ui.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.client.auth.UserSessionManager
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditSite
import ru.wassertech.client.permissions.canDeleteSite
import ru.wassertech.client.permissions.canEditInstallation
import ru.wassertech.client.permissions.canDeleteInstallation
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.theme.ClientsRowDivider
import java.util.UUID

/**
 * Экран деталей объекта для app-client.
 * Показывает список установок внутри объекта с ограничениями по правам доступа.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val layoutDir = LocalLayoutDirection.current
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { UserSessionManager.getCurrentSession() }
    
    // Получаем данные объекта
    val site by db.hierarchyDao().observeSite(siteId).collectAsState(initial = null)
    val siteName = site?.name ?: "Объект"
    
    // Получаем список установок
    val installations by db.hierarchyDao().observeInstallations(siteId).collectAsState(initial = emptyList())
    
    // Получаем информацию о клиенте для определения типа
    val clientId = site?.effectiveOwnerClientId() ?: site?.clientId
    val client by remember(clientId) {
        if (clientId != null) {
            db.clientDao().observeClientRaw(clientId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val isCorporate = client.firstOrNull()?.isCorporate ?: false
    val isSiteArchived = site?.isArchived == true
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newInstallationName by remember { mutableStateOf("") }
    
    var editSiteId by remember { mutableStateOf<String?>(null) }
    var editSiteName by remember { mutableStateOf("") }
    var editSiteAddress by remember { mutableStateOf("") }
    
    var deleteInstallationId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (currentUser != null && canCreateEntity(currentUser) && site != null) {
                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                        newInstallationName = ""
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить установку")
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
            // Заголовок объекта
            Column(modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                    ),
                    shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ru.wassertech.core.ui.theme.HeaderCardStyle.padding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val siteIconRes = when {
                            isSiteArchived && isCorporate -> R.drawable.object_factory_red
                            isSiteArchived && !isCorporate -> R.drawable.object_house_red
                            isCorporate -> R.drawable.object_factory_blue
                            else -> R.drawable.object_house_blue
                        }
                        Image(
                            painter = painterResource(id = siteIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            siteName,
                            style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                            color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                        )
                        Spacer(Modifier.weight(1f))
                        // Кнопка редактирования объекта (только если можно редактировать)
                        val siteForEdit = site
                        if (currentUser != null && siteForEdit != null && canEditSite(currentUser, siteForEdit)) {
                            IconButton(onClick = {
                                editSiteId = siteForEdit.id
                                editSiteName = siteForEdit.name
                                editSiteAddress = siteForEdit.address ?: ""
                            }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Редактировать объект",
                                    tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }
            
            // Список установок
            if (installations.isEmpty()) {
                EmptyGroupPlaceholder(
                    text = "У этого объекта пока нет установок",
                    indent = 16.dp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(installations, key = { it.id }) { installation ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InstallationRow(
                                installation = installation,
                                onClick = { onOpenInstallation(installation.id) },
                                onEdit = if (currentUser != null && canEditInstallation(currentUser, installation)) {
                                    { /* TODO: Реализовать редактирование установки */ }
                                } else null,
                                onDelete = if (currentUser != null && canDeleteInstallation(currentUser, installation)) {
                                    { deleteInstallationId = installation.id }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между установками (кроме последней)
                            val index = installations.indexOf(installation)
                            if (index >= 0 && index < installations.size - 1) {
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
    
    // Диалог добавления установки
    if (showAddDialog && site != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая установка") },
            text = {
                OutlinedTextField(
                    value = newInstallationName,
                    onValueChange = { newInstallationName = it },
                    label = { Text("Название установки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newInstallationName.trim()
                        val siteForCreation = site
                        if (name.isNotEmpty() && siteForCreation != null) {
                            scope.launch(Dispatchers.IO) {
                                val newInstallation = InstallationEntity(
                                    id = UUID.randomUUID().toString(),
                                    siteId = siteForCreation.id,
                                    name = name,
                                    createdAtEpoch = System.currentTimeMillis(),
                                    updatedAtEpoch = System.currentTimeMillis(),
                                    isArchived = false,
                                    dirtyFlag = true, // Помечаем как созданную локально
                                    syncStatus = 1, // QUEUED
                                    orderIndex = installations.size,
                                    ownerClientId = currentUser?.clientId,
                                    origin = ru.wassertech.client.auth.OriginType.CLIENT.name
                                )
                                db.hierarchyDao().upsertInstallation(newInstallation)
                            }
                            showAddDialog = false
                            newInstallationName = ""
                        }
                    },
                    enabled = newInstallationName.trim().isNotEmpty() && site != null
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
    
    // Диалог редактирования объекта
    editSiteId?.let { id ->
        val siteToEdit = site
        if (siteToEdit != null) {
            AlertDialog(
                onDismissRequest = { editSiteId = null },
                title = { Text("Редактировать объект") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSiteName,
                            onValueChange = { editSiteName = it },
                            label = { Text("Название") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editSiteAddress,
                            onValueChange = { editSiteAddress = it },
                            label = { Text("Адрес (опционально)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = editSiteName.trim()
                            if (name.isNotEmpty()) {
                                scope.launch(Dispatchers.IO) {
                                    val updatedSite = siteToEdit.copy(
                                        name = name,
                                        address = editSiteAddress.trim().takeIf { it.isNotEmpty() }
                                    )
                                    db.hierarchyDao().upsertSite(updatedSite)
                                }
                                editSiteId = null
                            }
                        },
                        enabled = editSiteName.trim().isNotEmpty()
                    ) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editSiteId = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
    
    // Диалог подтверждения удаления установки
    deleteInstallationId?.let { installationId ->
        AlertDialog(
            onDismissRequest = { deleteInstallationId = null },
            title = { Text("Удалить установку?") },
            text = {
                Text("Вы уверены, что хотите удалить эту установку? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteInstallation(installationId)
                        }
                        deleteInstallationId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteInstallationId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun InstallationRow(
    installation: InstallationEntity,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iconRes = R.drawable.equipment_filter_triple
    
    EntityRowWithMenu(
        title = installation.name,
        subtitle = null,
        leadingIcon = {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Установка",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
        },
        trailingIcon = {
            Icon(
                imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        isEditMode = false,
        isArchived = installation.isArchived == true,
        onClick = onClick,
        onRestore = null,
        onArchive = null,
        onDelete = onDelete,
        onEdit = onEdit,
        onMoveToGroup = null,
        availableGroups = emptyList(),
        modifier = modifier,
        reorderableState = null,
        showDragHandle = false
    )
}

