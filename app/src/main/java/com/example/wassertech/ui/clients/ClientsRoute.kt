package com.example.wassertech.ui.clients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.viewmodel.ClientsViewModel
import com.example.wassertech.viewmodel.ClientsViewModelFactory

@Composable
fun ClientsRoute(
    onClientClick: (ClientEntity) -> Unit
) {
    val context = LocalContext.current
    val dao = AppDatabase.getInstance(context).clientDao()

    val vm: ClientsViewModel = viewModel(
        factory = ClientsViewModelFactory(dao)
    )

    // Подписываемся на стейты VM
    val groups by vm.groups.collectAsState()
    val clients by vm.clients.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()
    val selectedGroupId by vm.selectedGroupId.collectAsState()

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

        onAssignClientGroup = vm::assignClientToGroup,
        onClientClick = onClientClick,
        onAddClient = {},

        onCreateClient = vm::createClient,

        // Архив/восстановление
        onArchiveClient = vm::archiveClient,
        onRestoreClient = vm::restoreClient,
        onArchiveGroup = vm::archiveGroup, // каскадно архивируем клиентов группы
        onRestoreGroup = vm::restoreGroup,

        // Перемещения (стрелки)
        onMoveGroupUp = vm::moveGroupUp,
        onMoveGroupDown = vm::moveGroupDown,
        onMoveClientUp = vm::moveClientUp,
        onMoveClientDown = vm::moveClientDown,
    )
}
