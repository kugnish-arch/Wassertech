package ru.wassertech.client.ui.reports

import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.MaintenanceSessionEntity
import ru.wassertech.feature.reports.ReportsDatabaseProvider as IReportsDatabaseProvider
import ru.wassertech.feature.reports.InstallationData
import ru.wassertech.feature.reports.MaintenanceSessionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Провайдер базы данных для ReportsScreen
 */
class ReportsDatabaseProvider(private val database: AppDatabase) : IReportsDatabaseProvider {
    override fun getAllNonArchivedInstallations(): List<InstallationData> {
        return database.hierarchyDao().getAllNonArchivedInstallationsNow().map { entity ->
            InstallationData(
                id = entity.id,
                name = entity.name,
                siteId = entity.siteId,
                orderIndex = entity.orderIndex,
                isArchived = entity.isArchived
            )
        }
    }
    
    override fun observeSessionsByInstallation(installationId: String): Flow<List<MaintenanceSessionData>> {
        return database.sessionsDao().observeSessionsByInstallation(installationId).map { sessions ->
            sessions.map { entity ->
                MaintenanceSessionData(
                    id = entity.id,
                    siteId = entity.siteId,
                    installationId = entity.installationId,
                    startedAtEpoch = entity.startedAtEpoch,
                    finishedAtEpoch = entity.finishedAtEpoch,
                    technician = entity.technician,
                    notes = entity.notes
                )
            }
        }
    }
    
    override suspend fun getClientNameByInstallationId(installationId: String): String? {
        val installation = database.hierarchyDao().getInstallationNow(installationId) ?: return null
        val site = database.hierarchyDao().getSiteNow(installation.siteId) ?: return null
        val client = database.clientDao().getClientNow(site.clientId)
        return client.name
    }
}

