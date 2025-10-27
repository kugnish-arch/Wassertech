package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class HierarchyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()

    fun clients(includeArchived: Boolean = false): Flow<List<ClientEntity>> =
        if (includeArchived) hierarchyDao.observeClients(true) else hierarchyDao.observeClients()

    fun sites(clientId: String): Flow<List<SiteEntity>> = hierarchyDao.observeSites(clientId)
    fun installations(siteId: String): Flow<List<InstallationEntity>> = hierarchyDao.observeInstallations(siteId)
    fun components(installationId: String): Flow<List<ComponentEntity>> = hierarchyDao.observeComponents(installationId)

    suspend fun getClient(id: String): ClientEntity? = hierarchyDao.getClient(id)
    suspend fun getSite(id: String): SiteEntity? = hierarchyDao.getSite(id)
    suspend fun getInstallation(id: String): InstallationEntity? = hierarchyDao.getInstallation(id)

    fun editClient(client: ClientEntity) { viewModelScope.launch { hierarchyDao.upsertClient(client) } }
    fun editSite(site: SiteEntity) { viewModelScope.launch { hierarchyDao.upsertSite(site) } }
    fun editInstallation(installation: InstallationEntity) { viewModelScope.launch { hierarchyDao.upsertInstallation(installation) } }
    fun editComponent(component: ComponentEntity) { viewModelScope.launch { hierarchyDao.upsertComponent(component) } }

    fun addClient(name: String, notes: String?, isCorporate: Boolean) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertClient(ClientEntity(id, name, phone = null, notes = notes, isCorporate = isCorporate))
        }
    }
    fun addSite(clientId: String, name: String, address: String?) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertSite(SiteEntity(id, clientId, name, address, orderIndex = 0))
        }
    }
    fun addInstallation(siteId: String, name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertInstallation(InstallationEntity(id, siteId, name, orderIndex = 0))
        }
    }
    fun addInstallationToSite(siteId: String, name: String) = addInstallation(siteId, name)

    fun addInstallationToMain(clientId: String, name: String) {
        viewModelScope.launch {
            val currentSites = hierarchyDao.observeSites(clientId).first()
            val main = currentSites.firstOrNull { it.name.equals("Главный", ignoreCase = true) }
                ?: run {
                    val newId = UUID.randomUUID().toString()
                    val site = SiteEntity(id = newId, clientId = clientId, name = "Главный", address = null, orderIndex = 0)
                    hierarchyDao.upsertSite(site); site
                }
            addInstallation(main.id, name)
        }
    }

    fun addComponentFromTemplate(installationId: String, name: String, type: ComponentType, templateId: String?) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertComponent(
                ComponentEntity(id, installationId, name, type, orderIndex = 0, templateId = templateId)
            )
        }
    }

    fun reorderSites(sites: List<SiteEntity>) { viewModelScope.launch { hierarchyDao.updateSites(sites) } }
    fun reorderInstallations(list: List<InstallationEntity>) { viewModelScope.launch { hierarchyDao.updateInstallations(list) } }
    fun reorderComponents(list: List<ComponentEntity>) { viewModelScope.launch { hierarchyDao.updateComponents(list) } }

    fun reorderSites(clientId: String, orderIds: List<String>) {
        viewModelScope.launch {
            val current = hierarchyDao.observeSites(clientId).first()
            val ordered = orderIds.mapIndexedNotNull { idx, id ->
                current.firstOrNull { it.id == id }?.copy(orderIndex = idx)
            }
            hierarchyDao.updateSites(ordered)
        }
    }
    fun reorderComponents(installationId: String, orderIds: List<String>) {
        viewModelScope.launch {
            val current = hierarchyDao.observeComponents(installationId).first()
            val ordered = orderIds.mapIndexedNotNull { idx, id ->
                current.firstOrNull { it.id == id }?.copy(orderIndex = idx)
            }
            hierarchyDao.updateComponents(ordered)
        }
    }

    fun archiveClient(clientId: String) = viewModelScope.launch {
        val c = getClient(clientId) ?: return@launch
        editClient(c.copy(isArchived = true, archivedAtEpoch = System.currentTimeMillis()))
    }
    fun restoreClient(clientId: String) = viewModelScope.launch {
        val c = getClient(clientId) ?: return@launch
        editClient(c.copy(isArchived = false, archivedAtEpoch = null))
    }
}
