package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wassertech.data.AppDatabase

class ClientsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientsViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            val dao = db.clientDao()
            return ClientsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
