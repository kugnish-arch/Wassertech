package ru.wassertech.feature.reports

import android.content.Context
import ru.wassertech.feature.reports.model.ComponentRowDTO
import ru.wassertech.feature.reports.model.ComponentWithFieldsDTO
import ru.wassertech.feature.reports.model.ComponentFieldDTO
import ru.wassertech.feature.reports.model.ReportDTO
import ru.wassertech.feature.reports.model.WaterAnalysisItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import java.text.SimpleDateFormat
import java.util.*
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log

object ReportAssembler {

    /**
     * Вспомогательная функция для вызова suspend функций через рефлексию.
     * Suspend функции в Kotlin компилируются с дополнительным параметром Continuation.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> callSuspendMethod(
        receiver: Any,
        methodName: String,
        vararg args: Any?
    ): T? = withTimeout(30000) { // Таймаут 30 секунд для каждого вызова
        suspendCancellableCoroutine { cont ->
            try {
                Log.d("ReportAssembler", "Calling suspend method: $methodName with ${args.size} args on ${receiver.javaClass.simpleName}")
                val clazz = receiver.javaClass
                // Ищем все методы с таким именем (включая методы из интерфейсов)
                val allMethods = (clazz.declaredMethods.asList() + clazz.methods.asList()).distinctBy { 
                    "${it.name}${it.parameterTypes.joinToString { it.name }}"
                }
                val methods = allMethods.filter { it.name == methodName }
                
                Log.d("ReportAssembler", "Found ${methods.size} methods with name $methodName")
                methods.forEach { m ->
                    Log.d("ReportAssembler", "  Method: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})")
                }
                
                // Ищем метод с правильным количеством параметров (args + Continuation в конце)
                val method = methods.firstOrNull { method ->
                    val paramTypes = method.parameterTypes
                    val isSuspend = paramTypes.size == args.size + 1 &&
                            Continuation::class.java.isAssignableFrom(paramTypes.last())
                    if (isSuspend) {
                        Log.d("ReportAssembler", "Found suspend method: ${method.name} with params: ${paramTypes.joinToString { it.simpleName }}")
                    }
                    isSuspend
                }
                
                if (method == null) {
                    val availableMethods = methods.map { 
                        it.parameterTypes.joinToString(", ", "${it.name}(") + ")"
                    }.joinToString("\n  ")
                    val errorMsg = "Suspend method $methodName with ${args.size} parameters not found in ${clazz.name}.\n" +
                            "Available methods:\n  $availableMethods"
                    Log.e("ReportAssembler", errorMsg)
                    cont.resumeWithException(NoSuchMethodException(errorMsg))
                    return@suspendCancellableCoroutine
                }
                
                method.isAccessible = true
                
                // Получаем тип Continuation из сигнатуры метода
                val continuationParamType = method.parameterTypes.last()
                Log.d("ReportAssembler", "Continuation type: ${continuationParamType.name}")
                
                // Флаг для отслеживания, был ли вызван continuation
                val wasResumed = AtomicBoolean(false)
                
                // Создаем wrapper continuation, который отслеживает вызовы
                // Используем дженерик T? для правильной типизации
                val wrapperCont = object : Continuation<T?> {
                    override val context = cont.context
                    override fun resumeWith(result: kotlin.Result<T?>) {
                        wasResumed.set(true)
                        Log.d("ReportAssembler", "Continuation.resumeWith called for $methodName")
                        cont.resumeWith(result)
                    }
                }
                
                // Создаем массив аргументов: исходные аргументы + Continuation
                val allArgs = arrayOfNulls<Any?>(args.size + 1)
                args.forEachIndexed { index, arg -> 
                    allArgs[index] = arg
                    Log.d("ReportAssembler", "  Arg[$index]: ${arg?.javaClass?.simpleName ?: "null"} = $arg")
                }
                
                // Используем wrapper continuation вместо оригинального
                // Кастим к Continuation<Any?>, так как Room ожидает этот тип
                @Suppress("UNCHECKED_CAST")
                allArgs[args.size] = wrapperCont as Continuation<Any?>
                
                Log.d("ReportAssembler", "Invoking method $methodName on thread: ${Thread.currentThread().name}")
                
                // Вызываем метод - он сам вызовет continuation.resume() или continuation.resumeWithException()
                // Возвращаемое значение может быть COROUTINE_SUSPENDED или фактическое значение
                val result = method.invoke(receiver, *allArgs)
                Log.d("ReportAssembler", "Method $methodName returned: ${result?.javaClass?.simpleName ?: "null"}, result == COROUTINE_SUSPENDED: ${result === COROUTINE_SUSPENDED}")
                
                // Если метод вернул результат напрямую (не COROUTINE_SUSPENDED) и continuation не был вызван,
                // значит Room не вызвал continuation, и нам нужно вызвать его самим
                if (result !== COROUTINE_SUSPENDED && !wasResumed.get()) {
                    Log.d("ReportAssembler", "Method returned result synchronously but continuation was not called, resuming manually")
                    // Кастим результат к нужному типу
                    @Suppress("UNCHECKED_CAST")
                    val typedResult = result as? T
                    if (cont.isActive) {
                        cont.resume(typedResult)
                    }
                } else if (result === COROUTINE_SUSPENDED) {
                    Log.d("ReportAssembler", "Method suspended, waiting for continuation to be called")
                    // Метод приостановлен, continuation будет вызван асинхронно
                } else {
                    Log.d("ReportAssembler", "Continuation was already called by Room method")
                }
            } catch (e: Exception) {
                Log.e("ReportAssembler", "Error calling suspend method $methodName", e)
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    /**
     * Assembles a report from session data.
     * @param db Database instance (must be AppDatabase from app-crm module)
     * @param context Context for loading configs
     * @param sessionId Session ID to assemble report for
     */
    suspend fun assemble(db: Any, context: Context, sessionId: String): ReportDTO = withContext(Dispatchers.IO) {
        Log.d("ReportAssembler", "Starting assemble for sessionId: $sessionId")
        try {
            // Используем рефлексию для доступа к методам AppDatabase
            // Это необходимо, чтобы избежать циклической зависимости
            Log.d("ReportAssembler", "Getting DAOs from database")
            val sessionsDao = db.javaClass.getMethod("sessionsDao").invoke(db)
            val hierarchyDao = db.javaClass.getMethod("hierarchyDao").invoke(db)
            val clientDao = db.javaClass.getMethod("clientDao").invoke(db)
            val templatesDao = db.javaClass.getMethod("templatesDao").invoke(db)
            Log.d("ReportAssembler", "DAOs retrieved successfully")

            // Получаем сессию (nullable) через правильный вызов suspend функции
            Log.d("ReportAssembler", "Getting session by id")
            val session = callSuspendMethod<Any>(sessionsDao, "getSessionById", sessionId)
            Log.d("ReportAssembler", "Session retrieved: ${session != null}")

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
            callSuspendMethod<Any>(hierarchyDao, "getInstallation", id)
        }
        val installationSiteId = installation?.let { getField(it, "siteId") as? String }
        val installationName = installation?.let { getField(it, "name") as? String ?: "" }
        val installationId = installation?.let { getField(it, "id") as? String }

        val site = installationSiteId?.let { sid ->
            callSuspendMethod<Any>(hierarchyDao, "getSite", sid)
        }
        val siteClientId = site?.let { getField(it, "clientId") as? String }
        val siteName = site?.let { getField(it, "name") as? String }

        // getClient не suspend, вызываем напрямую
        val client = siteClientId?.let { cid ->
            clientDao.javaClass.getMethod("getClient", String::class.java).invoke(clientDao, cid) as? Any
        }
        val clientName = client?.let { getField(it, "name") as? String ?: "Клиент" }
        val clientAddressFull = client?.let { getField(it, "addressFull") as? String }
        val clientPhone = client?.let { getField(it, "phone") as? String }
        val clientContactPerson = client?.let { getField(it, "contactPerson") as? String }

        // Компоненты установки — если нет установки, пустой список
        val components = installationId?.let { instId ->
            callSuspendMethod<List<*>>(hierarchyDao, "getComponentsNow", instId) ?: emptyList<Any>()
        } ?: emptyList<Any>()

        // Старые observations (если есть) — возвращаем список, сортировка handled in DAO
        val observations = callSuspendMethod<List<*>>(sessionsDao, "getObservations", sessionId) ?: emptyList<Any>()

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
        val maintenanceValues = callSuspendMethod<List<*>>(sessionsDao, "getValuesForSession", sessionId) ?: emptyList<Any>()
        val valuesByComponent = maintenanceValues.groupBy { value ->
            (value as? Any)?.let { getField(it, "componentId") as? String } ?: ""
        }
        
        // Создаем множество ID компонентов, которые уже обработаны через valuesByComponent
        val processedComponentIds = mutableSetOf<String>()
        
        val componentsWithFields = mutableListOf<ComponentWithFieldsDTO>()
        for ((componentId, values) in valuesByComponent) {
            if (componentId.isEmpty()) continue
            processedComponentIds.add(componentId)
            val component = callSuspendMethod<Any>(hierarchyDao, "getComponent", componentId)
            val componentName = component?.let { getField(it, "name") as? String } ?: componentId
            val componentTemplateId = component?.let { getField(it, "templateId") as? String }
            
            // Получаем componentType из шаблона (COMMON или HEAD)
            var componentType: String? = "COMMON" // По умолчанию COMMON
            componentTemplateId?.let { templateId ->
                try {
                    val template = callSuspendMethod<Any>(templatesDao, "getTemplateById", templateId)
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
                    val fields = callSuspendMethod<List<*>>(templatesDao, "getMaintenanceFieldsForTemplate", templateId) ?: emptyList<Any>()
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
                    val fields = callSuspendMethod<List<*>>(templatesDao, "getMaintenanceFieldsForTemplate", templateId) ?: emptyList<Any>()
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
                    val fields = callSuspendMethod<List<*>>(templatesDao, "getMaintenanceFieldsForTemplate", templateId) ?: emptyList<Any>()
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
                        val template = callSuspendMethod<Any>(templatesDao, "getTemplateById", templateId)
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
        } catch (e: Exception) {
            Log.e("ReportAssembler", "Error in assemble", e)
            throw e
        }
    }
}
