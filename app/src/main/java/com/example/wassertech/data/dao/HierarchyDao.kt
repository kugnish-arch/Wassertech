package com.example.wassertech.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.wassertech.data.entities.*

@Dao
interface HierarchyDao {
    // Clients
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun observeClients(): Flow<List<ClientEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertClient(c: ClientEntity)
    @Query("DELETE FROM clients WHERE id = :id") suspend fun deleteClient(id: String)
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1") suspend fun getClient(id: String): ClientEntity?

    // Sites
    @Query("SELECT * FROM sites WHERE clientId = :clientId ORDER BY name COLLATE NOCASE")
    fun observeSites(clientId: String): Flow<List<SiteEntity>>
    @Query("SELECT * FROM sites WHERE clientId = :clientId AND name = :name LIMIT 1")
    suspend fun findSiteByName(clientId: String, name: String): SiteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSite(s: SiteEntity)
    @Query("DELETE FROM sites WHERE id = :id") suspend fun deleteSite(id: String)
    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1") suspend fun getSite(id: String): SiteEntity?

    // Installations
    @Query("SELECT * FROM installations WHERE siteId = :siteId ORDER BY name COLLATE NOCASE")
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertInstallation(i: InstallationEntity)
    @Query("DELETE FROM installations WHERE id = :id") suspend fun deleteInstallation(id: String)
    @Query("SELECT * FROM installations WHERE id = :id LIMIT 1") suspend fun getInstallation(id: String): InstallationEntity?

    // Components
    @Query("SELECT * FROM components WHERE installationId = :installationId ORDER BY position ASC, name COLLATE NOCASE")
    fun observeComponents(installationId: String): Flow<List<ComponentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertComponent(c: ComponentEntity)
    @Query("DELETE FROM components WHERE id = :id") suspend fun deleteComponent(id: String)
    @Query("SELECT * FROM components WHERE id = :id LIMIT 1") suspend fun getComponent(id: String): ComponentEntity?

    // Ordering helpers
    @Query("SELECT COALESCE(MAX(position), -1) FROM components WHERE installationId = :installationId")
    suspend fun maxPosition(installationId: String): Int

    @Query("UPDATE components SET position = :position WHERE id = :id")
    suspend fun updateComponentPosition(id: String, position: Int)
}
