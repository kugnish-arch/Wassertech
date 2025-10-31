package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class HierarchyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val clientDao = db.clientDao()

    fun clients(includeArchived: Boolean = false): Flow<List<ClientEntity>> =
        if (includeArchived) clientDao.observeClients(true) else clientDao.observeClients()

    fun client(id: String): Flow<ClientEntity?> =
        clientDao.observeAllClients()
            .map { list -> list.firstOrNull { it.id == id } }

    fun sites(clientId: String): Flow<List<SiteEntity>> = hierarchyDao.observeSites(clientId)
    fun installations(siteId: String): Flow<List<InstallationEntity>> = hierarchyDao.observeInstallations(siteId)
    fun components(installationId: String): Flow<List<ComponentEntity>> = hierarchyDao.observeComponents(installationId)


    // ЧТЕНИЕ (были без контекста)
    suspend fun getClient(id: String): ClientEntity? =
        withContext(Dispatchers.IO) { clientDao.getClient(id) }

    suspend fun getSite(id: String): SiteEntity? =
        withContext(Dispatchers.IO) { hierarchyDao.getSite(id) }

    suspend fun getInstallation(id: String): InstallationEntity? =
        withContext(Dispatchers.IO) { hierarchyDao.getInstallation(id) }

    /*
    fun editClient(client: ClientEntity) { viewModelScope.launch { clientDao.upsertClient(client) } }

    fun editSite(site: SiteEntity) { viewModelScope.launch { hierarchyDao.upsertSite(site) } }
    fun editInstallation(installation: InstallationEntity) { viewModelScope.launch { hierarchyDao.upsertInstallation(installation) } }
    fun editComponent(component: ComponentEntity) { viewModelScope.launch { hierarchyDao.upsertComponent(component) } }
    */

    fun renameInstallation(installationId: String, newName: String) {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val dao = db.hierarchyDao()
            val inst = dao.getInstallation(installationId)
            if (inst != null) {
                val updated = inst.copy(name = newName)
                dao.updateInstallation(updated)
            }
        }
    }


    fun editSite(site: SiteEntity) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.upsertSite(site) }
    }

    fun addClient(name: String, notes: String?, isCorporate: Boolean) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            clientDao.upsertClient(ClientEntity(id, name, phone = null, notes = notes, isCorporate = isCorporate))
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

}
