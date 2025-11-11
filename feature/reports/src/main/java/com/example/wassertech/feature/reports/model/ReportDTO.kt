package com.example.wassertech.feature.reports.model

data class CompanyConfig(
    val legal_name: String,
    val inn: String,
    val phone1: String,
    val phone2: String,
    val email: String,
    val website: String,
    val sign_name: String,
    val sign_short: String
)

data class ContractConfig(
    val number: String,
    val date_rus: String
)

data class WaterAnalysisItem(
    val name: String,
    val value: String,
    val unit: String,
    val norm: String
)

data class ReportDTO(
    val reportNumber: String,
    val reportDate: String,
    val reportDateRus: String, // Дата в русском формате

    val companyName: String,
    val engineerName: String?,

    val clientName: String,
    val clientAddress: String?,
    val clientPhone: String?,
    val clientSignName: String?,

    val siteName: String?,
    val installationName: String,
    val installationLocation: String?,

    val components: List<ComponentRowDTO>,
    val observations: List<String>,
    val conclusions: String?,
    val nextMaintenanceDate: String?,

    // Новые поля для v2 шаблона
    val works: List<String> = emptyList(), // Выполненные работы
    val waterAnalyses: List<WaterAnalysisItem> = emptyList(), // Результаты анализов воды
    val comments: String? = null, // Комментарии и замечания
    
    // Поля для v3 шаблона - компоненты с их полями
    val componentsWithFields: List<ComponentWithFieldsDTO> = emptyList(),

    // Конфигурация компании (из JSON)
    val companyConfig: CompanyConfig? = null,
    val contractConfig: ContractConfig? = null,

    // пути к ресурсам (опционально)
    val logoAssetPath: String? = "img/logo-wassertech-bolder.png"
)

data class ComponentRowDTO(
    val name: String,
    val type: String?,
    val serial: String?,
    val status: String,
    val notes: String?
)

data class ComponentFieldDTO(
    val label: String,
    val value: String,
    val unit: String? = null,
    val checkboxClass: String? = null // "checkbox-yes" или "checkbox-no" для чекбоксов
)

data class ComponentWithFieldsDTO(
    val componentName: String,
    val componentType: String?,
    val fields: List<ComponentFieldDTO>
)

