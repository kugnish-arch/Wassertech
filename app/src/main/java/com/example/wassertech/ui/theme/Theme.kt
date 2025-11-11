package com.example.wassertech.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import android.app.Activity
import android.os.Build

/* ================== БАЗОВЫЕ ЦВЕТА ================== */
// Графитово-серо-белая палитра с красным акцентом
private val BrandRed = Color(0xFFE53935) // Акцентные элементы (FAB, активная вкладка)
private val AppBackground = Color(0xFFFAFAFA) // Фон приложения
private val CardSurface = Color(0xFFFFFFFF) // Карточки / поля
private val TextPrimary = Color(0xFF1E1E1E) // Текст
private val IconSecondary = Color(0xFF5A5A5A) // Иконки второстепенные
private val ButtonSecondaryBorder = Color(0xFFD0D0D0) // Контур вторичных кнопок
private val ButtonSecondaryText = Color(0xFF2C2C2C) // Текст вторичных кнопок

/* ================== СВЕТЛАЯ ЦВЕТОВАЯ СХЕМА ================== */
// Определяет цвета для светлой темы приложения
private val LightColors = lightColorScheme(
    primary = BrandRed, // Акцентные элементы
    onPrimary = Color.White,
    primaryContainer = BrandRed.copy(alpha = 0.1f),
    onPrimaryContainer = BrandRed,
    
    secondary = IconSecondary, // Второстепенные элементы
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8), // Средне-серый фон для плашек заголовков (без фиолетового оттенка)
    onSecondaryContainer = Color(0xFF1E1E1E), // Текст на плашке заголовка
    
    tertiary = IconSecondary.copy(alpha = 0.6f),
    onTertiary = Color.White,
    tertiaryContainer = IconSecondary.copy(alpha = 0.1f),
    onTertiaryContainer = IconSecondary,
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = AppBackground, // Фон приложения
    onBackground = TextPrimary, // Текст на фоне
    
    surface = CardSurface, // Карточки / поля
    onSurface = TextPrimary, // Текст на карточках
    surfaceVariant = CardSurface,
    onSurfaceVariant = IconSecondary, // Второстепенные иконки и текст
    
    outline = ButtonSecondaryBorder, // Контур вторичных кнопок
    outlineVariant = ButtonSecondaryBorder.copy(alpha = 0.5f),
    
    scrim = Color.Black,
    inverseSurface = TextPrimary,
    inverseOnSurface = CardSurface,
    inversePrimary = BrandRed.copy(alpha = 0.8f),
    surfaceTint = Color.Transparent
)

/* ================== ТЕМНАЯ ЦВЕТОВАЯ СХЕМА ================== */
// Определяет цвета для темной темы приложения (используется при системной темной теме)
private val DarkColors = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRed.copy(alpha = 0.2f),
    onPrimaryContainer = BrandRed.copy(alpha = 0.9f),
    
    secondary = IconSecondary,
    onSecondary = Color.White,
    secondaryContainer = IconSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = IconSecondary,
    
    tertiary = IconSecondary.copy(alpha = 0.7f),
    onTertiary = Color.White,
    tertiaryContainer = IconSecondary.copy(alpha = 0.2f),
    onTertiaryContainer = IconSecondary,
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFFAFAFA),
    
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    outline = Color(0xFF808080),
    outlineVariant = Color(0xFF606060),
    
    scrim = Color.Black,
    inverseSurface = Color(0xFFFAFAFA),
    inverseOnSurface = Color(0xFF1E1E1E),
    inversePrimary = BrandRed,
    surfaceTint = Color.Transparent
)

/* ================== ТИПОГРАФИКА ================== */
// Шрифты: Montserrat SemiBold для заголовков, Roboto Regular для текста
// На Android используем системные шрифты, максимально близкие к указанным
private val MontserratSemiBoldFamily = FontFamily.SansSerif // Будет использован SemiBold вес
private val RobotoRegularFamily = FontFamily.SansSerif // Будет использован Regular вес

