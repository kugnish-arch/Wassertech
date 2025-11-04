package com.example.wassertech.report

import android.content.Context
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.report.model.ComponentRowDTO
import com.example.wassertech.report.model.ReportDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object ReportAssembler {

    suspend fun assemble(context: Context, sessionId: String): ReportDTO = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)

        // Получаем сессию (nullable)
        val session = db.sessionsDao().getSessionById(sessionId)

        // Без сессии - бросаем понятную ошибку
        requireNotNull(session) { "Session $sessionId not found" }

        // Installation может быть null (installationId в сессии nullable)
        val installation = session.installationId?.let { db.hierarchyDao().getInstallation(it) }
        val site = installation?.siteId?.let { sid -> db.hierarchyDao().getSite(sid) }
        val client = site?.clientId?.let { cid -> db.clientDao().getClient(cid) }

        // Компоненты установки — если нет установки, пустой список
        val components = installation?.let { db.hierarchyDao().getComponentsNow(it.id) } ?: emptyList()

        // Старые observations (если есть) — возвращаем список, сортировка handled in DAO
        val observations = db.sessionsDao().getObservations(sessionId)

        // Сформируем строки компонентов — используем лишь поля, которые реально есть в сущности
        val rows = components.map { cmp ->
            ComponentRowDTO(
                name = cmp.name,
                // ComponentEntity.type — non-null enum, безопасно брать name
                type = cmp.type.name,
                // В базе у ComponentEntity нет serialNumber/status/notes — ставим безопасные дефолты
                serial = "",
                status = "",
                notes = ""
            )
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val reportDate = dateFmt.format(Date(session.startedAtEpoch))
        val nextDate = null // нет поля nextMaintenance в сущности MaintenanceSessionEntity

        // Составляем строки наблюдений: выбираем текстовое представление значения
        val observationTexts = observations.mapNotNull { o ->
            val txt = when {
                o.valueText != null -> o.valueText
                o.valueNumber != null -> o.valueNumber.toString()
                o.valueBool != null -> if (o.valueBool) "Да" else "Нет"
                else -> null
            }
            txt?.takeIf { it.isNotBlank() }
        }

        ReportDTO(
            reportNumber = "TO-${sessionId.take(8).uppercase()}",
            reportDate = reportDate,

            companyName = "Wassertech",
            engineerName = session.technician ?: "Инженер",

            clientName = client?.name ?: "Клиент",
            // ReportDTO fields ожидают non-null String (исправляем nullable -> дефолт "")
            clientAddress = client?.addressFull ?: "",
            clientPhone = client?.phone ?: "",

            siteName = site?.name ?: "",
            installationName = installation?.name ?: "",
            installationLocation = "",

            components = rows,
            observations = observationTexts,
            conclusions = session.notes ?: "",
            nextMaintenanceDate = nextDate,

            logoAssetPath = "img/logo.png"
        )
    }
}