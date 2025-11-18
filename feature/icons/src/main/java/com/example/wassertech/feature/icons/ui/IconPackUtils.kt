package ru.wassertech.feature.icons.ui

import java.text.SimpleDateFormat
import java.util.*

/**
 * Форматирует epoch timestamp в строку даты.
 */
fun formatEpoch(epoch: Long): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(epoch))
}


