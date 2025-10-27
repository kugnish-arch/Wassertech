package com.example.wassertech.ui.util

import com.example.wassertech.data.types.ComponentType

object TemplateTypeMapper {
    /**
     * Map template meta (category/name) to existing legacy enum ComponentType.
     * Keep the mapping conservative: only SOFTENER, RO and FILTER (default).
     */
    fun map(category: String?, name: String): ComponentType {
        val cat = (category ?: "").lowercase()
        val nm  = name.lowercase()
        fun has(s: String) = cat.contains(s) || nm.contains(s)

        return when {
            // умягчение
            has("soft") || has("softener") || has("умягч") -> ComponentType.SOFTENER
            // обратный осмос
            has("ro") || has("osmos") || has("осмос") || has("обратн") -> ComponentType.RO
            // по умолчанию считаем фильтром
            else -> ComponentType.FILTER
        }
    }
}
