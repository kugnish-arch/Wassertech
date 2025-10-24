package com.example.wassertech.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.ClientEntity
import com.example.wassertech.data.ClientRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "clients.db"
    ).fallbackToDestructiveMigration().build()

    private val repo = ClientRepository(db.clientDao())

    val clients = repo.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addClient(name: String, phone: String, address: String, notes: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.add(ClientEntity(name = trimmed, phone = phone.trim().ifEmpty { null }, address = address.trim().ifEmpty { null }, notes = notes.trim().ifEmpty { null }))
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch { repo.remove(client) }
    }
}