// Определяет стили текста для всего приложения
// Заголовки (display*, headline*, titleLarge) используют Montserrat SemiBold
// Основной текст (titleMedium*, body*, label*) использует Roboto Regular
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratSemiBoldFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, // Размер заголовков экранов - не больше 22sp
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoRegularFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/* ================== РАЗМЕРЫ ================== */
// Общие размеры и отступы приложения
object Dimens {
    val SubNavbarGap = 4.dp // Отступ между элементами в нижней навигации
}

/* ================== ЦВЕТА ДЛЯ КОМПОНЕНТОВ ================== */
// Цвета для карточек - почти белые, больше elevation
val CardBackground = Color(0xFFFFFFFF) // Почти белый фон для карточек
val CardElevation = 4.dp // Увеличенная тень для карточек

// Цвет для разворачивающихся меню - светло-серый
val ExpandableMenuBackground = Color(0xFFF5F5F5) // Светло-серый фон для разворачивающихся меню

// Цвет для карточек в разворачивающихся списках - белый
val ExpandableMenuCardBackground = Color(0xFFFFFFFF) // Белый фон для карточек в разворачивающихся списках

// Цвет для выпадающих меню (DropdownMenu, ExposedDropdownMenu) - практически белый без оттенка
val DropdownMenuBackground = Color(0xFFFFFFFF) // Практически белый фон для выпадающих меню

/* ================== СТИЛЬ ПОДЗАГОЛОВКОВ ================== */
// Стиль для подзаголовков (серые карточки с именами клиентов, установок, объектов)
// Используется на экранах: Клиент, Объект, Установка, Редактирование ТО, Детали обслуживания
object HeaderCardStyle {
    val backgroundColor = Color(0xFFE8E8E8) // Средне-серый фон
    val textColor = Color(0xFF1E1E1E) // Текст на плашке заголовка
    val titleTextStyle = AppTypography.titleMedium // Шрифт для заголовка
    val subtitleTextStyle = AppTypography.bodyMedium // Шрифт для подзаголовка (если нужен)
    val shape = RoundedCornerShape(
        topStart = 0.dp, // Без скругления верхних углов
        topEnd = 0.dp,
        bottomStart = 12.dp, // Скругление нижних углов
        bottomEnd = 12.dp
    )
    val padding = androidx.compose.foundation.layout.PaddingValues(
        top = 12.dp,
        start = 12.dp,
        end = 12.dp,
        bottom = 12.dp
    )
}

/* ================== ИКОНКИ НАВИГАЦИИ ================== */
// Стандартные иконки для навигации и разворачивания меню
// Используются во всех разворачивающихся меню и навигационных карточках
object NavigationIcons {
    val ExpandMenuIcon: ImageVector = Icons.Filled.ExpandMore // Иконка для разворачивания меню
    val CollapseMenuIcon: ImageVector = Icons.Filled.ExpandLess // Иконка для сворачивания меню
    val NavigateIcon: ImageVector = Icons.Filled.ChevronRight // Иконка для навигации в карточках
}

/* ================== ЦВЕТА ДЛЯ ИКОНОК ================== */
// Цвет для иконки сохранения (зеленый)
val SaveIconColor = Color(0xFF4CAF50) // Зеленый цвет для иконки сохранения

// Цвет для иконки HTML файлов (зеленый)
val HtmlIconColor = Color(0xFF4CAF50) // Зеленый цвет для иконки HTML файлов (используется для временной зеленой PDF иконки)

// Цвет для иконки PDF файлов (красный)
val PdfIconColor = Color(0xFFD32F2F) // Красный цвет для иконки PDF файлов

// Цвет для иконки удаления (корзина) - используется везде, где можно что-то удалить
val DeleteIcon = Icons.Filled.Delete // Иконка корзины для удаления элементов

// Scrim (подложка) для FAB группы - легкая тень/фон для визуального отделения выпрыгивающих кнопок от списка
val FABScrimColor = Color.Black.copy(alpha = 0.08f) // Полупрозрачная черная подложка для FAB группы
val FABScrimElevation = 8.dp // Тень для scrim FAB группы

