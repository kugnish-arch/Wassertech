package ru.wassertech.core.ui.icons

import ru.wassertech.core.ui.components.IconPackUiData
import ru.wassertech.core.ui.components.IconUiData

/**
 * Состояние для IconPickerDialog.
 * Используется для передачи данных о паках и иконках в диалог выбора иконки.
 */
data class IconPickerUiState(
    val packs: List<IconPackUiData>,
    val iconsByPack: Map<String, List<IconUiData>>
)

