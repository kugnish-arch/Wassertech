package ru.wassertech.data.dao

import androidx.room.*
import ru.wassertech.data.entities.*
import kotlinx.coroutines.flow.Flow

import ru.wassertech.data.entities.ComponentEntity
import ru.wassertech.data.entities.InstallationEntity
import ru.wassertech.data.entities.SiteEntity
@Dao
interface HierarchyDao {

    // ---- Sites ----

    /** Поток списка объектов клиента, отсортированный по orderIndex, затем по имени. */
    @Query("""
        SELECT * FROM sites 
        WHERE clientId = :clientId 
        AND (isArchived = 0 OR isArchived IS NULL)
        ORDER BY orderIndex ASC, name COLLATE NOCASE
    """)
    fun observeSites(clientId: String): Flow<List<SiteEntity>>
    
    /** Поток списка объектов клиента, включая архивные. */
    @Query("""
        SELECT * FROM sites 
        WHERE clientId = :clientId 
        ORDER BY orderIndex ASC, name COLLATE NOCASE
    """)
    fun observeSitesIncludingArchived(clientId: String): Flow<List<SiteEntity>>

    /** Поток одного объекта по id (удобно для подписи «Объект: Клиент — Объект»). */
    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    fun observeSite(id: String): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSite(id: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSite(site: SiteEntity)

    @Update
    suspend fun updateSites(sites: List<SiteEntity>)

    @Transaction
    suspend fun reorderSites(newOrder: List<SiteEntity>) {
        // orderIndex уже пересчитан на стороне VM/UI
        updateSites(newOrder)
    }

    // ---- Installations ----

    @Update
    suspend fun updateInstallation(installation: InstallationEntity)

    @Query("""
        SELECT * FROM installations 
        WHERE siteId = :siteId 
        AND (isArchived = 0 OR isArchived IS NULL)
        ORDER BY orderIndex ASC, name COLLATE NOCASE
    """)
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>
    
    /** Поток списка установок, включая архивные. */
    @Query("""
        SELECT * FROM installations 
        WHERE siteId = :siteId 
        ORDER BY orderIndex ASC, name COLLATE NOCASE
    """)
    fun observeInstallationsIncludingArchived(siteId: String): Flow<List<InstallationEntity>>

    /** Поток одной установки по id. */
    @Query("SELECT * FROM installations WHERE id = :id LIMIT 1")
    fun observeInstallation(id: String): Flow<InstallationEntity?>

    @Query("SELECT * FROM installations WHERE id = :id LIMIT 1")
    suspend fun getInstallation(id: String): InstallationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstallation(installation: InstallationEntity)

    @Update
    suspend fun updateInstallations(list: List<InstallationEntity>)

    @Transaction
    suspend fun reorderInstallations(newOrder: List<InstallationEntity>) {
        updateInstallations(newOrder)
    }

    // ---- Components ----

    @Query("SELECT * FROM components WHERE installationId = :installationId ORDER BY orderIndex ASC, name COLLATE NOCASE")
    fun observeComponents(installationId: String): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE id = :id LIMIT 1")
    suspend fun getComponent(id: String): ComponentEntity?
    
    /** Получить все компоненты, использующие указанный шаблон */
    @Query("SELECT * FROM components WHERE templateId = :templateId")
    suspend fun getComponentsByTemplate(templateId: String): List<ComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponent(component: ComponentEntity)

    @Update
    suspend fun updateComponents(list: List<ComponentEntity>)

    @Transaction
    suspend fun reorderComponents(newOrder: List<ComponentEntity>) {
        updateComponents(newOrder)
    }

    /** Дублирующий поток (если где-то использовался ранее) — оставлен для совместимости. */
    @Query("SELECT * FROM components WHERE installationId = :installationId ORDER BY (orderIndex IS NULL), orderIndex, id")
    fun observeComponentsByInstallation(installationId: String): Flow<List<ComponentEntity>>

    /** Удалить компонент по id. */
    @Query("DELETE FROM components WHERE id = :componentId")
    suspend fun deleteComponent(componentId: String)
    
    /** Удалить установку по id. */
    @Query("DELETE FROM installations WHERE id = :installationId")
    suspend fun deleteInstallation(installationId: String)
    
    /** Удалить объект по id. */
    @Query("DELETE FROM sites WHERE id = :siteId")
    suspend fun deleteSite(siteId: String)

    @Query("SELECT * FROM installations WHERE id = :id LIMIT 1")
    suspend fun getInstallationNow(id: String): InstallationEntity?

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSiteNow(id: String): SiteEntity?

    @Query("""
    SELECT * FROM components
    WHERE installationId = :installationId
    ORDER BY 
      CASE WHEN orderIndex IS NULL THEN 1 ELSE 0 END,
      orderIndex ASC, name COLLATE NOCASE ASC
""")
    suspend fun getComponentsNow(installationId: String): List<ComponentEntity>

    /** Получить все объекты для синхронизации */
    @Query("SELECT * FROM sites")
    fun getAllSitesNow(): List<SiteEntity>

    /** Получить все установки для синхронизации */
    @Query("SELECT * FROM installations")
    fun getAllInstallationsNow(): List<InstallationEntity>

    /** Получить все компоненты для синхронизации */
    @Query("SELECT * FROM components")
    fun getAllComponentsNow(): List<ComponentEntity>

    /** Вставка объекта (для синхронизации) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    /** Вставка установки (для синхронизации) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallation(installation: InstallationEntity)

    /** Вставка компонента (для синхронизации) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: ComponentEntity)
    
    // ========== Методы синхронизации ==========
    
    /** Получить все "грязные" объекты */
    @Query("SELECT * FROM sites WHERE dirtyFlag = 1")
    fun getDirtySitesNow(): List<SiteEntity>
    
    /** Получить все "грязные" установки */
    @Query("SELECT * FROM installations WHERE dirtyFlag = 1")
    fun getDirtyInstallationsNow(): List<InstallationEntity>
    
    /** Получить все "грязные" компоненты */
    @Query("SELECT * FROM components WHERE dirtyFlag = 1")
    fun getDirtyComponentsNow(): List<ComponentEntity>
    
    /** Пометить объекты как синхронизированные */
    @Query("""
        UPDATE sites 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markSitesAsSynced(ids: List<String>)
    
    /** Пометить установки как синхронизированные */
    @Query("""
        UPDATE installations 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markInstallationsAsSynced(ids: List<String>)
    
    /** Пометить компоненты как синхронизированные */
    @Query("""
        UPDATE components 
        SET dirtyFlag = 0, syncStatus = 0 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentsAsSynced(ids: List<String>)
    
    /** Пометить объекты как конфликтные */
    @Query("""
        UPDATE sites 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markSitesAsConflict(ids: List<String>)
    
    /** Пометить установки как конфликтные */
    @Query("""
        UPDATE installations 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markInstallationsAsConflict(ids: List<String>)
    
    /** Пометить компоненты как конфликтные */
    @Query("""
        UPDATE components 
        SET dirtyFlag = 0, syncStatus = 2 
        WHERE id IN (:ids)
    """)
    suspend fun markComponentsAsConflict(ids: List<String>)
    
    /** Снять флаг "грязный" у объектов */
    @Query("""
        UPDATE sites 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearSitesDirtyFlag(ids: List<String>)
    
    /** Снять флаг "грязный" у установок */
    @Query("""
        UPDATE installations 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearInstallationsDirtyFlag(ids: List<String>)
    
    /** Снять флаг "грязный" у компонентов */
    @Query("""
        UPDATE components 
        SET dirtyFlag = 0 
        WHERE id IN (:ids)
    """)
    suspend fun clearComponentsDirtyFlag(ids: List<String>)
    
    // ---- Методы для архивации/восстановления компонентов ----
    
    /** Архивировать компонент */
    @Query("""
        UPDATE components
        SET isArchived = 1, archivedAtEpoch = :timestamp, updatedAtEpoch = :timestamp, dirtyFlag = 1, syncStatus = 1
        WHERE id = :componentId
    """)
    suspend fun archiveComponent(componentId: String, timestamp: Long = System.currentTimeMillis())
    
    /** Восстановить компонент из архива */
    @Query("""
        UPDATE components
        SET isArchived = 0, archivedAtEpoch = NULL, updatedAtEpoch = :timestamp, dirtyFlag = 1, syncStatus = 1
        WHERE id = :componentId
    """)
    suspend fun restoreComponent(componentId: String, timestamp: Long = System.currentTimeMillis())
}