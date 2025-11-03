package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel для иерархии: Клиенты → Объекты → Установки → Компоненты.
 * Добавлены удобные observe* алиасы, чтобы их можно было вызывать из UI.
 */
class HierarchyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val clientDao = db.clientDao()

    // ---------------------------------------------------------------------
    // 1) КЛИЕНТЫ
    // ---------------------------------------------------------------------

    fun clients(includeArchived: Boolean = false): Flow<List<ClientEntity>> =
        if (includeArchived) clientDao.observeClients(true) else clientDao.observeClients()

    /** Поток одного клиента по id. */
    fun client(id: String): Flow<ClientEntity?> =
        clientDao.observeAllClients().map { list -> list.firstOrNull { it.id == id } }

    suspend fun getClient(id: String): ClientEntity? =
        withContext(Dispatchers.IO) { clientDao.getClient(id) }

    fun addClient(name: String, notes: String?, isCorporate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            clientDao.upsertClient(
                ClientEntity(
                    id = id,
                    name = name,
                    phone = null,
                    notes = notes,
                    isCorporate = isCorporate
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // 2) ОБЪЕКТЫ (Sites)
    // ---------------------------------------------------------------------

    fun sites(clientId: String): Flow<List<SiteEntity>> =
        hierarchyDao.observeSites(clientId)

    /** Поток одного объекта по id. */
    fun site(id: String): Flow<SiteEntity?> = hierarchyDao.observeSite(id)

    /** Алиас для читаемости в UI: observeSite(id). */
    fun observeSite(id: String): Flow<SiteEntity?> = site(id)

    suspend fun getSite(id: String): SiteEntity? =
        withContext(Dispatchers.IO) { hierarchyDao.getSite(id) }

    fun editSite(site: SiteEntity) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.upsertSite(site) }
    }

    fun addSite(clientId: String, name: String, address: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertSite(
                SiteEntity(
                    id = id,
                    clientId = clientId,
                    name = name,
                    address = address,
                    orderIndex = 0
                )
            )
        }
    }

    fun reorderSites(sites: List<SiteEntity>) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.updateSites(sites) }
    }

    fun reorderSites(clientId: String, orderIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = hierarchyDao.observeSites(clientId).first()
            val ordered = orderIds.mapIndexedNotNull { idx, id ->
                current.firstOrNull { it.id == id }?.copy(orderIndex = idx)
            }
            hierarchyDao.updateSites(ordered)
        }
    }

    // ---------------------------------------------------------------------
    // 3) УСТАНОВКИ (Installations)
    // ---------------------------------------------------------------------

    fun installations(siteId: String): Flow<List<InstallationEntity>> =
        hierarchyDao.observeInstallations(siteId)

    /** Поток одной установки по id. */
    fun installation(id: String): Flow<InstallationEntity?> =
        hierarchyDao.observeInstallation(id)

    /** Алиас для читаемости в UI: observeInstallation(id). */
    fun observeInstallation(id: String): Flow<InstallationEntity?> = installation(id)

    suspend fun getInstallation(id: String): InstallationEntity? =
        withContext(Dispatchers.IO) { hierarchyDao.getInstallation(id) }

    fun addInstallation(siteId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertInstallation(
                InstallationEntity(
                    id = id,
                    siteId = siteId,
                    name = name,
                    orderIndex = 0
                )
            )
        }
    }

    fun addInstallationToSite(siteId: String, name: String) = addInstallation(siteId, name)

    /** Создать установку в «Главном» объекте (создаст объект при отсутствии). */
    fun addInstallationToMain(clientId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSites = hierarchyDao.observeSites(clientId).first()
            val main = currentSites.firstOrNull { it.name.equals("Главный", ignoreCase = true) }
                ?: run {
                    val newId = UUID.randomUUID().toString()
                    val site = SiteEntity(
                        id = newId,
                        clientId = clientId,
                        name = "Главный",
                        address = null,
                        orderIndex = 0
                    )
                    hierarchyDao.upsertSite(site)
                    site
                }
            addInstallation(main.id, name)
        }
    }

    /** Переименовать установку. */
    fun renameInstallation(installationId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val inst = hierarchyDao.getInstallation(installationId)
            if (inst != null) {
                hierarchyDao.updateInstallation(inst.copy(name = newName))
            }
        }
    }

    fun reorderInstallations(list: List<InstallationEntity>) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.updateInstallations(list) }
    }

    // ---------------------------------------------------------------------
    // 4) КОМПОНЕНТЫ (Components)
    // ---------------------------------------------------------------------

    fun components(installationId: String): Flow<List<ComponentEntity>> =
        hierarchyDao.observeComponents(installationId)

    fun addComponentFromTemplate(
        installationId: String,
        name: String,
        type: ComponentType,
        templateId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            hierarchyDao.upsertComponent(
                ComponentEntity(
                    id = id,
                    installationId = installationId,
                    name = name,
                    type = type,            // тип в логике сейчас не критичен, оставлен для совместимости
                    orderIndex = 0,
                    templateId = templateId
                )
            )
        }
    }

    fun reorderComponents(list: List<ComponentEntity>) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.updateComponents(list) }
    }

    fun reorderComponents(installationId: String, orderIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = hierarchyDao.observeComponents(installationId).first()
            val ordered = orderIds.mapIndexedNotNull { idx, id ->
                current.firstOrNull { it.id == id }?.copy(orderIndex = idx)
            }
            hierarchyDao.updateComponents(ordered)
        }
    }

    /** Удалить компонент по id (иконка корзины на экране «Установка»). */
    fun deleteComponent(componentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            hierarchyDao.deleteComponent(componentId)
        }
    }
}