package com.example.wassertech.feature.reports

import android.content.Context
import androidx.room.RoomDatabase
import com.example.wassertech.feature.reports.model.ComponentRowDTO
import com.example.wassertech.feature.reports.model.ComponentWithFieldsDTO
import com.example.wassertech.feature.reports.model.ComponentFieldDTO
import com.example.wassertech.feature.reports.model.ReportDTO
import com.example.wassertech.feature.reports.model.WaterAnalysisItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object ReportAssembler {

    /**
     * Assembles a report from session data.
     * @param db Database instance (must be AppDatabase from app-crm module)
     * @param context Context for loading configs
     * @param sessionId Session ID to assemble report for
     */
    suspend fun assemble(db: RoomDatabase, context: Context, sessionId: String): ReportDTO = withContext(Dispatchers.IO) {
        // Используем рефлексию для доступа к методам AppDatabase
        // Это необходимо, чтобы избежать циклической зависимости
        val sessionsDao = db.javaClass.getMethod("sessionsDao").invoke(db)
        val hierarchyDao = db.javaClass.getMethod("hierarchyDao").invoke(db)
        val clientDao = db.javaClass.getMethod("clientDao").invoke(db)
        val templatesDao = db.javaClass.getMethod("templatesDao").invoke(db)

        // Получаем сессию (nullable)
        val session = sessionsDao.javaClass.getMethod("getSessionById", String::class.java).invoke(sessionsDao, sessionId) as? Any

        // Без сессии - бросаем понятную ошибку
        requireNotNull(session) { "Session $sessionId not found" }

        // Вспомогательная функция для получения поля объекта через рефлексию
        fun getField(obj: Any, fieldName: String): Any? {
            return obj.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.get(obj)
        }

        // Получаем поля сессии
        val sessionInstallationId = getField(session, "installationId") as? String
        val sessionSiteId = getField(session, "siteId") as? String
        val sessionStartedAtEpoch = getField(session, "startedAtEpoch") as? Long?
        val sessionTechnician = getField(session, "technician") as? String
        val sessionNotes = getField(session, "notes") as? String

        // Installation может быть null (installationId в сессии nullable)
        val installation = sessionInstallationId?.let { id ->
            hierarchyDao.javaClass.getMethod("getInstallation", String::class.java).invoke(hierarchyDao, id) as? Any
        }
        val installationSiteId = installation?.let { getField(it, "siteId") as? String }
        val installationName = installation?.let { getField(it, "name") as? String ?: "" }
        val installationId = installation?.let { getField(it, "id") as? String }

        val site = installationSiteId?.let { sid ->
            hierarchyDao.javaClass.getMethod("getSite", String::class.java).invoke(hierarchyDao, sid) as? Any
        }
        val siteClientId = site?.let { getField(it, "clientId") as? String }
        val siteName = site?.let { getField(it, "name") as? String }

        val client = siteClientId?.let { cid ->
            clientDao.javaClass.getMethod("getClient", String::class.java).invoke(clientDao, cid) as? Any
        }
        val clientName = client?.let { getField(it, "name") as? String ?: "Клиент" }
        val clientAddressFull = client?.let { getField(it, "addressFull") as? String }
        val clientPhone = client?.let { getField(it, "phone") as? String }
        val clientContactPerson = client?.let { getField(it, "contactPerson") as? String }

        // Компоненты установки — если нет установки, пустой список
        val components = installationId?.let { instId ->
            hierarchyDao.javaClass.getMethod("getComponentsNow", String::class.java).invoke(hierarchyDao, instId) as? List<*> ?: emptyList<Any>()
        } ?: emptyList<Any>()

        // Старые observations (если есть) — возвращаем список, сортировка handled in DAO
        val observations = sessionsDao.javaClass.getMethod("getObservations", String::class.java).invoke(sessionsDao, sessionId) as? List<*> ?: emptyList<Any>()

        // Сформируем строки компонентов — используем лишь поля, которые реально есть в сущности
        val rows = components.mapNotNull { cmp ->
            val cmpName = (cmp as? Any)?.let { getField(it, "name") as? String } ?: return@mapNotNull null
            val cmpType = (cmp as? Any)?.let { 
                val typeObj = getField(it, "type")
                // Пытаемся получить name из enum
                typeObj?.javaClass?.getMethod("name")?.invoke(typeObj) as? String ?: "UNKNOWN"
            } ?: "UNKNOWN"
            
            ComponentRowDTO(
                name = cmpName,
                type = cmpType,
                serial = "",
                status = "",
                notes = ""
            )
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFmtRus = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
        val reportDate = sessionStartedAtEpoch?.let { dateFmt.format(Date(it)) } ?: "Не указана"
        val reportDateRus = sessionStartedAtEpoch?.let { dateFmtRus.format(Date(it)) } ?: ""
        val nextDate = null // нет поля nextMaintenance в сущности MaintenanceSessionEntity
        
        // Генерируем номер отчета в формате АXXXXX/mmyy
        val reportNumber = ReportNumberGenerator.generateReportNumber(context)

        // Составляем строки наблюдений: выбираем текстовое представление значения
        val observationTexts = observations.mapNotNull { o ->
            val valueText = (o as? Any)?.let { getField(it, "valueText") as? String }
            val valueNumber = (o as? Any)?.let { getField(it, "valueNumber") as? Number }
            val valueBool = (o as? Any)?.let { getField(it, "valueBool") as? Boolean }
            
            val txt = when {
                valueText != null -> valueText
                valueNumber != null -> valueNumber.toString()
                valueBool != null -> if (valueBool) "Да" else "Нет"
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
        val maintenanceValues = sessionsDao.javaClass.getMethod("getValuesForSession", String::class.java).invoke(sessionsDao, sessionId) as? List<*> ?: emptyList<Any>()
        val valuesByComponent = maintenanceValues.groupBy { value ->
            (value as? Any)?.let { getField(it, "componentId") as? String } ?: ""
        }
        
        // Создаем множество ID компонентов, которые уже обработаны через valuesByComponent
        val processedComponentIds = mutableSetOf<String>()
        
        val componentsWithFields = mutableListOf<ComponentWithFieldsDTO>()
        for ((componentId, values) in valuesByComponent) {
            if (componentId.isEmpty()) continue
            processedComponentIds.add(componentId)
            val component = hierarchyDao.javaClass.getMethod("getComponent", String::class.java).invoke(hierarchyDao, componentId) as? Any
            val componentName = component?.let { getField(it, "name") as? String } ?: componentId
            val componentTemplateId = component?.let { getField(it, "templateId") as? String }
            
            // Получаем componentType из шаблона (COMMON или HEAD)
            var componentType: String? = "COMMON" // По умолчанию COMMON
            componentTemplateId?.let { templateId ->
                try {
                    val template = templatesDao.javaClass.getMethod("getTemplateById", String::class.java).invoke(templatesDao, templateId) as? Any
                    val templateComponentType = template?.let { 
                        val typeObj = getField(it, "componentType")
                        typeObj?.javaClass?.getMethod("name")?.invoke(typeObj) as? String
                    }
                    componentType = templateComponentType ?: "COMMON"
                } catch (e: Exception) {
                    // Если шаблон не найден, используем COMMON по умолчанию
                    componentType = "COMMON"
                }
            }
            
            // Получаем метки полей из шаблона
            val fieldLabels: Map<String, String> = componentTemplateId?.let { templateId ->
                try {
                    val fields = templatesDao.javaClass.getMethod("getMaintenanceFieldsForTemplate", String::class.java).invoke(templatesDao, templateId) as? List<*> ?: emptyList<Any>()
                    fields.mapNotNull { field ->
                        val key = (field as? Any)?.let { getField(it, "key") as? String } ?: return@mapNotNull null
                        val label = (field as? Any)?.let { getField(it, "label") as? String }
                        key to (label ?: key)
                    }.toMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            // Получаем единицы измерения из шаблона
            val fieldUnits: Map<String, String?> = componentTemplateId?.let { templateId ->
                try {
                    val fields = templatesDao.javaClass.getMethod("getMaintenanceFieldsForTemplate", String::class.java).invoke(templatesDao, templateId) as? List<*> ?: emptyList<Any>()
                    fields.mapNotNull { field ->
                        val key = (field as? Any)?.let { getField(it, "key") as? String } ?: return@mapNotNull null
                        val unit = (field as? Any)?.let { getField(it, "unit") as? String }
                        key to unit
                    }.toMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            // Получаем типы полей из шаблона для определения чекбоксов
            val fieldTypes: Map<String, String> = componentTemplateId?.let { templateId ->
                try {
                    val fields = templatesDao.javaClass.getMethod("getMaintenanceFieldsForTemplate", String::class.java).invoke(templatesDao, templateId) as? List<*> ?: emptyList<Any>()
                    fields.mapNotNull { field ->
                        val key = (field as? Any)?.let { getField(it, "key") as? String } ?: return@mapNotNull null
                        val typeObj = (field as? Any)?.let { getField(it, "type") }
                        val typeName = typeObj?.javaClass?.getMethod("name")?.invoke(typeObj) as? String
                        key to (typeName ?: "TEXT")
                    }.toMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
            
            val fields = (values as? List<*>)?.mapNotNull { value ->
                val fieldKey = (value as? Any)?.let { getField(it, "fieldKey") as? String } ?: return@mapNotNull null
                val label = fieldLabels[fieldKey] ?: fieldKey.substringBefore('_', fieldKey)
                val fieldTypeName = fieldTypes[fieldKey] ?: "TEXT"
                val valueText = (value as? Any)?.let { 
                    val vText = getField(it, "valueText") as? String
                    val vBool = getField(it, "valueBool") as? Boolean
                    when {
                        vText != null -> vText
                        vBool != null -> if (vBool) "Да" else "Нет"
                        else -> null
                    }
                }
                
                // Определяем класс для чекбоксов
                val checkboxClass = if (fieldTypeName == "CHECKBOX") {
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
                        unit = fieldUnits[fieldKey],
                        checkboxClass = checkboxClass
                    )
                }
            }?.sortedBy { it.label.lowercase(Locale.getDefault()) } ?: emptyList()
            
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
            val componentId = (component as? Any)?.let { getField(it, "id") as? String } ?: continue
            if (componentId !in processedComponentIds) {
                val componentTemplateId = (component as? Any)?.let { getField(it, "templateId") as? String }
                // Получаем componentType из шаблона
                var componentType: String? = "COMMON"
                componentTemplateId?.let { templateId ->
                    try {
                        val template = templatesDao.javaClass.getMethod("getTemplateById", String::class.java).invoke(templatesDao, templateId) as? Any
                        val templateComponentType = template?.let { 
                            val typeObj = getField(it, "componentType")
                            typeObj?.javaClass?.getMethod("name")?.invoke(typeObj) as? String
                        }
                        componentType = templateComponentType ?: "COMMON"
                    } catch (e: Exception) {
                        componentType = "COMMON"
                    }
                }
                
                // Добавляем HEAD компоненты даже без полей
                if (componentType == "HEAD") {
                    val componentName = (component as? Any)?.let { getField(it, "name") as? String } ?: componentId
                    val headComponent = ComponentWithFieldsDTO(
                        componentName = componentName,
                        componentType = componentType,
                        fields = emptyList()  // Пустой список полей для HEAD компонентов без значений
                    )
                    componentsWithFields.add(headComponent)
                    android.util.Log.d("ReportAssembler", "Added HEAD component without fields: $componentName")
                }
            }
        }
        
        // Логируем перед сортировкой
        val headBeforeSort = componentsWithFields.count { it.componentType == "HEAD" }
        android.util.Log.d("ReportAssembler", "Before sort: ${componentsWithFields.size} components, $headBeforeSort HEAD")
        
        // Создаем мапу componentId -> orderIndex для сортировки
        val componentOrderMap = components.mapNotNull { cmp ->
            val name = (cmp as? Any)?.let { getField(it, "name") as? String } ?: return@mapNotNull null
            val orderIndex = (cmp as? Any)?.let { getField(it, "orderIndex") as? Int } ?: Int.MAX_VALUE
            name to orderIndex
        }.toMap()
        
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
            engineerName = sessionTechnician ?: "Инженер",

            clientName = clientName ?: "Клиент",
            clientAddress = clientAddressFull ?: "",
            clientPhone = clientPhone ?: "",
            clientSignName = clientContactPerson,

            siteName = siteName ?: "",
            installationName = installationName ?: "",
            installationLocation = "",

            components = rows,
            observations = observationTexts,
            conclusions = sessionNotes ?: "",
            nextMaintenanceDate = nextDate,

            works = works,
            waterAnalyses = waterAnalyses,
            comments = sessionNotes ?: "",

            companyConfig = companyConfig,
            contractConfig = contractConfig,

            logoAssetPath = "img/logo-wassertech-bolder.png",
            
            componentsWithFields = componentsWithFields
        )
    }
}