/* ================== ЦВЕТА ДЛЯ ЭКРАНА КЛИЕНТЫ ================== */
// Цвета для групп клиентов на экране ClientsScreen
val ClientsGroupCollapsedBackground = Color(0xFFF3F4F6) // Светлый фон для свёрнутых групп
val ClientsGroupExpandedBackground = Color(0xFF2E2E2E) // Графитовый фон для активной (открытой) группы
val ClientsGroupExpandedText = Color(0xFFFFFFFF) // Белый текст для активной группы
val ClientsGroupBorder = Color(0xFFDADADA) // Тонкий бордер снизу для групп
val ClientsRowDivider = Color(0xFFDADADA) // Разделительная линия между клиентами в списке

/* ================== СТИЛЬ СЕГМЕНТИРОВАННЫХ КНОПОК ================== */
// Стиль для сегментированных кнопок (SegmentedButton) - скругленные углы
object SegmentedButtonStyle {
    // Размер скругления углов для сегментированных кнопок
    val cornerRadius = 8.dp
    
    /**
     * Возвращает форму для сегментированной кнопки в зависимости от её позиции
     * @param index Индекс кнопки (0 - первая, последняя - count - 1)
     * @param count Общее количество кнопок в ряду
     * @return RoundedCornerShape с соответствующими скруглениями
     */
    fun getShape(index: Int, count: Int): RoundedCornerShape {
        return when {
            count == 1 -> RoundedCornerShape(cornerRadius) // Одна кнопка - все углы скруглены
            index == 0 -> RoundedCornerShape( // Первая кнопка - скругление слева
                topStart = cornerRadius,
                bottomStart = cornerRadius,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            )
            index == count - 1 -> RoundedCornerShape( // Последняя кнопка - скругление справа
                topStart = 0.dp,
                bottomStart = 0.dp,
                topEnd = cornerRadius,
                bottomEnd = cornerRadius
            )
            else -> RoundedCornerShape(0.dp) // Средние кнопки - без скругления
        }
    }
}

/* ================== СТИЛЬ ДИАЛОГОВ ================== */
// Стили для диалогов добавления/редактирования элементов
// Используются во всех диалогах добавления (Клиент, Группа, Объект, Установка, Компонент и т.д.)
object DialogStyle {
    val shape = RoundedCornerShape(12.dp) // Радиус скругления 12dp - более инженерный стиль
    val elevation = 6.dp // Elevation 6 - легкая тень для визуального отделения от фона
    val padding = androidx.compose.foundation.layout.PaddingValues(24.dp) // Отступы внутри диалога
    val contentSpacing = 16.dp // Расстояние между элементами содержимого
}

/* ================== НАСТРОЙКИ СИСТЕМНЫХ БАРОВ ================== */
// Конфигурация для системных кнопок навигации Android
// Всегда темные иконки системных кнопок (видно на любом фоне)
object SystemBarsStyle {
    /**
     * Определяет, должны ли системные кнопки навигации быть темными (light navigation bars)
     * @param isDarkTheme true для темной темы, false для светлой (параметр игнорируется, всегда темные)
     * @return false = темные иконки (всегда)
     */
    fun isLightNavigationBars(isDarkTheme: Boolean): Boolean {
        return false // Всегда темные иконки системных кнопок
    }
    
    /**
     * Определяет, должен ли статус-бар быть светлым (light status bar)
     * @param isDarkTheme true для темной темы, false для светлой
     * @return false = темные иконки статус-бара (для светлой темы), true = светлые иконки (для темной темы)
     */
    fun isLightStatusBar(isDarkTheme: Boolean): Boolean {
        return isDarkTheme // Для темной темы - светлые иконки, для светлой - темные
    }
}

@Composable
fun WassertechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    // Настройка системных баров (статус-бар и навигационные кнопки)
    // Всегда темные иконки на белом фоне для видимости
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                
                // Всегда темные иконки для системных кнопок навигации (false = темные иконки)
                windowInsetsController.isAppearanceLightNavigationBars = false
                
                // Всегда темные иконки для статус-бара (false = темные иконки)
                windowInsetsController.isAppearanceLightStatusBars = false
                
                // Установка белого цвета для системных баров (чтобы темные иконки были видны)
                val whiteColor = Color(0xFFFFFFFF).toArgb()
                
                // Установка белого цвета статус-бара
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = whiteColor
                }
                
                // Установка белого цвета навигационной панели
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.navigationBarColor = whiteColor
                }
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}


