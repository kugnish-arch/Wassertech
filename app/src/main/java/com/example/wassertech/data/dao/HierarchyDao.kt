package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyDao {

    // ---- Sites ----

    /** Поток списка объектов клиента, отсортированный по orderIndex, затем по имени. */
    @Query("SELECT * FROM sites WHERE clientId = :clientId ORDER BY orderIndex ASC, name COLLATE NOCASE")
    fun observeSites(clientId: String): Flow<List<SiteEntity>>

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

    @Query("SELECT * FROM installations WHERE siteId = :siteId ORDER BY orderIndex ASC, name COLLATE NOCASE")
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>

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
}