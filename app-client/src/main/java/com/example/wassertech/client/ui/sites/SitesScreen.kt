package ru.wassertech.client.ui.sites

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.client.auth.UserSessionManager
import ru.wassertech.client.auth.OriginType
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditSite
import ru.wassertech.client.permissions.canDeleteSite
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.theme.ClientsRowDivider
// Временно используем стандартные Material3 компоненты
// TODO: Вынести AppFloatingActionButton и CommonAddDialog в core:ui или создать локальные версии
import java.util.UUID

/**
 * Экран списка объектов для app-client.
 * Показывает объекты текущего клиента (как созданные инженером, так и самим клиентом).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    clientId: String,
    onOpenSite: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { UserSessionManager.getCurrentSession() }
    
    // Получаем список объектов для текущего клиента
    val sites by db.hierarchyDao().observeSites(clientId).collectAsState(initial = emptyList())
    
    // Получаем информацию о клиенте для отображения в заголовке
    val client by remember(clientId) {
        db.clientDao().observeClientRaw(clientId)
    }.collectAsState(initial = emptyList())
    val clientName = client.firstOrNull()?.name ?: "Мои объекты"
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newSiteName by remember { mutableStateOf("") }
    var newSiteAddress by remember { mutableStateOf("") }
    
    var editSiteId by remember { mutableStateOf<String?>(null) }
    var editSiteName by remember { mutableStateOf("") }
    var editSiteAddress by remember { mutableStateOf("") }
    
    var deleteSiteId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (currentUser != null && canCreateEntity(currentUser)) {
                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                        newSiteName = ""
                        newSiteAddress = ""
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить объект")
                }
            }
        }
    ) { padding ->
        val layoutDir = LocalLayoutDirection.current
        
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
            // Заголовок экрана
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
                        Text(
                            text = clientName,
                            style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                            color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }
            
            // Список объектов
            if (sites.isEmpty()) {
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Нет объектов",
                    description = "Создайте объект, чтобы добавить установки и компоненты."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(sites, key = { it.id }) { site ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SiteRow(
                                site = site,
                                onClick = { onOpenSite(site.id) },
                                onEdit = if (currentUser != null && canEditSite(currentUser, site)) {
                                    {
                                        editSiteId = site.id
                                        editSiteName = site.name
                                        editSiteAddress = site.address ?: ""
                                    }
                                } else null,
                                onDelete = if (currentUser != null && canDeleteSite(currentUser, site)) {
                                    { deleteSiteId = site.id }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между объектами (кроме последнего)
                            val index = sites.indexOf(site)
                            if (index >= 0 && index < sites.size - 1) {
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
    
    // Диалог добавления объекта
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSiteName,
                        onValueChange = { newSiteName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSiteAddress,
                        onValueChange = { newSiteAddress = it },
                        label = { Text("Адрес (опционально)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                    val name = newSiteName.trim()
                    if (name.isNotEmpty() && currentUser != null) {
                        scope.launch(Dispatchers.IO) {
                            val newSite = SiteEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = clientId,
                                name = name,
                                address = newSiteAddress.trim().takeIf { it.isNotEmpty() },
                                orderIndex = sites.size,
                                isArchived = false,
                                archivedAtEpoch = null,
                                ownerClientId = currentUser.clientId,
                                origin = OriginType.CLIENT.name
                            )
                            db.hierarchyDao().upsertSite(newSite)
                        }
                            showAddDialog = false
                            newSiteName = ""
                            newSiteAddress = ""
                        }
                    },
                    enabled = newSiteName.trim().isNotEmpty()
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
    editSiteId?.let { siteId ->
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
                                val site = db.hierarchyDao().getSite(siteId)
                                if (site != null) {
                                    val updatedSite = site.copy(
                                        name = name,
                                        address = editSiteAddress.trim().takeIf { it.isNotEmpty() }
                                    )
                                    db.hierarchyDao().upsertSite(updatedSite)
                                }
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
    
    // Диалог подтверждения удаления
    deleteSiteId?.let { siteId ->
        AlertDialog(
            onDismissRequest = { deleteSiteId = null },
            title = { Text("Удалить объект?") },
            text = {
                Text("Вы уверены, что хотите удалить этот объект? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteSite(siteId)
                        }
                        deleteSiteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSiteId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SiteRow(
    site: SiteEntity,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iconRes = R.drawable.object_house_blue // Можно добавить логику выбора иконки
    
    EntityRowWithMenu(
        title = site.name,
        subtitle = site.address,
        leadingIcon = {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Объект",
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
        isEditMode = false, // В app-client нет режима редактирования для списка объектов
        isArchived = site.isArchived == true,
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

