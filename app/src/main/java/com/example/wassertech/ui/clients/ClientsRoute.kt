package com.example.wassertech.ui.clients

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.viewmodel.ClientsViewModel
import com.example.wassertech.viewmodel.ClientsViewModelFactory

@Composable
fun ClientsRoute(
    onClientClick: (ClientEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(app))

    val groups by vm.groups.collectAsState()
    val clients by vm.clients.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()

    ClientsScreen(
        groups = groups,
        clients = clients,
        selectedGroupId = null,            // фильтры групп больше не используем
        includeArchived = includeArchived,
        onSelectAll = {},
        onSelectNoGroup = {},
        onSelectGroup = {},

        onToggleIncludeArchived = { vm.toggleIncludeArchived() },
        onCreateGroup = { title -> vm.createGroup(title) },

        onAssignClientGroup = { _, _ -> }, // в новой раскладке не требуется
        onClientClick = onClientClick,
        onAddClient = { /* просто откроем диалог в UI */ },

        onCreateClient = { name, corp, groupId -> vm.createClient(name, corp, groupId) },

        onArchiveClient = { id -> vm.archiveClient(id) },
        onRestoreClient = { id -> vm.restoreClient(id) },
        onArchiveGroup = { id -> vm.archiveGroup(id) },
        onRestoreGroup = { id -> vm.restoreGroup(id) }
    )
}
