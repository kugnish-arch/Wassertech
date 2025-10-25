package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyDao {

    // Clients
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun observeClients(): Flow<List<ClientEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertClient(e: ClientEntity)
    @Query("DELETE FROM clients WHERE id = :id") suspend fun deleteClient(id: String)
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1") suspend fun getClient(id: String): ClientEntity?

    // Sites
    @Query("SELECT * FROM sites WHERE clientId = :clientId ORDER BY position ASC, name COLLATE NOCASE")
    fun observeSites(clientId: String): Flow<List<SiteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSite(e: SiteEntity)
    @Query("DELETE FROM sites WHERE id = :id") suspend fun deleteSite(id: String)
    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1") suspend fun getSite(id: String): SiteEntity?
    @Query("SELECT * FROM sites WHERE clientId = :clientId AND name = :name LIMIT 1") suspend fun findSiteByName(clientId: String, name: String): SiteEntity?
    @Query("SELECT COALESCE(MAX(position), -1) FROM sites WHERE clientId = :clientId") suspend fun maxSitePosition(clientId: String): Int
    @Query("UPDATE sites SET position = :position WHERE id = :id") suspend fun updateSitePosition(id: String, position: Int)

    // Installations
    @Query("SELECT * FROM installations WHERE siteId = :siteId ORDER BY position ASC, name COLLATE NOCASE")
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertInstallation(e: InstallationEntity)
    @Query("DELETE FROM installations WHERE id = :id") suspend fun deleteInstallation(id: String)
    @Query("SELECT * FROM installations WHERE id = :id LIMIT 1") suspend fun getInstallation(id: String): InstallationEntity?
    @Query("SELECT COALESCE(MAX(position), -1) FROM installations WHERE siteId = :siteId") suspend fun maxInstallationPosition(siteId: String): Int
    @Query("UPDATE installations SET position = :position WHERE id = :id") suspend fun updateInstallationPosition(id: String, position: Int)

    // Components
    @Query("SELECT * FROM components WHERE installationId = :installationId ORDER BY position ASC, name COLLATE NOCASE")
    fun observeComponents(installationId: String): Flow<List<ComponentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertComponent(e: ComponentEntity)
    @Query("DELETE FROM components WHERE id = :id") suspend fun deleteComponent(id: String)
    @Query("SELECT COALESCE(MAX(position), -1) FROM components WHERE installationId = :installationId")
    suspend fun maxPosition(installationId: String): Int
    @Query("UPDATE components SET position = :position WHERE id = :id") suspend fun updateComponentPosition(id: String, position: Int)
    @Query("SELECT * FROM components WHERE id = :id LIMIT 1") suspend fun getComponent(id: String): ComponentEntity?
}
