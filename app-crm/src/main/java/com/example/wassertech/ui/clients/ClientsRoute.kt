package ru.wassertech.ui.clients

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import ru.wassertech.data.AppDatabase
import ru.wassertech.viewmodel.ClientsViewModel
import ru.wassertech.viewmodel.ClientsViewModelFactory

@Composable
fun ClientsRoute(
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onClientClick: (String) -> Unit
) {
    // Factory-путь (не Hilt)
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val vm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(db.clientDao(), db))

    // Подписки на стейты VM
    val groups by vm.groups.collectAsState()
    val clients by vm.clients.collectAsState()
    val includeArchived by vm.includeArchived.collectAsState()
    val selectedGroupId by vm.selectedGroupId.collectAsState()

    // Обновляем клиентов при возврате на экран
    // Важно: reloadClients() использует текущее значение includeArchived из ViewModel,
    // поэтому состояние сохраняется автоматически
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Просто перезагружаем клиентов - includeArchived уже сохранен в ViewModel
                vm.reloadClients()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Создаем обертку для onCancel, которая будет использоваться в ClientsScreen
    // Но onCancel вызывается из AppTopBar, поэтому нужно использовать другой подход
    // Передаем onCancel напрямую, а логику установки shouldSave = false делаем в ClientsScreen
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

        onReorderGroupClients = vm::reorderClientsInGroup,

        // Удаление
        onDeleteClient = vm::deleteClient,
        onDeleteGroup = vm::deleteGroup,
        
        // Режим редактирования
        isEditing = isEditing,
        onToggleEdit = onToggleEdit,
        onCancel = onCancel
    )
}
