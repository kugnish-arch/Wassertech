package com.example.wassertech.report.model

data class ReportDTO(
    val reportNumber: String,
    val reportDate: String,

    val companyName: String,
    val engineerName: String?,

    val clientName: String,
    val clientAddress: String?,
    val clientPhone: String?,

    val siteName: String?,
    val installationName: String,
    val installationLocation: String?,

    val components: List<ComponentRowDTO>,
    val observations: List<String>,
    val conclusions: String?,
    val nextMaintenanceDate: String?,

    // пути к ресурсам (опционально)
    val logoAssetPath: String? = "img/logo.png" // хранить в assets/img/logo.png
)

data class ComponentRowDTO(
    val name: String,
    val type: String?,
    val serial: String?,
    val status: String,
    val notes: String?
)
