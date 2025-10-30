package com.example.wassertech.ui.clients

/*==================================================
Маршрут-обёртка ClientsRoute

Эта функция:
-получает VM (через фабрику),
-«подписывается» на Flow из VM,
-передаёт данные и коллбеки в экран ClientsScreen.
==================================================*/

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.viewmodel.ClientsViewModel
import com.example.wassertech.viewmodel.ClientsViewModelFactory
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.collectAsState

@Composable
fun ClientsRoute(
    onClientClick: (String) -> Unit,

) {

    val app = LocalContext.current.applicationContext as Application

    // Создаём VM через твою фабрику (требует Application)
    val vm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(app))

    val groups by vm.groups.collectAsState(emptyList())
    val clients by vm.clients.collectAsState(emptyList())
    val selectedGroupId by vm.selectedGroupId.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()



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
        onClientClick = { client -> onClientClick(client.id) },

        // можно оставить onAddClient пустым — диалог открывается локально в экране
        onAddClient = {},

        onArchiveClient = { vm.archiveClient(it) },
        onRestoreClient = { vm.restoreClient(it) },
        onArchiveGroup  = { vm.archiveGroup(it) },
        onRestoreGroup  = { vm.restoreGroup(it) },

        // ВАЖНО: проброс сохранения нового клиента
        onCreateClient = { name, corporate, groupId ->
            vm.createClient(name, corporate, groupId)
        }
    )
}

