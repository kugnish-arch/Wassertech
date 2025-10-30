package com.example.wassertech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.dao.ClientDao
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientsViewModel(
    private val clientDao: ClientDao
) : ViewModel() {

    private val _includeArchived = MutableStateFlow(false)
    val includeArchived: StateFlow<Boolean> = _includeArchived

    val clients: StateFlow<List<ClientEntity>> =
        includeArchived.flatMapLatest { include ->
            clientDao.observeClients(includeArchived = include)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // groups: переключаем источник по includeArchived
    val groups: StateFlow<List<ClientGroupEntity>> =
        includeArchived.flatMapLatest { include ->
            if (include) clientDao.observeAllGroups() else clientDao.observeActiveGroups()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleIncludeArchived() {
        _includeArchived.value = !_includeArchived.value
    }

    fun createGroup(title: String) = viewModelScope.launch {
        val ts = System.currentTimeMillis()
        val group = ClientGroupEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            notes = null,
            sortOrder = 0,
            isArchived = false,
            archivedAtEpoch = null,
            createdAtEpoch = ts,
            updatedAtEpoch = ts
        )
        clientDao.upsertGroup(group)
    }

    fun createClient(name: String, corporate: Boolean, groupId: String?) = viewModelScope.launch {
        val ts = System.currentTimeMillis()
        val client = ClientEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isCorporate = corporate,
            clientGroupId = groupId,
            isArchived = false,
            archivedAtEpoch = null,
            createdAtEpoch = ts,
            updatedAtEpoch = ts
        )
        clientDao.upsertClient(client)
    }

    fun archiveClient(id: String) = viewModelScope.launch {
        clientDao.archiveClient(id, System.currentTimeMillis())
    }

    fun restoreClient(id: String) = viewModelScope.launch {
        clientDao.restoreClient(id)
    }

    // Каскад на клиентов
    fun archiveGroup(id: String) = viewModelScope.launch {
        clientDao.archiveGroupCascade(id, System.currentTimeMillis())
    }

    fun restoreGroup(id: String) = viewModelScope.launch {
        clientDao.restoreGroupCascade(id, System.currentTimeMillis())
    }
}
