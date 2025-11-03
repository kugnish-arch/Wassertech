package com.example.wassertech.ui.clients

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.viewmodel.ClientsViewModel
import com.example.wassertech.viewmodel.ClientsViewModelFactory

@Composable
fun ClientsRoute(
    onClientClick: (String) -> Unit
) {
    // Factory-путь (не Hilt)
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).clientDao() }
    val vm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(dao))

    // Подписки на стейты VM
    val groups by vm.groups.collectAsState()
    val clients by vm.clients.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()
    val selectedGroupId by vm.selectedGroupId.collectAsState()

    // Обновляем клиентов при возврате на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.reloadClients()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ClientsScreen(
        groups = groups,
        clients = clients,

        selectedGroupId = selectedGroupId,
        includeArchived = includeArchived,

        onSelectAll = vm::selectAll,
        onSelectNoGroup = vm::selectNoGroup,
        onSelectGroup = vm::selectGroup,

        onToggleIncludeArchived = vm::toggleIncludeArchived,
        onCreateGroup = vm::createGroup,

        onAssignClientGroup = vm::assignClientGroup,    // перенос клиента между группами
        onClientClick = onClientClick,
        onAddClient = { /* при желании можно показать тут прелоадер/лог */ },
        onCreateClient = vm::createClient,

        // НОВОЕ: переименование
        onRenameGroup = vm::renameGroup,
        onRenameClientName = vm::renameClientName,

        // Архив/восстановление
        onArchiveClient = vm::archiveClient,
        onRestoreClient = vm::restoreClient,
        onArchiveGroup = vm::archiveGroup,   // каскадно архивируем клиентов группы
        onRestoreGroup = vm::restoreGroup,

        // Перемещения
        onMoveGroupUp = vm::moveGroupUp,
        onMoveGroupDown = vm::moveGroupDown,
        onMoveClientUp = vm::moveClientUp,
        onMoveClientDown = vm::moveClientDown,

        onReorderGroupClients = vm::reorderClientsInGroup
    )
}
