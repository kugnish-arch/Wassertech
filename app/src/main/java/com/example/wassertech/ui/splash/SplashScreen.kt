package com.example.wassertech.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.wassertech.R

// Цвета бренда для анимации логотипа
private val BrandRed = Color(0xFFE53935)
private val Graphite = Color(0xFF1E1E1E)

/**
 * Компонуемый экран заставки с анимацией логотипа Wassertech.
 * 
 * Анимация состоит из следующих этапов:
 * 1. Появление логотипа (alpha и scale)
 * 2. Падение красной вертикальной линии сверху вниз
 * 3. "Вайп" графитового фона справа от линии
 * 4. Проявление белого текста "TECH" в зоне графитового фона
 * 
 * @param onFinished Callback, вызываемый после завершения анимации
 * @param totalMs Общая длительность анимации в миллисекундах (по умолчанию 1500ms)
 */
@Composable
fun SplashRouteFixed(onFinished: () -> Unit, totalMs: Int = 1500) {
    // Анимационные состояния
    val alpha = remember { Animatable(0f) }      // Прозрачность: 0 (невидим) -> 1 (видим)
    val scale = remember { Animatable(0.96f) }   // Масштаб: 0.96 -> 1.0 (легкое увеличение)
    val lineProgress = remember { Animatable(0f) } // Прогресс линии: 0 (вверху) -> 1 (внизу)
    val wipeProgress = remember { Animatable(0f) } // Прогресс "вайпа": 0 (линия) -> 1 (справа)

    LaunchedEffect(Unit) {
        // Этап 1: Появление логотипа (300ms)
        alpha.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))

        // Небольшая пауза перед следующим этапом (120ms)
        delay(120)
        
        // Этап 2: Падение красной линии сверху вниз (380ms)
        lineProgress.animateTo(1f, tween(380, easing = FastOutLinearInEasing))

        // Этап 3: "Вайп" графитового фона и появление белого текста (520ms)
        wipeProgress.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))

        // Ожидание до завершения общей длительности анимации
        val elapsedTime = 300 + 120 + 380 + 520
        delay((totalMs - elapsedTime).coerceAtLeast(0).toLong())
        onFinished()
    }

    SplashLogoFixed(
        alpha = alpha.value,
        scale = scale.value,
        lineProgress = lineProgress.value,
        wipeProgress = wipeProgress.value
    )
}

/**
 * Компонуемый логотип с анимацией для экрана заставки.
 * 
 * Структура логотипа (слои снизу вверх):
 * 1. WASSER (красный текст) - статичный слой слева
 * 2. TECH (чёрный текст) - базовый слой справа, всегда видим
 * 3. Красная вертикальная линия - падает сверху вниз, разделяет "WASSER" и "TECH"
 * 4. Графитовый фон - "вайпится" справа от красной линии
 * 5. TECH (белый текст) - проявляется только в зоне графитового фона
 * 
 * Все слои используют matchParentSize для единой системы координат.
 * 
 * @param alpha Прозрачность логотипа (0f = невидим, 1f = полностью видим)
 * @param scale Масштаб логотипа (1f = нормальный размер)
 * @param lineProgress Прогресс падения красной линии (0f = вверху, 1f = внизу)
 * @param wipeProgress Прогресс "вайпа" графитового фона (0f = только линия, 1f = полностью справа)
 */
@Composable
private fun SplashLogoFixed(
    alpha: Float,
    scale: Float,
    lineProgress: Float,
    wipeProgress: Float
) {
    // Максимальная ширина логотипа для адаптивности на разных экранах
    val maxLogoWidth = 280.dp
    
    // Размеры логотипа в пикселях (обновляются после измерения)
    var logoW by remember { mutableStateOf(0f) }
    var logoH by remember { mutableStateOf(0f) }

    // Внешний контейнер - центрирует логотип на экране
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внутренний контейнер - ограничивает ширину и применяет анимации
        Box(
            modifier = Modifier
                .widthIn(max = maxLogoWidth)  // Ограничение ширины для стабильности в портретной ориентации
                .wrapContentHeight()
                .scale(scale)                 // Применяем масштабирование
                .alpha(alpha)                 // Применяем прозрачность
                .onGloballyPositioned {
                    // Сохраняем размеры логотипа для расчетов координат в Canvas
                    logoW = it.size.width.toFloat()
                    logoH = it.size.height.toFloat()
                }
        ) {
            // ВАЖНО: Все слои используют matchParentSize() для единой системы координат
            // Это позволяет точно позиционировать элементы относительно друг друга

            // Слой 1: WASSER (красный текст) - статичный, всегда видим слева
            Image(
                painter = painterResource(R.drawable.logo_wasser_red),
                contentDescription = "wasser (red)",
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize()
            )

            // Слой 2: TECH (чёрный текст) - базовый слой справа, всегда видим
            // Этот слой виден везде, где нет графитового фона
            Image(
                painter = painterResource(R.drawable.logo_tech_black),
                contentDescription = "tech (black)",
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize()
            )

            // Слой 3: Красная вертикальная линия - падает сверху вниз
            // Позиция линии: 63% ширины логотипа (эмпирически подобранное значение)
            Canvas(Modifier.matchParentSize()) {
                if (logoW > 0f && logoH > 0f) {
                    val xLine = logoW * 0.63f  // X-координата линии (разделяет "WASSER" и "TECH")
                    val yEnd = logoH * lineProgress  // Y-координата конца линии (прогресс падения)
                    
                    drawLine(
                        color = BrandRed,
                        start = Offset(xLine, 0f),      // Начало линии вверху
                        end = Offset(xLine, yEnd),      // Конец линии (меняется в зависимости от прогресса)
                        strokeWidth = 6f,               // Толщина линии
                        cap = StrokeCap.Round           // Закругленные концы
                    )
                }
            }

            // Слой 4: Графитовый фон - "вайпится" справа от красной линии
            // Начинается от линии и расширяется вправо в зависимости от wipeProgress
            Canvas(Modifier.matchParentSize()) {
                if (logoW > 0f && logoH > 0f) {
                    val xLine = logoW * 0.63f  // Позиция красной линии
                    // Правая граница графитового фона (расширяется от линии вправо)
                    val rightNow = xLine + (logoW - xLine) * wipeProgress
                    
                    drawRect(
                        color = Graphite,
                        topLeft = Offset(xLine, 0f),  // Начинается от красной линии
                        size = androidx.compose.ui.geometry.Size(
                            (rightNow - xLine).coerceAtLeast(0f),  // Ширина фона (расширяется)
                            logoH  // Высота фона (на всю высоту логотипа)
                        )
                    )
                }
            }

            // Слой 5: TECH (белый текст) - проявляется ТОЛЬКО в зоне графитового фона
            // Используется clipRect для обрезки белого текста только в области графитового фона
            val whiteTech = painterResource(R.drawable.logo_tech_white)
            Canvas(Modifier.matchParentSize()) {
                if (logoW > 0f && logoH > 0f) {
                    val xLine = logoW * 0.63f
                    val rightNow = xLine + (logoW - xLine) * wipeProgress
                    
                    // Обрезаем область рисования до зоны графитового фона
                    clipRect(
                        left = xLine,      // Начинается от красной линии
                        top = 0f,          // Вверху
                        right = rightNow,  // Заканчивается на правой границе графитового фона
                        bottom = logoH     // Внизу
                    ) {
                        // Рисуем белый текст TECH только в обрезанной области
                        with(whiteTech) { draw(size = size) }
                    }
                }
            }
        }
    }
}
