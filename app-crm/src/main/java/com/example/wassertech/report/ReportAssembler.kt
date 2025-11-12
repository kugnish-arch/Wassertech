package ru.wassertech.report

import android.content.Context
import ru.wassertech.data.AppDatabase
import ru.wassertech.report.model.ComponentRowDTO
import ru.wassertech.report.model.ComponentWithFieldsDTO
import ru.wassertech.report.model.ComponentFieldDTO
import ru.wassertech.report.model.ReportDTO
import ru.wassertech.report.model.WaterAnalysisItem
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
        val dateFmtRus = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
        val reportDate = session.startedAtEpoch?.let { dateFmt.format(Date(it)) } ?: "Не указана"
        val reportDateRus = session.startedAtEpoch?.let { dateFmtRus.format(Date(it)) } ?: ""
        val nextDate = null // нет поля nextMaintenance в сущности MaintenanceSessionEntity
        
        // Генерируем номер отчета в формате АXXXXX/mmyy
        val reportNumber = ReportNumberGenerator.generateReportNumber(context)

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
        
        // Создаем множество ID компонентов, которые уже обработаны через valuesByComponent
        val processedComponentIds = mutableSetOf<String>()
        
        val componentsWithFields = mutableListOf<ComponentWithFieldsDTO>()
        for ((componentId, values) in valuesByComponent) {
            processedComponentIds.add(componentId)
            val component = db.hierarchyDao().getComponent(componentId)
            val componentName = component?.name ?: componentId
            
            // Получаем componentType из шаблона (COMMON или HEAD)
            var componentType: String? = "COMMON" // По умолчанию COMMON
            component?.templateId?.let { templateId ->
                try {
                    val template = db.templatesDao().getTemplateById(templateId)
                    componentType = template?.componentType?.name ?: "COMMON"
                } catch (e: Exception) {
                    // Если шаблон не найден, используем COMMON по умолчанию
                    componentType = "COMMON"
                }
            }
            
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
            val fieldTypes: Map<String, ru.wassertech.data.types.FieldType> = component?.templateId?.let { templateId ->
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
                val checkboxClass = if (fieldType == ru.wassertech.data.types.FieldType.CHECKBOX) {
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
            
            // ВАЖНО: Добавляем компонент даже если fields пуст, если это HEAD компонент
            // Это нужно для отображения заглавных компонентов в начале и конце отчёта
            val isHeadComponent = componentType == "HEAD"
            if (fields.isNotEmpty() || isHeadComponent) {
                componentsWithFields.add(
                    ComponentWithFieldsDTO(
                        componentName = componentName,
                        componentType = componentType,
                        fields = fields  // Может быть пустым для HEAD компонентов
                    )
                )
            }
        }
        
        // ВАЖНО: Обрабатываем HEAD компоненты, которые не имеют maintenance_values
        // Эти компоненты должны быть добавлены в отчёт даже без полей
        for (component in components) {
            if (component.id !in processedComponentIds) {
                // Получаем componentType из шаблона
                var componentType: String? = "COMMON"
                component.templateId?.let { templateId ->
                    try {
                        val template = db.templatesDao().getTemplateById(templateId)
                        componentType = template?.componentType?.name ?: "COMMON"
                    } catch (e: Exception) {
                        componentType = "COMMON"
                    }
                }
                
                // Добавляем HEAD компоненты даже без полей
                if (componentType == "HEAD") {
                    val headComponent = ComponentWithFieldsDTO(
                        componentName = component.name,
                        componentType = componentType,
                        fields = emptyList()  // Пустой список полей для HEAD компонентов без значений
                    )
                    componentsWithFields.add(headComponent)
                    android.util.Log.d("ReportAssembler", "Added HEAD component without fields: ${component.name}")
                }
            }
        }
        
        // Логируем перед сортировкой
        val headBeforeSort = componentsWithFields.count { it.componentType == "HEAD" }
        android.util.Log.d("ReportAssembler", "Before sort: ${componentsWithFields.size} components, $headBeforeSort HEAD")
        
        // Создаем мапу componentId -> orderIndex для сортировки
        val componentOrderMap = components.associate { it.name to it.orderIndex }
        
        // Сортируем компоненты по orderIndex, затем по имени
        componentsWithFields.sortWith(compareBy(
            { componentOrderMap[it.componentName] ?: Int.MAX_VALUE },
            { it.componentName.lowercase(Locale.getDefault()) }
        ))
        
        // Логируем после сортировки
        val headAfterSort = componentsWithFields.count { it.componentType == "HEAD" }
        android.util.Log.d("ReportAssembler", "After sort: ${componentsWithFields.size} components, $headAfterSort HEAD")
        componentsWithFields.forEachIndexed { idx, comp ->
            if (comp.componentType == "HEAD") {
                android.util.Log.d("ReportAssembler", "  HEAD[$idx]: ${comp.componentName}")
            }
        }

        ReportDTO(
            reportNumber = reportNumber,  // Используем новый формат номера
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