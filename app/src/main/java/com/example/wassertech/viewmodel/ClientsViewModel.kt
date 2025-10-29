package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ClientEntity
import kotlinx.coroutines.flow.Flow

class ClientsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val clientDao = db.clientDao()

    val clients: Flow<List<ClientEntity>> = clientDao.observeActiveClients()
}