package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.seed.TemplateSeeder
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class HierarchyViewModel(app: Application): AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(app) }
    private val dao by lazy { db.hierarchyDao() }

    init {
        viewModelScope.launch {
            try { TemplateSeeder.seedOnce(db) } catch (_: Throwable) {}
        }
    }

    fun clients(): Flow<List<ClientEntity>> = dao.observeClients()
    fun sites(clientId: String): Flow<List<SiteEntity>> = dao.observeSites(clientId)
    fun installations(siteId: String): Flow<List<InstallationEntity>> = dao.observeInstallations(siteId)
    fun components(installationId: String): Flow<List<ComponentEntity>> = dao.observeComponents(installationId)

    suspend fun getClient(id: String): ClientEntity? = dao.getClient(id)
    suspend fun getSite(id: String): SiteEntity? = dao.getSite(id)
    suspend fun getInstallation(id: String): InstallationEntity? = dao.getInstallation(id)

    fun addClient(name: String, notes: String?, isCorporate: Boolean) = viewModelScope.launch {
        dao.upsertClient(ClientEntity(UUID.randomUUID().toString(), name, null, notes, isCorporate))
    }
    fun editClient(entity: ClientEntity) = viewModelScope.launch { dao.upsertClient(entity) }
    fun deleteClient(id: String) = viewModelScope.launch { dao.deleteClient(id) }

    fun addSite(clientId: String, name: String, address: String?) = viewModelScope.launch {
        dao.upsertSite(SiteEntity(UUID.randomUUID().toString(), clientId, name, address))
    }
    fun editSite(entity: SiteEntity) = viewModelScope.launch { dao.upsertSite(entity) }
    fun deleteSite(id: String) = viewModelScope.launch { dao.deleteSite(id) }

    fun addInstallation(siteId: String, name: String) = viewModelScope.launch {
        dao.upsertInstallation(InstallationEntity(UUID.randomUUID().toString(), siteId, name))
    }
    fun editInstallation(entity: InstallationEntity) = viewModelScope.launch { dao.upsertInstallation(entity) }
    fun deleteInstallation(id: String) = viewModelScope.launch { dao.deleteInstallation(id) }

    fun addComponent(installationId: String, name: String, type: ComponentType) = viewModelScope.launch {
        val next = dao.maxPosition(installationId) + 1
        dao.upsertComponent(ComponentEntity(UUID.randomUUID().toString(), installationId, name, type, position = next))
    }
    fun deleteComponent(id: String) = viewModelScope.launch { dao.deleteComponent(id) }

    suspend fun getMainSiteIdForClient(clientId: String): String {
        val existing = dao.findSiteByName(clientId, "Главный")
        if (existing != null) return existing.id
        val newSite = SiteEntity(UUID.randomUUID().toString(), clientId, "Главный", null)
        dao.upsertSite(newSite)
        return newSite.id
    }

    fun addInstallationToMain(clientId: String, name: String) = viewModelScope.launch {
        val mainSiteId = getMainSiteIdForClient(clientId)
        dao.upsertInstallation(InstallationEntity(UUID.randomUUID().toString(), mainSiteId, name))
    }

    suspend fun getInstallationName(installationId: String): String? {
        return dao.getInstallation(installationId)?.name
    }

    fun reorderComponents(installationId: String, orderedIds: List<String>) = viewModelScope.launch {
        orderedIds.forEachIndexed { index, id -> dao.updateComponentPosition(id, index) }
    }
}
