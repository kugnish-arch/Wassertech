package ru.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.*
import ru.wassertech.data.types.ComponentType
import ru.wassertech.sync.DeletionTracker
import ru.wassertech.sync.SafeDeletionHelper
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import ru.wassertech.sync.markArchivedForSync
import ru.wassertech.sync.markUnarchivedForSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
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
    private val iconDao = db.iconDao()
    private val iconPackDao = db.iconPackDao()
    private val iconRepository = ru.wassertech.data.repository.IconRepository(application)

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
                ).markCreatedForSync()
            )
        }
    }

    // ---------------------------------------------------------------------
    // 2) ОБЪЕКТЫ (Sites)
    // ---------------------------------------------------------------------

    fun sites(clientId: String, includeArchived: Boolean = false): Flow<List<SiteEntity>> =
        if (includeArchived) hierarchyDao.observeSitesIncludingArchived(clientId)
        else hierarchyDao.observeSites(clientId)

    /** Поток одного объекта по id. */
    fun site(id: String): Flow<SiteEntity?> = hierarchyDao.observeSite(id)
        .onEach { site ->
            android.util.Log.d("HierarchyViewModel", "site Flow updated: id=$id, iconId=${site?.iconId}")
        }

    /** Алиас для читаемости в UI: observeSite(id). */
    fun observeSite(id: String): Flow<SiteEntity?> = site(id)

    suspend fun getSite(id: String): SiteEntity? =
        withContext(Dispatchers.IO) { hierarchyDao.getSite(id) }

    fun editSite(site: SiteEntity) {
        viewModelScope.launch(Dispatchers.IO) { 
            hierarchyDao.upsertSite(site.markUpdatedForSync()) 
        }
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
                ).markCreatedForSync(getApplication())
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

    fun installations(siteId: String, includeArchived: Boolean = false): Flow<List<InstallationEntity>> =
        if (includeArchived) hierarchyDao.observeInstallationsIncludingArchived(siteId)
        else hierarchyDao.observeInstallations(siteId)

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
                ).markCreatedForSync(getApplication())
            )
        }
    }

    fun addInstallationToSite(siteId: String, name: String) = addInstallation(siteId, name)

    /** Создать установку в «Главном» объекте (создаст объект при отсутствии). */
    fun addInstallationToMain(clientId: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSites = hierarchyDao.observeSites(clientId).first()
            val main = currentSites.firstOrNull { it.name.equals("Основной", ignoreCase = true) }
                ?: run {
                    val newId = UUID.randomUUID().toString()
                    val site = SiteEntity(
                        id = newId,
                        clientId = clientId,
                        name = "Основной",
                        address = null,
                        orderIndex = 0
                    ).markCreatedForSync(getApplication())
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
                hierarchyDao.updateInstallation(inst.copy(name = newName).markUpdatedForSync())
            }
        }
    }
    
    /** Изменить объект установки. */
    fun moveInstallationToSite(installationId: String, newSiteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val inst = hierarchyDao.getInstallation(installationId)
            if (inst != null) {
                hierarchyDao.updateInstallation(inst.copy(siteId = newSiteId).markUpdatedForSync())
            }
        }
    }
    
    /** Переименовать установку и изменить объект. */
    fun updateInstallation(installationId: String, newName: String, newSiteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val inst = hierarchyDao.getInstallation(installationId)
            if (inst != null) {
                hierarchyDao.updateInstallation(inst.copy(name = newName, siteId = newSiteId).markUpdatedForSync())
            }
        }
    }

    fun reorderInstallations(list: List<InstallationEntity>) {
        viewModelScope.launch(Dispatchers.IO) { hierarchyDao.updateInstallations(list) }
    }

    fun reorderInstallations(siteId: String, orderIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = hierarchyDao.observeInstallations(siteId).first()
            val ordered = orderIds.mapIndexedNotNull { idx, id ->
                current.firstOrNull { it.id == id }?.copy(orderIndex = idx)
            }
            hierarchyDao.updateInstallations(ordered)
        }
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
                ).markCreatedForSync(getApplication())
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
            DeletionTracker.markComponentDeleted(db, componentId)
        }
    }
    
    
    /** Удалить объект по id. */
    fun deleteSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SafeDeletionHelper.deleteSite(db, siteId)
        }
    }
    
    /** Архивировать объект. */
    fun archiveSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val site = hierarchyDao.getSite(siteId) ?: return@launch
            hierarchyDao.upsertSite(site.markArchivedForSync())
        }
    }
    
    /** Восстановить объект из архива. */
    fun restoreSite(siteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val site = hierarchyDao.getSite(siteId) ?: return@launch
            hierarchyDao.upsertSite(site.markUnarchivedForSync())
        }
    }
    
    /** Архивировать установку. */
    fun archiveInstallation(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val installation = hierarchyDao.getInstallation(installationId) ?: return@launch
            hierarchyDao.updateInstallation(installation.markArchivedForSync())
        }
    }
    
    /** Восстановить установку из архива. */
    fun restoreInstallation(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val installation = hierarchyDao.getInstallation(installationId) ?: return@launch
            hierarchyDao.updateInstallation(installation.markUnarchivedForSync())
        }
    }
    
    /** Удалить установку по id (для архивных). */
    fun deleteInstallation(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SafeDeletionHelper.deleteInstallation(db, installationId)
        }
    }
    
    /** Архивировать компонент. */
    fun archiveComponent(componentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            hierarchyDao.archiveComponent(componentId)
        }
    }
    
    /** Восстановить компонент из архива. */
    fun restoreComponent(componentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            hierarchyDao.restoreComponent(componentId)
        }
    }
    
    // ---------------------------------------------------------------------
    // 5) ИКОНКИ (Icons)
    // ---------------------------------------------------------------------
    
    /** Получить иконку по ID. */
    suspend fun getIcon(iconId: String?): IconEntity? {
        if (iconId == null) return null
        return withContext(Dispatchers.IO) {
            iconDao.getById(iconId)
        }
    }
    
    /** Получить Flow иконки по ID. */
    fun icon(iconId: String?): Flow<IconEntity?> {
        if (iconId == null) {
            android.util.Log.d("HierarchyViewModel", "icon: iconId is null, returning null")
            return kotlinx.coroutines.flow.flowOf(null)
        }
        android.util.Log.d("HierarchyViewModel", "icon: Looking for iconId=$iconId")
        return iconDao.observeAllActive().map { icons ->
            val found = icons.firstOrNull { it.id == iconId }
            android.util.Log.d("HierarchyViewModel", 
                "icon: Found ${if (found != null) "icon id=${found.id}, label=${found.label}" else "nothing"} in ${icons.size} active icons"
            )
            found
        }
    }
    
    /** Получить Flow иконки по ID с автоматическим обновлением при изменении iconId. */
    fun iconFlow(iconIdFlow: Flow<String?>): Flow<IconEntity?> {
        return iconIdFlow.flatMapLatest { iconId ->
            android.util.Log.d("HierarchyViewModel", "iconFlow: iconId changed to $iconId")
            icon(iconId).onEach { iconEntity ->
                android.util.Log.d("HierarchyViewModel", 
                    "iconFlow: Found icon for iconId=$iconId: ${if (iconEntity != null) "id=${iconEntity.id}, label=${iconEntity.label}, androidResName=${iconEntity.androidResName}, code=${iconEntity.code}" else "null"}"
                )
            }
        }
    }
    
    /** Загрузить паки и иконки для типа сущности. */
    suspend fun loadIconPacksAndIconsFor(entityType: ru.wassertech.core.ui.icons.IconEntityType): ru.wassertech.core.ui.icons.IconPickerUiState {
        return withContext(Dispatchers.IO) {
            val packs = iconPackDao.getAll()
            val allIcons = iconDao.getAllActive()
            val filteredIcons = allIcons.filter { icon ->
                icon.entityType == "ANY" || icon.entityType == entityType.name
            }
            
            // Логирование для отладки
            android.util.Log.d("HierarchyViewModel", 
                "Loading icons for entityType=${entityType.name}, total icons=${allIcons.size}, filtered=${filteredIcons.size}"
            )
            filteredIcons.take(5).forEach { icon ->
                android.util.Log.d("HierarchyViewModel", 
                    "Icon sample: id=${icon.id}, label=${icon.label}, " +
                    "androidResName=${icon.androidResName}, code=${icon.code}, " +
                    "imageUrl=${icon.imageUrl}, entityType=${icon.entityType}"
                )
            }
            
            val iconsByPack = filteredIcons.groupBy { it.packId }
            ru.wassertech.core.ui.icons.IconPickerUiState(
                packs = packs.map { 
                    ru.wassertech.core.ui.components.IconPackUiData(
                        id = it.id,
                        name = it.name
                    )
                },
                iconsByPack = iconsByPack.mapValues { (_, icons) ->
                    icons.map { icon ->
                        val localPath = iconRepository.getLocalIconPath(icon.id)
                        ru.wassertech.core.ui.components.IconUiData(
                            id = icon.id,
                            packId = icon.packId,
                            label = icon.label,
                            entityType = icon.entityType,
                            androidResName = icon.androidResName,
                            code = icon.code, // Передаем code для fallback
                            localImagePath = localPath // Передаем локальный путь к файлу
                        )
                    }
                }
            )
        }
    }
    
    /** Обновить иконку объекта. */
    fun updateSiteIcon(siteId: String, iconId: String?) {
        android.util.Log.d("HierarchyViewModel", "updateSiteIcon: siteId=$siteId, iconId=$iconId")
        viewModelScope.launch(Dispatchers.IO) {
            val site = hierarchyDao.getSite(siteId) ?: run {
                android.util.Log.w("HierarchyViewModel", "updateSiteIcon: Site not found, siteId=$siteId")
                return@launch
            }
            android.util.Log.d("HierarchyViewModel", "updateSiteIcon: Current site iconId=${site.iconId}, updating to $iconId")
            val updatedSite = site.copy(iconId = iconId).markUpdatedForSync()
            
            // Используем @Update вместо @Insert для гарантированного обновления Flow
            hierarchyDao.updateSites(listOf(updatedSite))
            
            // Проверяем, что данные действительно обновились
            val verifySite = hierarchyDao.getSite(siteId)
            android.util.Log.d("HierarchyViewModel", "updateSiteIcon: Verification - site.iconId=${verifySite?.iconId}")
            
            // Проверяем, что иконка существует в БД
            if (iconId != null) {
                val icon = iconDao.getById(iconId)
                android.util.Log.d("HierarchyViewModel", "updateSiteIcon: Icon exists: ${icon != null}, icon.isActive=${icon?.isActive}, icon.label=${icon?.label}")
            }
            
            android.util.Log.d("HierarchyViewModel", "updateSiteIcon: Site updated successfully")
        }
    }
    
    /** Обновить иконку установки. */
    fun updateInstallationIcon(installationId: String, iconId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val installation = hierarchyDao.getInstallation(installationId) ?: return@launch
            hierarchyDao.updateInstallation(installation.copy(iconId = iconId).markUpdatedForSync())
        }
    }
    
    /** Обновить иконку компонента. */
    fun updateComponentIcon(componentId: String, iconId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val component = hierarchyDao.getComponent(componentId) ?: return@launch
            hierarchyDao.upsertComponent(component.copy(iconId = iconId).markUpdatedForSync())
        }
    }
}
