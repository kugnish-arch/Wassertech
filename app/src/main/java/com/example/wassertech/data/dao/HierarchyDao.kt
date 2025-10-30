package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Update


@Dao
interface HierarchyDao {


    // ---- Sites ----
    @Query("SELECT * FROM sites WHERE clientId = :clientId ORDER BY orderIndex ASC, name COLLATE NOCASE")
    fun observeSites(clientId: String): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSite(id: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSite(site: SiteEntity)

    @Update
    suspend fun updateSites(sites: List<SiteEntity>)

    @Transaction
    suspend fun reorderSites(newOrder: List<SiteEntity>) {
        // assume orderIndex уже вычислен на стороне UI
        updateSites(newOrder)
    }

    // ---- Installations ----

    @Update
    suspend fun updateInstallation(installation: InstallationEntity)

    @Query("SELECT * FROM installations WHERE siteId = :siteId ORDER BY orderIndex ASC, name COLLATE NOCASE")
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>

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
}