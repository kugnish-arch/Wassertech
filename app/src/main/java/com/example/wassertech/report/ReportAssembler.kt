package com.example.wassertech.report

import android.content.Context
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.report.model.ComponentRowDTO
import com.example.wassertech.report.model.ComponentWithFieldsDTO
import com.example.wassertech.report.model.ComponentFieldDTO
import com.example.wassertech.report.model.ReportDTO
import com.example.wassertech.report.model.WaterAnalysisItem
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
        val dateFmtRus = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val reportDate = session.startedAtEpoch?.let { dateFmt.format(Date(it)) } ?: "Не указана"
        val reportDateRus = session.startedAtEpoch?.let { dateFmtRus.format(Date(it)) } ?: ""
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

        // Загружаем конфигурацию компании
        val (companyConfig, contractConfig) = CompanyConfigLoader.loadConfig(context)

        // Формируем список выполненных работ из наблюдений
        val works = observationTexts.takeIf { it.isNotEmpty() } ?: listOf("Плановое ТО установки водоподготовки")

        // Извлекаем данные анализов воды из наблюдений (можно улучшить логику позже)
        val waterAnalyses = emptyList<WaterAnalysisItem>() // Пока пусто, можно заполнить позже

        // Собираем данные из maintenance_values с разрешением меток полей
        val maintenanceValues = db.sessionsDao().getValuesForSession(sessionId)
        val valuesByComponent = maintenanceValues.groupBy { it.componentId }
        
        val componentsWithFields = mutableListOf<ComponentWithFieldsDTO>()
        for ((componentId, values) in valuesByComponent) {
            val component = db.hierarchyDao().getComponent(componentId)
            val componentName = component?.name ?: componentId
            val componentType = component?.type?.name
            
            // Получаем метки полей из шаблона
            val fieldLabels: Map<String, String> = component?.templateId?.let { templateId ->
                try {
                    db.templatesDao().getMaintenanceFieldsForTemplate(templateId)
                        .associate { it.key to (it.label ?: it.key) }
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            // Получаем единицы измерения из шаблона
            val fieldUnits: Map<String, String?> = component?.templateId?.let { templateId ->
                try {
                    db.templatesDao().getMaintenanceFieldsForTemplate(templateId)
                        .associate { it.key to it.unit }
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            // Получаем типы полей из шаблона для определения чекбоксов
            val fieldTypes: Map<String, com.example.wassertech.data.types.FieldType> = component?.templateId?.let { templateId ->
                try {
                    db.templatesDao().getMaintenanceFieldsForTemplate(templateId)
                        .associate { it.key to it.type }
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            val fields = values.mapNotNull { value ->
                val label = fieldLabels[value.fieldKey] ?: value.fieldKey.substringBefore('_', value.fieldKey)
                val fieldType = fieldTypes[value.fieldKey]
                val valueText = when {
                    value.valueText != null -> value.valueText
                    value.valueBool != null -> if (value.valueBool == true) "Да" else "Нет"
                    else -> null
                }
                
                // Определяем класс для чекбоксов
                val checkboxClass = if (fieldType == com.example.wassertech.data.types.FieldType.CHECKBOX) {
                    when (valueText) {
                        "Да" -> " checkbox-yes"
                        "Нет" -> " checkbox-no"
                        else -> null
                    }
                } else null
                
                valueText?.let {
                    ComponentFieldDTO(
                        label = label,
                        value = it,
                        unit = fieldUnits[value.fieldKey],
                        checkboxClass = checkboxClass
                    )
                }
            }.sortedBy { it.label.lowercase(Locale.getDefault()) }
            
            if (fields.isNotEmpty()) {
                componentsWithFields.add(
                    ComponentWithFieldsDTO(
                        componentName = componentName,
                        componentType = componentType,
                        fields = fields
                    )
                )
            }
        }
        
        // Сортируем компоненты по имени
        componentsWithFields.sortBy { it.componentName.lowercase(Locale.getDefault()) }

        ReportDTO(
            reportNumber = "TO-${sessionId.take(8).uppercase()}",
            reportDate = reportDate,
            reportDateRus = reportDateRus,

            companyName = companyConfig?.legal_name ?: "Wassertech",
            engineerName = session.technician ?: "Инженер",

            clientName = client?.name ?: "Клиент",
            clientAddress = client?.addressFull ?: "",
            clientPhone = client?.phone ?: "",
            clientSignName = client?.contactPerson,

            siteName = site?.name ?: "",
            installationName = installation?.name ?: "",
            installationLocation = "",

            components = rows,
            observations = observationTexts,
            conclusions = session.notes ?: "",
            nextMaintenanceDate = nextDate,

            works = works,
            waterAnalyses = waterAnalyses,
            comments = session.notes ?: "",

            companyConfig = companyConfig,
            contractConfig = contractConfig,

            logoAssetPath = "img/logo-wassertech-bolder.png",
            
            componentsWithFields = componentsWithFields
        )
    }
}