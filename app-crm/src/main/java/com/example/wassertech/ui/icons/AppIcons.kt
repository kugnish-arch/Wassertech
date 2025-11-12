package ru.wassertech.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Central icon mapping.
 * We use stable, widely-available Material icons to ensure compile succeeds now.
 * If you prefer newer symbols (Factory, Warehouse, Brick, Lab Panel),
 * we can import them later as vector assets and swap here in one place.
 */
object AppIcons {
    // Клиент
    val ClientPrivate: ImageVector = Icons.Outlined.Person
    // Используем Domain как «корпоративный» (здание/офис). Можно заменить на Factory позже.
    val ClientCorporate: ImageVector = Icons.Outlined.Domain

    // Объект (Site) — дом/склад. Заменим на Warehouse при импорте символа.
    val Site: ImageVector = Icons.Outlined.HomeWork

    // Установка — лабораторный/научный символ
    val Installation: ImageVector = Icons.Outlined.Science

    // Компонент — «категории/блоки». Можно заменить на Brick позже.
    val Component: ImageVector = Icons.Outlined.Category
}
