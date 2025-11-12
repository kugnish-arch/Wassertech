package ru.wassertech.ui.util

import ru.wassertech.data.types.ComponentType

object TemplateTypeMapper {
    /**
     * Map template meta (category/name) to ComponentType.
     * Returns HEAD if category contains "head", otherwise COMMON.
     */
    fun map(category: String?, name: String): ComponentType {
        val cat = (category ?: "").lowercase()
        val nm  = name.lowercase()
        fun has(s: String) = cat.contains(s) || nm.contains(s)

        return when {
            // Заглавный шаблон
            has("head") -> ComponentType.HEAD
            // По умолчанию обычный шаблон
            else -> ComponentType.COMMON
        }
    }
}
