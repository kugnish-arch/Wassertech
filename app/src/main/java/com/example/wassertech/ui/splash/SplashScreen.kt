package com.example.wassertech.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
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
        // Замедляем анимацию в 1.5 раза
        // Этап 1: Появление логотипа (450ms = 300 * 1.5)
        alpha.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, tween(450, easing = FastOutSlowInEasing))

        // Небольшая пауза перед следующим этапом (180ms = 120 * 1.5)
        delay(180)
        
        // Этап 2: Падение красной линии сверху вниз (570ms = 380 * 1.5)
        lineProgress.animateTo(1f, tween(570, easing = FastOutLinearInEasing))

        // Этап 3: "Вайп" графитового фона и появление белого текста (780ms = 520 * 1.5)
        wipeProgress.animateTo(1f, tween(780, easing = LinearOutSlowInEasing))

        // Ожидание до завершения общей длительности анимации
        val elapsedTime = 450 + 180 + 570 + 780
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
    // Получаем плотность для конвертации пикселей в dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Размеры экрана в пикселях (для линии и фона на всю высоту)
    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }
    
    // Размеры логотипа и позиция разделения между wasser и tech
    var logoW by remember { mutableStateOf(0f) }
    var logoH by remember { mutableStateOf(0f) }
        //var logoCenterX by remember { mutableStateOf(0f) } // X-позиция центра логотипа
    var wasserW by remember { mutableStateOf(0f) } // Ширина wasser части
    var wasserH by remember { mutableStateOf(0f) } // Высота wasser части
    var wasserRightX by remember { mutableStateOf(0f) } // X-позиция правого края wasser
    var wasserTopY by remember { mutableStateOf(0f) } // Y-позиция верха wasser
    var xLineAbsolute by remember { mutableStateOf(0f) } // Абсолютная X-позиция линии разделения (wasser.right + 8px)
    var techXAbsolute by remember { mutableStateOf(0f) } // Абсолютная X-позиция tech (xLineAbsolute + 4px)

    // Внешний контейнер - заполняет весь экран для линии и фона
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenW = it.size.width.toFloat()
                screenH = it.size.height.toFloat()
            },
        contentAlignment = Alignment.Center
    ) {
        // Логотип (wasser) - маленький, по центру экрана
        Box(
            modifier = Modifier
                .widthIn(max = 200.dp)  // Уменьшенная максимальная ширина логотипа
                .wrapContentHeight()
                .scale(scale * 0.7f)    // Уменьшаем масштаб (0.7 от текущего)
                .alpha(alpha)            // Применяем прозрачность
                .onGloballyPositioned { coordinates ->
                    // Сохраняем размеры логотипа для расчетов координат
                    logoW = coordinates.size.width.toFloat()
                    logoH = coordinates.size.height.toFloat()
                }
        ) {
            // Row только для wasser (tech позиционируется абсолютно)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Слой 1: WASSER (красный текст) - статичный, всегда видим слева
                // Сдвигаем wasser левее на 4px
                Image(
                    painter = painterResource(R.drawable.logo_wasser_red),
                    contentDescription = "wasser (red)",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .offset(x = with(density) { -4.dp }) // Сдвигаем левее на 4px
                        .onGloballyPositioned { wasserCoordinates ->
                            wasserW = wasserCoordinates.size.width.toFloat()
                            wasserH = wasserCoordinates.size.height.toFloat()
                            val wasserBounds = wasserCoordinates.boundsInRoot()
                            wasserRightX = wasserBounds.right
                            wasserTopY = wasserBounds.top
                            // Линия должна быть справа от wasser + 8px (расстояние до tech)
                            val spacingPx = with(density) { 8.dp.toPx() } // 8px между wasser и tech
                            xLineAbsolute = wasserRightX + spacingPx
                            // tech должен быть в 4px справа от линии
                            val techOffsetPx = with(density) { 4.dp.toPx() }
                            techXAbsolute = xLineAbsolute + techOffsetPx
                        }
                )
            }
            
        }
        
        // Слой 2: TECH (чёрный текст) - позиционируется абсолютно в 4px справа от красной линии
        // Этот слой виден везде, где нет графитового фона
        // Позиционируем относительно экрана, выравниваем по вертикали с wasser
        if (techXAbsolute > 0f && wasserH > 0f && wasserTopY > 0f) {
            Image(
                painter = painterResource(R.drawable.logo_tech_black),
                contentDescription = "tech (black)",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(
                        x = with(density) { techXAbsolute.toDp() },
                        y = with(density) { wasserTopY.toDp() }
                    )
                    .height(with(density) { wasserH.toDp() }) // Фиксируем высоту равной wasser
                    .wrapContentWidth() // Ширина подстраивается под пропорции изображения
            )
        }

        // Слой 3: Красная вертикальная линия - падает сверху вниз на всю высоту экрана
        // Линия рисуется на позиции разделения между wasser и tech
        Canvas(Modifier.fillMaxSize()) {
            if (screenW > 0f && screenH > 0f && xLineAbsolute > 0f) {
                val yEnd = screenH * lineProgress  // Y-координата конца линии (прогресс падения)
                
                drawLine(
                    color = BrandRed,
                    start = Offset(xLineAbsolute, 0f),      // Начало линии вверху экрана на позиции разделения
                    end = Offset(xLineAbsolute, yEnd),      // Конец линии (меняется в зависимости от прогресса)
                    strokeWidth = 12f,              // Толщина линии
                    cap = StrokeCap.Round           // Закругленные концы
                )
            }
        }

        // Слой 4: Графитовый фон - "вайпится" справа от красной линии на всю высоту экрана
        Canvas(Modifier.fillMaxSize()) {
            if (screenW > 0f && screenH > 0f && xLineAbsolute > 0f) {
                // Правая граница графитового фона (расширяется от линии вправо до края экрана)
                val rightNow = xLineAbsolute + (screenW - xLineAbsolute) * wipeProgress
                
                drawRect(
                    color = Graphite,
                    topLeft = Offset(xLineAbsolute, 0f),  // Начинается от красной линии, сверху экрана
                    size = androidx.compose.ui.geometry.Size(
                        (rightNow - xLineAbsolute).coerceAtLeast(0f),  // Ширина фона (расширяется)
                        screenH  // Высота фона (на всю высоту экрана)
                    )
                )
            }
        }
        
        // Слой 5: TECH (белый текст) - проявляется ТОЛЬКО в зоне графитового фона
        // Позиционируем белый текст TECH в том же месте, что и черный (techXAbsolute)
        // Обрезаем его маской, которая движется вместе с графитовым фоном
        if (screenW > 0f && screenH > 0f && techXAbsolute > 0f && wasserH > 0f && wasserTopY > 0f && wipeProgress > 0f) {
            // Правая граница маски (расширяется от линии вправо вместе с графитовым фоном)
            val maskRight = xLineAbsolute + (screenW - xLineAbsolute) * wipeProgress
            val maskLeftDp = with(density) { xLineAbsolute.toDp() }
            val maskRightDp = with(density) { maskRight.toDp() }
            val maskWidthDp = maskRightDp - maskLeftDp
            val techYAbsoluteDp = with(density) { wasserTopY.toDp() }
            
            // Контейнер для белого текста TECH, обрезанный маской
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                // Маска - обрезает белый tech до зоны графитового фона
                // Маска начинается от xLineAbsolute и расширяется вправо
                Box(
                    modifier = Modifier
                        .offset(x = maskLeftDp, y = 0.dp)
                        .width(maskWidthDp.coerceAtLeast(0.dp))
                        .fillMaxHeight()
                        .clipToBounds()
                ) {
                    // Белый текст TECH - позиционируем относительно маски
                    // techXAbsolute находится в 4px справа от xLineAbsolute (начала маски)
                    // Поэтому offset = 4px от начала маски
                    Image(
                        painter = painterResource(R.drawable.logo_tech_white),
                        contentDescription = "tech (white)",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(
                                x = with(density) { 4.dp }, // 4px от начала маски (xLineAbsolute)
                                y = techYAbsoluteDp
                            )
                            .height(with(density) { wasserH.toDp() }) // Фиксируем высоту равной wasser
                            .wrapContentWidth() // Ширина подстраивается под пропорции изображения
                    )
                }
            }
        }
    }
}
