package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HierarchyViewModel(app: Application): AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(app) }
    private val dao by lazy { db.hierarchyDao() }

    fun clients(): Flow<List<ClientEntity>> = dao.observeClients()
    fun sites(clientId: String): Flow<List<SiteEntity>> = dao.observeSites(clientId)
    fun installations(siteId: String): Flow<List<InstallationEntity>> = dao.observeInstallations(siteId)
    fun components(installationId: String): Flow<List<ComponentEntity>> = dao.observeComponents(installationId)

    fun addClient(name: String, notes: String?) = viewModelScope.launch {
        dao.upsertClient(ClientEntity(UUID.randomUUID().toString(), name, null, notes))
    }
    fun deleteClient(id: String) = viewModelScope.launch { dao.deleteClient(id) }

    fun addSite(clientId: String, name: String, address: String?) = viewModelScope.launch {
        dao.upsertSite(SiteEntity(UUID.randomUUID().toString(), clientId, name, address))
    }
    fun deleteSite(id: String) = viewModelScope.launch { dao.deleteSite(id) }

    fun addInstallation(siteId: String, name: String) = viewModelScope.launch {
        dao.upsertInstallation(InstallationEntity(UUID.randomUUID().toString(), siteId, name))
    }
    fun deleteInstallation(id: String) = viewModelScope.launch { dao.deleteInstallation(id) }

    fun addComponent(installationId: String, name: String, type: com.example.wassertech.data.types.ComponentType) = viewModelScope.launch {
        dao.upsertComponent(ComponentEntity(UUID.randomUUID().toString(), installationId, name, type))
    }
    fun deleteComponent(id: String) = viewModelScope.launch { dao.deleteComponent(id) }

    suspend fun getClient(id: String) = dao.getClient(id)
    suspend fun getSite(id: String) = dao.getSite(id)
    suspend fun getInstallation(id: String) = dao.getInstallation(id)
    suspend fun getComponent(id: String) = dao.getComponent(id)
}
