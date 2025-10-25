package com.example.wassertech.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ClientEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Legacy-compatible ClientsViewModel placed under package com.example.wassertech.ui
 * to satisfy old imports. It mirrors the API used by the new UI.
 * Prefer using com.example.wassertech.viewmodel.HierarchyViewModel going forward.
 */
class ClientsViewModel(app: Application): AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(app) }
    private val dao by lazy { db.hierarchyDao() }

    val clients = dao.observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addClient(name: String, notes: String?) {
        viewModelScope.launch {
            dao.upsertClient(ClientEntity(id = UUID.randomUUID().toString(), name = name, phone = null, notes = notes))
        }
    }

    // New style: delete by id (used by patched ClientsScreen)
    fun deleteClient(id: String) {
        viewModelScope.launch { dao.deleteClient(id) }
    }

    // Legacy compatibility: delete by entity
    fun deleteClient(c: ClientEntity) {
        deleteClient(c.id)
    }
}
