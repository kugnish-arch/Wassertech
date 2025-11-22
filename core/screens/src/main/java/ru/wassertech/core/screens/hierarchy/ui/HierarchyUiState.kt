package ru.wassertech.core.screens.hierarchy.ui

/**
 * UI State для элемента объекта (Site) в shared-экранах.
 */
data class SiteItemUi(
    val id: String,
    val name: String,
    val address: String?,
    val iconId: String?,
    val iconAndroidResName: String? = null, // Для отображения через IconResolver
    val iconCode: String? = null, // Для fallback поиска ресурса
    val iconLocalImagePath: String? = null, // Локальный путь к изображению
    val isArchived: Boolean,
    val clientId: String,
    val origin: String?,
    val createdByUserId: String?,
    // Права доступа
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canChangeIcon: Boolean,
    val canReorder: Boolean = true // Для drag-and-drop
)

/**
 * UI State для элемента установки (Installation) в shared-экранах.
 */
data class InstallationItemUi(
    val id: String,
    val name: String,
    val iconId: String?,
    val iconAndroidResName: String? = null, // Для отображения через IconResolver
    val iconCode: String? = null, // Для fallback поиска ресурса
    val iconLocalImagePath: String? = null, // Локальный путь к изображению
    val isArchived: Boolean,
    val siteId: String,
    val origin: String?,
    val createdByUserId: String?,
    // Права доступа
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canChangeIcon: Boolean,
    val canReorder: Boolean = true, // Для drag-and-drop
    val canStartMaintenance: Boolean = false, // Для кнопки "Провести ТО"
    val canViewMaintenanceHistory: Boolean = false // Для кнопки "История ТО"
)

/**
 * UI State для элемента компонента (Component) в shared-экранах.
 */
data class ComponentItemUi(
    val id: String,
    val name: String,
    val type: String,
    val templateName: String? = null, // Название шаблона компонента
    val iconId: String?,
    val iconAndroidResName: String? = null, // Для отображения через IconResolver
    val iconCode: String? = null, // Для fallback поиска ресурса
    val iconLocalImagePath: String? = null, // Локальный путь к изображению
    val isArchived: Boolean,
    val installationId: String,
    val origin: String?,
    val createdByUserId: String?,
    val temperatureValue: Double? = null, // Последнее значение температуры для SENSOR компонентов
    // Права доступа
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canChangeIcon: Boolean,
    val canReorder: Boolean = true // Для drag-and-drop
)

/**
 * UI State для экрана списка объектов клиента.
 */
data class ClientSitesUiState(
    val clientId: String,
    val clientName: String,
    val isCorporate: Boolean,
    val sites: List<SiteItemUi>,
    val includeArchived: Boolean = false, // Показывать ли архивные объекты
    val canAddSite: Boolean,
    val canEditClient: Boolean = false, // Можно ли редактировать клиента
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI State для экрана списка установок объекта.
 */
data class SiteInstallationsUiState(
    val siteId: String,
    val siteName: String,
    val clientName: String? = null, // Имя клиента для отображения
    val installations: List<InstallationItemUi>,
    val includeArchived: Boolean = false, // Показывать ли архивные установки
    val canAddInstallation: Boolean,
    val canEditSite: Boolean = false, // Можно ли редактировать объект
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI State для экрана списка компонентов установки.
 */
data class InstallationComponentsUiState(
    val installationId: String,
    val installationName: String,
    val siteName: String? = null, // Имя объекта для отображения
    val clientName: String? = null, // Имя клиента для отображения
    val components: List<ComponentItemUi>,
    val includeArchived: Boolean = false, // Показывать ли архивные компоненты
    val canAddComponent: Boolean,
    val canEditInstallation: Boolean = false, // Можно ли редактировать установку
    val isLoading: Boolean = false,
    val error: String? = null
)

