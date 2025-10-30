package com.example.wassertech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.dao.ClientDao
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity
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
    private val clientDao: ClientDao
) : ViewModel() {

    // --- UI state ---
    private val _includeArchived = MutableStateFlow(false)
    val includeArchived: StateFlow<Boolean> = _includeArchived.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null) // null = "Общая", спец ALL не используем
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

        val now = System.currentTimeMillis()
        val group = ClientGroupEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            notes = null,
            sortOrder = nextOrder,
            isArchived = false,
            archivedAtEpoch = null,
            createdAtEpoch = now,
            updatedAtEpoch = now
        )
        clientDao.upsertGroup(group)
        reloadGroups()
    }

    fun createClient(name: String, corporate: Boolean, groupId: String?) = viewModelScope.launch(Dispatchers.IO) {
        val nextOrder = (clientDao.getClientsNow(groupId)
            .maxOfOrNull { it.sortOrder ?: -1 } ?: -1) + 1

        val now = System.currentTimeMillis()
        val client = ClientEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isCorporate = corporate,
            clientGroupId = groupId,
            sortOrder = nextOrder,
            isArchived = false,
            archivedAtEpoch = null,
            createdAtEpoch = now,
            updatedAtEpoch = now,
            // остальные nullable поля по умолчанию из data-класса
        )
        clientDao.upsertClient(client)
        reloadClients()
    }

    fun assignClientToGroup(clientId: String, groupId: String?) = viewModelScope.launch(Dispatchers.IO) {
        clientDao.setClientGroup(clientId, groupId, System.currentTimeMillis())
        reloadClients()
    }

    // --- Архив / Восстановление (клиенты) ---
    fun archiveClient(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val updated = c.copy(isArchived = true, archivedAtEpoch = System.currentTimeMillis())
        clientDao.upsertClient(updated)
        reloadClients()
    }

    fun restoreClient(clientId: String) = viewModelScope.launch(Dispatchers.IO) {
        val c = clientDao.getClientByIdNow(clientId) ?: return@launch
        val updated = c.copy(isArchived = false, archivedAtEpoch = null)
        clientDao.upsertClient(updated)
        reloadClients()
    }

    // --- Архив / Восстановление (группы) ---
    fun archiveGroup(groupId: String) = viewModelScope.launch(Dispatchers.IO) {
        val groupsNow = clientDao.getAllGroupsNow()
        val g = groupsNow.firstOrNull { it.id == groupId } ?: return@launch
        val ts = System.currentTimeMillis()

        // архивируем группу
        clientDao.upsertGroup(
            g.copy(isArchived = true, archivedAtEpoch = ts, updatedAtEpoch = ts)
        )

        // каскадно архивируем клиентов этой группы
        val clientsInGroup = clientDao.getClientsNow(groupId)
        clientsInGroup.forEach { c ->
            val u = c.copy(isArchived = true, archivedAtEpoch = ts, updatedAtEpoch = ts)
            clientDao.upsertClient(u)
        }

        reloadAll()
    }

    fun restoreGroup(groupId: String) = viewModelScope.launch(Dispatchers.IO) {
        val groupsNow = clientDao.getAllGroupsNow()
        val g = groupsNow.firstOrNull { it.id == groupId } ?: return@launch
        val ts = System.currentTimeMillis()

        clientDao.upsertGroup(
            g.copy(isArchived = false, archivedAtEpoch = null, updatedAtEpoch = ts)
        )
        // клиентов НЕ разворачиваем автоматически (обычно группы восстанавливают пустыми)
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
        _groups.value = if (_includeArchived.value) all else all.filter { it.isArchived != true }
    }

    private fun reloadClients() = viewModelScope.launch(Dispatchers.IO) {
        // Мы не фильтруем по selectedGroupId на стороне VM — экран сам группирует по clientGroupId.
        // Поэтому просто берём всех клиентов (с учётом includeArchived) и отдаём в UI.
        val allGeneral = clientDao.getClientsNow(null)
        val byGroups = clientDao.getAllGroupsNow().flatMap { g -> clientDao.getClientsNow(g.id) }
        val all = (allGeneral + byGroups).sortedBy { it.sortOrder ?: Int.MAX_VALUE }
        _clients.value = if (_includeArchived.value) all else all.filter { it.isArchived != true }
    }
}

/** Фабрика на основе ClientDao (Application-agnostic) */
class ClientsViewModelFactory(
    private val clientDao: ClientDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientsViewModel::class.java)) {
            return ClientsViewModel(clientDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
