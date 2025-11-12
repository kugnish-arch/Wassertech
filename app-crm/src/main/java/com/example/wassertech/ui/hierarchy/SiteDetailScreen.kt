@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import ru.wassertech.ui.icons.AppIcons
import androidx.compose.ui.Alignment

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
                    Icon(
                        imageVector = AppIcons.Site,
                        contentDescription = null,
                        tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
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

            Text(
                text = "Установки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (installations.isEmpty()) {
                Text(
                    text = "У этого объекта пока нет установок",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installations, key = { it.id }) { inst ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenInstallation(inst.id) },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                        ) {
                            ListItem(
                                headlineContent = { Text(inst.name) },
                                trailingContent = {
                                    IconButton(onClick = { onOpenInstallation(inst.id) }) {
                                        Icon(ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon, contentDescription = "Открыть")
                                    }
                                }
                            )
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
