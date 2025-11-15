@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import kotlinx.coroutines.launch
import ru.wassertech.data.entities.InstallationEntity
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.CommonAddDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.theme.ClientsRowDivider

@Composable
fun SiteDetailScreen(
    siteId: String,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var siteName by remember { mutableStateOf("Объект") }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var editAddr by remember { mutableStateOf(TextFieldValue("")) }
    var isSiteArchived by remember { mutableStateOf(false) }
    var isCorporate by remember { mutableStateOf(false) }

    val installations: List<InstallationEntity> by vm.installations(siteId)
        .collectAsState(initial = emptyList())

    var showAddInst by remember { mutableStateOf(false) }
    var newInstName by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(siteId) {
        val s = vm.getSite(siteId)
        if (s != null) {
            siteName = s.name
            editName = TextFieldValue(s.name)
            editAddr = TextFieldValue(s.address ?: "")
            isSiteArchived = s.isArchived == true
            
            // Получаем информацию о клиенте для определения типа
            val client = vm.getClient(s.clientId)
            isCorporate = client?.isCorporate ?: false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем отступы от топБара
        floatingActionButton = {
            AppFloatingActionButton(
                template = FABTemplate(
                    icon = Icons.Filled.Add,
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    onClick = { showAddInst = true; newInstName = TextFieldValue("") }
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======= HEADER =======
            // Шапка в стиле из темы
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
                        // Выбираем иконку объекта в зависимости от типа клиента и архивации
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
                        // Иконка редактирования видна только в режиме редактирования
                        if (isEditing) {
                            IconButton(onClick = { showEdit = true }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Редактировать объект",
                                    tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                )
                            }
                        }
                    }
                }
                // Бордер снизу как у групп клиентов
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }

            // Содержимое секции установок
            if (installations.isEmpty()) {
                EmptyGroupPlaceholder(
                    text = "У этого объекта пока нет установок",
                    indent = 16.dp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(installations, key = { it.id }) { inst ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InstallationRowWithEdit(
                                installation = inst,
                                isEditMode = isEditing,
                                onClick = { onOpenInstallation(inst.id) },
                                onArchive = { vm.archiveInstallation(inst.id) },
                                onRestore = { vm.restoreInstallation(inst.id) },
                                onDelete = { vm.deleteInstallation(inst.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между установками (кроме последней)
                            val index = installations.indexOf(inst)
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

    if (showEdit) {
        CommonAddDialog(
            title = "Редактировать объект",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название") })
                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            onDismissRequest = { showEdit = false },
            confirmText = "Сохранить",
            onConfirm = {
                scope.launch {
                    val s = vm.getSite(siteId)
                    if (s != null) {
                        vm.editSite(s.copy(name = editName.text.trim(), address = editAddr.text.trim().ifEmpty { null }))
                        siteName = editName.text.trim()
                    }
                    showEdit = false
                }
            },
            confirmEnabled = editName.text.trim().isNotEmpty()
        )
    }

    if (showAddInst) {
        CommonAddDialog(
            title = "Новая установка",
            text = {
                OutlinedTextField(
                    value = newInstName,
                    onValueChange = { newInstName = it },
                    label = { Text("Название установки") },
                    singleLine = true
                )
            },
            onDismissRequest = { showAddInst = false },
            onConfirm = {
                val name = newInstName.text.trim().ifBlank { "Новая установка" }
                vm.addInstallation(siteId, name)
                showAddInst = false
            },
            confirmEnabled = newInstName.text.trim().isNotEmpty()
        )
    }
}

/* ---------- Вспомогательные UI-компоненты ---------- */

@Composable
private fun InstallationRowWithEdit(
    installation: InstallationEntity,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Определяем иконку установки
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
        trailingIcon = if (!isEditMode) {
            {
                Icon(
                    imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else null,
        isEditMode = isEditMode,
        isArchived = installation.isArchived == true,
        onClick = onClick,
        onRestore = onRestore,
        onArchive = onArchive,
        onDelete = onDelete,
        onEdit = null, // Редактирование установки пока не поддерживается на этом экране
        onMoveToGroup = null,
        availableGroups = emptyList(),
        modifier = modifier,
        reorderableState = null,
        showDragHandle = false
    )
}
