@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

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
import com.example.wassertech.viewmodel.HierarchyViewModel
import kotlinx.coroutines.launch
import com.example.wassertech.data.entities.InstallationEntity
import com.example.wassertech.ui.common.AppFloatingActionButton
import com.example.wassertech.ui.common.FABTemplate
import androidx.compose.ui.graphics.Color
import com.example.wassertech.ui.icons.AppIcons
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
            // Шапка в стиле ClientDetailScreen.kt
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Site,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        siteName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.weight(1f))
                    // Иконка редактирования видна только в режиме редактирования
                    if (isEditing) {
                        IconButton(onClick = { showEdit = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Редактировать объект",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
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
                                .clickable { onOpenInstallation(inst.id) }
                        ) {
                            ListItem(
                                headlineContent = { Text(inst.name) },
                                trailingContent = {
                                    IconButton(onClick = { onOpenInstallation(inst.id) }) {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Открыть")
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
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название") })
                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val s = vm.getSite(siteId)
                        if (s != null) {
                            vm.editSite(s.copy(name = editName.text.trim(), address = editAddr.text.trim().ifEmpty { null }))
                            siteName = editName.text.trim()
                        }
                        showEdit = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }

    if (showAddInst) {
        AlertDialog(
            onDismissRequest = { showAddInst = false },
            title = { Text("Новая установка") },
            text = {
                OutlinedTextField(
                    value = newInstName,
                    onValueChange = { newInstName = it },
                    label = { Text("Название установки") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newInstName.text.trim().ifBlank { "Новая установка" }
                    vm.addInstallation(siteId, name)
                    showAddInst = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddInst = false }) { Text("Отмена") } }
        )
    }
}
