package ru.wassertech.feature.reports

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для доступа к данным установок и сессий обслуживания
 */
interface ReportsDatabaseProvider {
    fun getAllNonArchivedInstallations(): List<InstallationData>
    fun observeSessionsByInstallation(installationId: String): Flow<List<MaintenanceSessionData>>
    suspend fun getClientNameByInstallationId(installationId: String): String?
}

/**
 * Данные установки
 */
data class InstallationData(
    val id: String,
    val name: String,
    val siteId: String,
    val orderIndex: Int,
    val isArchived: Boolean
)

/**
 * Данные сессии обслуживания
 */
data class MaintenanceSessionData(
    val id: String,
    val siteId: String,
    val installationId: String?,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long?,
    val technician: String?,
    val notes: String?
)

