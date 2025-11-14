package ru.wassertech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.dao.ClientDao
import ru.wassertech.data.entities.ClientEntity
import ru.wassertech.data.entities.ClientGroupEntity
import ru.wassertech.sync.SafeDeletionHelper
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import ru.wassertech.sync.markArchivedForSync
import ru.wassertech.sync.markUnarchivedForSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Полный VM для экрана "Клиенты".
 * - Держит стейт: groups, clients, includeArchived, selectedGroupId
 * - CRUD групп/клиентов (создание, назначение группы)
 * - Архивирование/восстановление + каскад для клиентов при архивировании группы
 * - Перемещение групп и клиентов внутри группы (по sortOrder)
 *
 * Примечание: используем синхронные DAO-методы (getAllGroupsNow(), getClientsNow(...)),
 * чтобы не зависеть от наличия Flow в DAO. После любых операций дергаем reload().
 */
class ClientsViewModel(
    private val clientDao: ClientDao,
    private val db: AppDatabase
) : ViewModel() {

    // --- UI state ---
    private val _includeArchived = MutableStateFlow(false)
    val includeArchived: StateFlow<Boolean> = _includeArchived.asStateFlow()

    private val _selectedGroupId =
        MutableStateFlow<String?>(null) // null = "Общая", спец ALL не используем
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _groups = MutableStateFlow<List<ClientGroupEntity>>(emptyList())
    val groups: StateFlow<List<ClientGroupEntity>> = _groups.asStateFlow()

    private val _clients = MutableStateFlow<List<ClientEntity>>(emptyList())
    val clients: StateFlow<List<ClientEntity>> = _clients.asStateFlow()

    init {
        reloadAll()
    }

    // --- Public API для роутинга/экрана ---
    fun selectAll() {
        // в новой верстке мы показываем секции, selectedGroupId фактически не используется,
        // но оставим для совместимости
        _selectedGroupId.value = null
        reloadClients()
    }

    fun selectNoGroup() {
        _selectedGroupId.value = null
        reloadClients()
    }

    fun selectGroup(id: String) {
        _selectedGroupId.value = id
        reloadClients()
    }

    fun toggleIncludeArchived() {
        _includeArchived.value = !_includeArchived.value
        reloadAll()
    }

    fun createGroup(title: String) = viewModelScope.launch(Dispatchers.IO) {
        val nextOrder = (clientDao.getAllGroupsNow()
            .maxOfOrNull { it.sortOrder ?: -1 } ?: -1) + 1

        val group = ClientGroupEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            notes = null,
            sortOrder = nextOrder
        ).markCreatedForSync()
        clientDao.upsertGroup(group)
        reloadGroups()
    }

    fun createClient(name: String, corporate: Boolean, groupId: String?) =
        viewModelScope.launch(Dispatchers.IO) {
            val nextOrder = (clientDao.getClientsNow(groupId)
                .maxOfOrNull { it.sortOrder ?: -1 } ?: -1) + 1

            val client = ClientEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                isCorporate = corporate,
                clientGroupId = groupId,
                sortOrder = nextOrder
            ).markCreatedForSync()
            clientDao.upsertClient(client)
            reloadClients()
        }

    fun assignClientToGroup(clientId: String, groupId: String?) =
        viewModelScope.launch(Dispatchers.IO) {
            clientDao.setClientGroup(clientId, groupId, System.currentTimeMillis())
            reloadClients()
        }

    // --- Архив / Восстановление (клиенты) ---
    fun archiveClient(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val updated = c.markArchivedForSync()
        clientDao.upsertClient(updated)
        reloadClients()
    }

    fun restoreClient(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val updated = c.markUnarchivedForSync()
        clientDao.upsertClient(updated)
        reloadClients()
    }

    fun editClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = client.markUpdatedForSync()
            clientDao.upsertClient(updated)
            reloadClients() // чтобы обновился список
        }
    }


    // --- Архив / Восстановление (группы) ---
    fun archiveGroup(groupId: String) = viewModelScope.launch(Dispatchers.IO) {
        val groupsNow = clientDao.getAllGroupsNow()
        val g = groupsNow.firstOrNull { it.id == groupId } ?: return@launch

        // архивируем группу
        clientDao.upsertGroup(g.markArchivedForSync())

        // каскадно архивируем клиентов этой группы
        val clientsInGroup = clientDao.getClientsNow(groupId)
        clientsInGroup.forEach { c ->
            clientDao.upsertClient(c.markArchivedForSync())
        }

        reloadAll()
    }

    fun restoreGroup(groupId: String) = viewModelScope.launch(Dispatchers.IO) {
        val groupsNow = clientDao.getAllGroupsNow()
        val g = groupsNow.firstOrNull { it.id == groupId } ?: return@launch

        // Восстанавливаем группу
        clientDao.upsertGroup(g.markUnarchivedForSync())
        
        // Каскадно восстанавливаем клиентов этой группы из архива
        val clientsInGroup = clientDao.getClientsNow(groupId)
        clientsInGroup.forEach { c ->
            if (c.isArchived == true) {
                clientDao.upsertClient(c.markUnarchivedForSync())
            }
        }
        
        reloadAll()
    }

    // --- Перемещение групп ---
    fun moveGroupUp(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val list = clientDao.getAllGroupsNow()
            .filter { includeArchived.value || it.isArchived != true }
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i > 0) {
            java.util.Collections.swap(list, i, i - 1)
            list.forEachIndexed { index, g -> clientDao.updateGroupOrder(g.id, index) }
            reloadGroups()
        }
    }

    fun moveGroupDown(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val list = clientDao.getAllGroupsNow()
            .filter { includeArchived.value || it.isArchived != true }
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i != -1 && i < list.lastIndex) {
            java.util.Collections.swap(list, i, i + 1)
            list.forEachIndexed { index, g -> clientDao.updateGroupOrder(g.id, index) }
            reloadGroups()
        }
    }

    // --- Перемещение клиентов ---
    fun moveClientUp(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val groupId = c.clientGroupId
        val list = clientDao.getClientsNow(groupId)
            .filter { includeArchived.value || it.isArchived != true }
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .toMutableList()
        val i = list.indexOfFirst { it.id == clientId }
        if (i > 0) {
            java.util.Collections.swap(list, i, i - 1)
            list.forEachIndexed { index, it -> clientDao.updateClientOrder(it.id, index) }
            reloadClients()
        }
    }

    fun moveClientDown(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val groupId = c.clientGroupId
        val list = clientDao.getClientsNow(groupId)
            .filter { includeArchived.value || it.isArchived != true }
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .toMutableList()
        val i = list.indexOfFirst { it.id == clientId }
        if (i != -1 && i < list.lastIndex) {
            java.util.Collections.swap(list, i, i + 1)
            list.forEachIndexed { index, it -> clientDao.updateClientOrder(it.id, index) }
            reloadClients()
        }
    }

    // --- internal reload helpers ---
    private fun reloadAll() {
        reloadGroups()
        reloadClients()
    }

    private fun reloadGroups() = viewModelScope.launch(Dispatchers.IO) {
        val all = clientDao.getAllGroupsNow()
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
        // Группы фильтруются по includeArchived, но это состояние управляется извне
        // В ClientsScreen будет дополнительная фильтрация по isEditMode
        _groups.value = if (_includeArchived.value) all else all.filter { it.isArchived != true }
    }

    fun reloadClients() = viewModelScope.launch(Dispatchers.IO) {
        // Мы не фильтруем по selectedGroupId на стороне VM — экран сам группирует по clientGroupId.
        // Поэтому просто берём всех клиентов (с учётом includeArchived) и отдаём в UI.
        val allGeneral = clientDao.getClientsNow(null)
        val byGroups = clientDao.getAllGroupsNow().flatMap { g -> clientDao.getClientsNow(g.id) }
        val all = (allGeneral + byGroups).sortedBy { it.sortOrder ?: Int.MAX_VALUE }
        _clients.value = if (_includeArchived.value) all else all.filter { it.isArchived != true }
    }

    fun renameGroup(groupId: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            clientDao.updateGroupTitle(groupId, title)
            reloadGroups()
        }
    }

    fun renameClientName(clientId: String, newName: String) {
        val name = newName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            clientDao.updateClientName(clientId, name)
            reloadClients()
        }
    }

    fun assignClientGroup(clientId: String, groupId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            clientDao.assignClientToGroup(clientId, groupId)
            reloadClients()
        }
    }

    fun reorderClientsInGroup(groupId: String?, orderedIds: List<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            orderedIds.forEachIndexed { index, id ->
                clientDao.updateClientOrder(id, index)
            }
            reloadClients()
        }

    // --- Удаление (только для архивных элементов) ---
    fun deleteClient(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        SafeDeletionHelper.deleteClient(db, clientId)
        reloadClients()
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch(Dispatchers.IO) {
        SafeDeletionHelper.deleteClientGroup(db, groupId)
        reloadAll()
    }
}


/** Фабрика на основе ClientDao (Application-agnostic) */
class ClientsViewModelFactory(
    private val clientDao: ClientDao,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientsViewModel::class.java)) {
            return ClientsViewModel(clientDao, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
