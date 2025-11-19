package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import ru.wassertech.core.screens.hierarchy.SiteInstallationsScreenShared
import ru.wassertech.core.screens.hierarchy.ui.SiteInstallationsUiState
import ru.wassertech.ui.hierarchy.HierarchyUiStateMapper
import ru.wassertech.data.repository.IconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationsScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    onStartMaintenance: (String, String, String) -> Unit, // siteId, installationId, installationName
    onOpenMaintenanceHistory: (String) -> Unit, // installationId
    vm: HierarchyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val iconRepository = remember { IconRepository(context) }
    
    // Данные установок
    val installations by vm.installations(siteId).collectAsState(initial = emptyList())
    
    // Данные объекта
    val site by vm.site(siteId).collectAsState(initial = null)
    val siteName = site?.name ?: "Объект"
    
    // Загружаем иконки для всех установок
    var installationIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(installations) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.data.entities.IconEntity?>()
            installations.forEach { installation ->
                if (installation.iconId != null) {
                    val icon = vm.getIcon(installation.iconId)
                    iconsMap[installation.id] = icon
                } else {
                    iconsMap[installation.id] = null
                }
            }
            installationIcons = iconsMap
        }
    }
    
    // Преобразуем в UI State
    var uiState by remember {
        mutableStateOf<SiteInstallationsUiState?>(null)
    }
    LaunchedEffect(installations, installationIcons) {
        scope.launch(Dispatchers.IO) {
            val items = installations.map { installation ->
                val icon = installationIcons[installation.id]
                withContext(Dispatchers.IO) {
                    HierarchyUiStateMapper.run {
                        installation.toInstallationItemUi(iconRepository, icon)
                    }
                }
            }
            uiState = SiteInstallationsUiState(
                siteId = siteId,
                siteName = siteName,
                installations = items,
                canAddInstallation = true, // В CRM всегда можно добавлять
                canEditSite = true,
                isLoading = false
            )
        }
    }
    
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    
    // Используем shared-экран
    uiState?.let { state ->
        SiteInstallationsScreenShared(
            state = state,
            onInstallationClick = onOpenInstallation,
            onAddInstallationClick = { showAdd = true },
            onInstallationArchive = { vm.archiveInstallation(it) },
            onInstallationRestore = { vm.restoreInstallation(it) },
            onInstallationDelete = { vm.deleteInstallation(it) },
            onChangeInstallationIcon = { installationId ->
                scope.launch {
                    val iconPickerState = vm.loadIconPacksAndIconsFor(ru.wassertech.core.ui.icons.IconEntityType.INSTALLATION)
                    // TODO: Открыть IconPickerDialog для установки
                    // Пока что просто обновляем через ViewModel
                }
            },
            onInstallationsReordered = { newOrder ->
                vm.reorderInstallations(siteId, newOrder)
            },
            onStartMaintenance = { siteId, installationId, installationName ->
                onStartMaintenance(siteId, installationId, installationName)
            },
            onOpenMaintenanceHistory = onOpenMaintenanceHistory,
            isEditing = false,
            onToggleEdit = null
        )
    }
    
    // Диалог добавления установки
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новая установка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Название") }, 
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addInstallation(siteId, n)
                        name = TextFieldValue("")
                        showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
