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
    onClientClick: (ClientEntity) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(app))

    // «Подписываемся» на потоки VM (Flow -> State)
    val groups by vm.groups.collectAsState(initial = emptyList())
    val clients by vm.clients.collectAsState(initial = emptyList())
    val selectedGroupId by vm.selectedGroupId.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()

    // Передаём данные и методы VM в UI
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
        onClientClick = onClientClick
    )
}
