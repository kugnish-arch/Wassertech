package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.seed.TemplateSeeder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClientsViewModel(app: Application): AndroidViewModel(app) {
    private val db by lazy {
        AppDatabase.get(app)
    }
    private val dao by lazy { db.hierarchyDao() }

    val clients = dao.observeClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            TemplateSeeder.seedOnce(db)
        }
    }

    fun addClient(name: String, notes: String?) {
        viewModelScope.launch {
            dao.upsertClient(ClientEntity(id = UUID.randomUUID().toString(), name = name, phone = null, notes = notes))
        }
    }

    fun deleteClient(c: ClientEntity) {
        viewModelScope.launch { dao.deleteClient(c.id) }
    }
}
