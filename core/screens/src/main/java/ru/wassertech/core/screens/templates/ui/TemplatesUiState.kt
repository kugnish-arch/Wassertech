package ru.wassertech.core.screens.templates.ui

/**
 * UI State для элемента шаблона в списке.
 */
data class TemplateItemUi(
    val id: String,
    val name: String,
    val category: String?,
    val isArchived: Boolean,
    val sortOrder: Int
)

/**
 * UI State для экрана списка шаблонов.
 */
data class TemplatesUiState(
    val templates: List<TemplateItemUi>,
    val localOrder: List<String>, // Локальный порядок для drag-and-drop
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI State для поля шаблона в редакторе.
 * Использует строковое представление типа поля, так как core:screens не имеет доступа к data модулю.
 * Приложения должны преобразовывать строку в FieldType enum при использовании.
 */
data class TemplateFieldUi(
    val id: String,
    val templateId: String,
    val key: String,
    val label: String,
    val type: String, // "TEXT", "CHECKBOX", "NUMBER" - строковое представление FieldType
    val isCharacteristic: Boolean, // true = характеристика, false = чек-лист ТО
    val unit: String?,
    val min: String?,   // UI-friendly (TextField)
    val max: String?,   // UI-friendly (TextField)
    val errors: List<String> = emptyList()
)

/**
 * UI State для экрана редактора шаблона.
 */
data class TemplateEditorUiState(
    val templateId: String,
    val templateName: String,
    val isHeadComponent: Boolean,
    val fields: List<TemplateFieldUi>,
    val localFieldOrder: List<String>, // Локальный порядок полей для drag-and-drop
    val isLoading: Boolean = false,
    val error: String? = null
)

