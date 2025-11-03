package com.example.wassertech.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SettingsApplications
//import androidx.compose.material.icons.outlined.Manufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.ClientsViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.ui.icons.AppIcons
import kotlinx.coroutines.launch
import com.example.wassertech.ui.common.EditDoneBottomBar
import com.example.wassertech.ui.common.BarAction


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onOpenSite: (String) -> Unit,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val context = LocalContext.current
    val clientDao = AppDatabase.getInstance(context).clientDao()

    val clientsVm: ClientsViewModel = viewModel {
        val dao = AppDatabase.getInstance(context).clientDao()
        ClientsViewModel(dao)
    }

    val groups by clientsVm.groups.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val accent = Color(0xFF26A69A)

    // Данные
    val sites by vm.sites(clientId).collectAsState(initial = emptyList())

    //Данные клиента
    val client by vm.client(clientId).collectAsState(initial = null)
    val clientName = client?.name ?: "-Клиент-"
    val isCorporate = client?.isCorporate ?: false
    // В начало экрана (рядом с другими collectAsState):
    val all by vm.clients(includeArchived = true).collectAsState(initial = emptyList())

    // Режим редактирования (как на экране "Клиенты")
    var isEditing by remember { mutableStateOf(false) }

    // Локальный порядок сайтов (для стрелок при редактировании)
    var localOrder by remember(clientId) { mutableStateOf(sites.map { it.id }) }
    LaunchedEffect(sites) { if (!isEditing) localOrder = sites.map { it.id } }

    // Диалоги
    var showAddSite by remember { mutableStateOf(false) }
    var showAddInstallation by remember { mutableStateOf(false) }

    // add site
    var addSiteName by remember { mutableStateOf(TextFieldValue("")) }
    var addSiteAddr by remember { mutableStateOf(TextFieldValue("")) }

    // add installation
    var addInstallationName by remember { mutableStateOf(TextFieldValue("")) }
    var sitePickerExpanded by remember { mutableStateOf(false) }
    var selectedSiteIndex by remember { mutableStateOf(0) }

    // Раскрытие установок по объектам (в обычном режиме)
    var expandedSites by remember { mutableStateOf(setOf<String>()) }

    var showEditClient by remember { mutableStateOf(false) }
    var editClientName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showEditClient, client, groups) {
        if (showEditClient && client != null) {
            // имя
            editClientName = TextFieldValue(client!!.name)

            // выбрать индекс группы клиента; если null — ставим "Без группы" (индекс 0, см. ниже)
            val currentGroupId = client!!.clientGroupId
            selectedGroupIndex = if (currentGroupId == null) {
                0 // "Без группы"
            } else {
                // +1 потому что 0 — это "Без группы", дальше идут реальные группы
                val idx = groups.indexOfFirst { it.id == currentGroupId }
                if (idx >= 0) idx + 1 else 0
            }
        }
    }

    Scaffold(
        // Полностью убираем системные отступы — как просили
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        // FAB — только вне режима редактирования
        floatingActionButton = {
            if (!isEditing) {
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    // "+ Установка" — акцентный цвет как у FAB "Клиент"
                    ExtendedFloatingActionButton(
                        onClick = { showAddInstallation = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        content = { Text("+ Установка") }
                    )
                    Spacer(Modifier.width(8.dp))
                    ExtendedFloatingActionButton(
                        onClick = { showAddSite = true },
                        content = { Text("+ Объект") }
                    )
                }
            }
        },

        // Нижняя кнопка "Изменить/Готово" слева, как на экране "Клиенты"
        bottomBar = {
            EditDoneBottomBar(
                isEditing = isEditing,
                onEdit = {
                    // включаем режим редактирования
                    isEditing = true
                    // сброс локального порядка на актуальный
                    localOrder = sites.map { it.id }
                },
                onDone = {
                    // сохраняем порядок сайтов и выходим из редактирования
                    vm.reorderSites(clientId, localOrder)
                    isEditing = false
                },
                actions = emptyList() // или можно добавить действия справа, если нужно
            )
        }

    ) { _->
        Column(
            Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======= HEADER =======
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                // Плашка имени — как у групп (secondaryContainer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorporate) AppIcons.ClientCorporate else AppIcons.ClientPrivate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        clientName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(Modifier.weight(1f))

                    // Иконка "Редактировать" (✎) — показывается только в режиме редактирования
                    if (isEditing) {
                        IconButton(onClick = {
                            // при открытии диалога сразу заполняем поля текущими данными
                            editClientName = TextFieldValue(clientName)
                            showEditClient = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Редактировать клиента",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (client != null) {
                    val groupName = groups.firstOrNull { it.id == client!!.clientGroupId }?.title
                    if (groupName != null) {
                        Text(
                            text = "Группа: $groupName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.padding(start = 50.dp, top = 2.dp, bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Без группы",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 50.dp, top = 2.dp, bottom = 4.dp)
                        )
                    }
                }


            }

            // ======= Список объектов/установок =======
            if (isEditing) {
                // Режим редактирования: показываем список объектов с крупными стрелками для сортировки
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sites, key = { it.id }) { s ->
                        val index = localOrder.indexOf(s.id)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}. ${s.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.weight(1f))
                                Row {
                                    IconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(s.id)
                                            if (pos > 0) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos - 1]; list[pos - 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Вверх",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(s.id)
                                            if (pos >= 0 && pos < localOrder.lastIndex) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos + 1]; list[pos + 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index < localOrder.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Вниз",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Обычный режим: раскрывающиеся объекты + установки
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sites, key = { it.id }) { s ->
                        val isExpanded = expandedSites.contains(s.id)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column {
                                ListItem(
                                    modifier = Modifier.clickable { onOpenSite(s.id) },
                                    leadingContent = {
                                        Icon(
                                            imageVector = AppIcons.Site,
                                            contentDescription = null
                                        )
                                    },
                                    headlineContent = { Text(s.name) },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            expandedSites = if (isExpanded) expandedSites - s.id else expandedSites + s.id
                                        }) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                                if (isExpanded) {
                                    val installations by vm.installations(s.id).collectAsState(initial = emptyList())
                                    Column(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (installations.isEmpty()) {
                                            Text("Нет установок", style = MaterialTheme.typography.bodyMedium)
                                        } else {
                                            installations.forEach { inst ->
                                                ElevatedCard(
                                                    onClick = { onOpenInstallation(inst.id) },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        Modifier
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Иконка установки — Material3 Manufacturing
                                                        Icon(
                                                            imageVector = Icons.Outlined.SettingsApplications,
                                                            contentDescription = null
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            inst.name,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----
    if (showAddSite) {
        AlertDialog(
            onDismissRequest = { showAddSite = false },
            title = { Text("Добавить объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = addSiteName, onValueChange = { addSiteName = it }, label = { Text("Название объекта") })
                    OutlinedTextField(value = addSiteAddr, onValueChange = { addSiteAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = addSiteName.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addSite(clientId, n, addSiteAddr.text.trim().ifEmpty { null })
                        addSiteName = TextFieldValue("")
                        addSiteAddr = TextFieldValue("")
                        showAddSite = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddSite = false }) { Text("Отмена") } }
        )
    }

    if (showAddInstallation) {
        AlertDialog(
            onDismissRequest = { showAddInstallation = false },
            title = { Text("Добавить установку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = addInstallationName, onValueChange = { addInstallationName = it }, label = { Text("Название установки") })
                    ExposedDropdownMenuBox(expanded = sitePickerExpanded, onExpandedChange = { sitePickerExpanded = it }) {
                        OutlinedTextField(
                            value = sites.getOrNull(selectedSiteIndex)?.name ?: "Выберите объект",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Объект") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sitePickerExpanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = sitePickerExpanded, onDismissRequest = { sitePickerExpanded = false }) {
                            sites.forEachIndexed { index, s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = {
                                    selectedSiteIndex = index
                                    sitePickerExpanded = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = addInstallationName.text.trim()
                    if (n.isNotEmpty()) {
                        val selectedSiteId = sites.getOrNull(selectedSiteIndex)?.id
                        if (selectedSiteId != null) vm.addInstallationToSite(selectedSiteId, n)
                        else vm.addInstallationToMain(clientId, n)
                        addInstallationName = TextFieldValue("")
                        showAddInstallation = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddInstallation = false }) { Text("Отмена") } }


        )
    }

    if (showEditClient) {
        val groups by clientsVm.groups.collectAsState(initial = emptyList())

        // Опции групп: [0] — "Без группы", дальше реальные группы
        val groupOptions = remember(groups) {
            buildList<Pair<String?, String>> {
                add(null to "Без группы")
                addAll(groups.map { it.id to it.title })
            }
        }
        AlertDialog(
            onDismissRequest = { showEditClient = false },
            title = { Text("Редактировать клиента") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editClientName,
                        onValueChange = { editClientName = it },
                        label = { Text("Имя / название клиента") },
                        singleLine = true
                    )

                    // Выбор группы


                    ExposedDropdownMenuBox(
                        expanded = groupPickerExpanded,
                        onExpandedChange = { groupPickerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = groupOptions.getOrNull(selectedGroupIndex)?.second ?: "Без группы",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Группа") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupPickerExpanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = groupPickerExpanded,
                            onDismissRequest = { groupPickerExpanded = false }
                        ) {
                            groupOptions.forEachIndexed { index, pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.second) },
                                    onClick = {
                                        selectedGroupIndex = index
                                        groupPickerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = {
                    client?.let { current ->
                        val newName = editClientName.text.trim()
                        val selectedGroupId = groupOptions.getOrNull(selectedGroupIndex)?.first

                        // имя
                        clientsVm.editClient(current.copy(name = newName))

                        // группа (включая переход в "Без группы" при null)
                        if (selectedGroupId != current.clientGroupId) {
                            clientsVm.assignClientToGroup(current.id, selectedGroupId)
                        }
                    }
                    showEditClient = false
                }) { Text("Сохранить") }

            },
            dismissButton = {
                TextButton(onClick = { showEditClient = false }) {
                    Text("Отмена")
                }
            }
        )
    }

}
