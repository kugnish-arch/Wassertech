package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.dao.ClientDao
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel экрана "Клиенты" с поддержкой групп и архива.
 *
 * selectedGroupId:
 *  - ALL_GROUP_ID = показывать все группы
 *  - null         = показывать клиентов без группы
 *  - любое String = конкретная группа
 */
class ClientsViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase = AppDatabase.getInstance(application)
    private val clientDao: ClientDao = db.clientDao()

    companion object {
        const val ALL_GROUP_ID: String = "__ALL__"
    }

    // ---- состояние фильтров ----
    private val _selectedGroupId = MutableStateFlow<String?>(ALL_GROUP_ID)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _includeArchived = MutableStateFlow(false)
    val includeArchived: StateFlow<Boolean> = _includeArchived.asStateFlow()

    // ---- данные для UI ----

    // Список групп (только активные; при желании можно сделать observeAllGroups)
    val groups: Flow<List<ClientGroupEntity>> =
        clientDao.observeActiveGroups()

    // Главный поток клиентов с учётом выбранной группы и флага архива
    val clients: Flow<List<ClientEntity>> =
        combine(selectedGroupId, includeArchived) { gid, inc ->
            gid to inc
        }.flatMapLatest { (gid, inc) ->
            when (gid) {
                ALL_GROUP_ID -> clientDao.observeClients(includeArchived = inc)
                null         -> clientDao.observeClientsByGroup(clientGroupId = null, includeArchived = inc)
                else         -> clientDao.observeClientsByGroup(clientGroupId = gid, includeArchived = inc)
            }
        }.distinctUntilChanged()

    // ---- события от UI ----

    fun selectAll() {
        _selectedGroupId.value = ALL_GROUP_ID
    }

    fun selectNoGroup() {
        _selectedGroupId.value = null
    }

    fun selectGroup(id: String) {
        _selectedGroupId.value = id
    }

    fun toggleIncludeArchived() {
        _includeArchived.value = !_includeArchived.value
    }

    // ---- операции с группами ----

    fun createGroup(title: String) {
        viewModelScope.launch {
            val trimmed = title.trim()
            if (trimmed.isNotEmpty()) {
                clientDao.upsertGroup(
                    ClientGroupEntity(title = trimmed)
                )
            }
        }
    }

    fun renameGroup(id: String, newTitle: String) {
        viewModelScope.launch {
            val trimmed = newTitle.trim()
            if (trimmed.isNotEmpty()) {
                clientDao.renameGroup(id, trimmed)
            }
        }
    }

    fun archiveGroup(id: String) {
        viewModelScope.launch {
            clientDao.archiveGroup(id)
            // если мы смотрели на эту группу — переключим фильтр на "Все"
            if (selectedGroupId.value == id) {
                selectAll()
            }
        }
    }

    fun restoreGroup(id: String) {
        viewModelScope.launch {
            clientDao.restoreGroup(id)
        }
    }

    // ---- операции с клиентами ----

    fun assignClientToGroup(clientId: String, groupId: String?) {
        viewModelScope.launch {
            clientDao.setClientGroup(clientId, groupId)
        }
    }

    fun archiveClient(clientId: String) {
        viewModelScope.launch {
            clientDao.archiveClient(clientId)
        }
    }

    fun restoreClient(clientId: String) {
        viewModelScope.launch {
            clientDao.restoreClient(clientId)
        }
    }
}
