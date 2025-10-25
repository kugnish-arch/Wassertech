package com.example.wassertech.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyDao {
    @Query("SELECT * FROM clients ORDER BY name")
    fun observeClients(): Flow<List<ClientEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(c: ClientEntity)
    @Delete
    suspend fun deleteClient(c: ClientEntity)

    @Query("SELECT * FROM sites WHERE clientId=:clientId ORDER BY name")
    fun observeSites(clientId: String): Flow<List<SiteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSite(s: SiteEntity)
    @Delete
    suspend fun deleteSite(s: SiteEntity)

    @Query("SELECT * FROM installations WHERE siteId=:siteId ORDER BY name")
    fun observeInstallations(siteId: String): Flow<List<InstallationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstallation(i: InstallationEntity)
    @Delete
    suspend fun deleteInstallation(i: InstallationEntity)

    @Query("SELECT * FROM components WHERE installationId=:installationId ORDER BY name")
    fun observeComponents(installationId: String): Flow<List<ComponentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponent(c: ComponentEntity)
    @Delete
    suspend fun deleteComponent(c: ComponentEntity)
}
