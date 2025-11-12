package ru.wassertech.core.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.R
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/* ================== ПАРАМЕТРЫ ================== */

// Цвета
private val BrandRed = Color(0xFFE53935)
private val Graphite = Color(0xFF1E1E1E)

// Целевая доля общей ширины (wasser + tech)
private const val TOTAL_WIDTH_FRACTION = 0.8f

// Отступы (в пикселях — переведём позже через density)
private const val GAP_WASSER_LINE_PX = 8f           // расстояние между правым краем wasser и красной линией
private const val LINE_TO_TECH_GAP_PX = 4f          // расстояние между линией и левым краем tech
private const val LINE_STROKE_PX = 12f              // толщина линии (используем для визуала, геометрию не расширяем, линия «внутри» отступа)

// Позиционирование tech: его ЛЕВЫЙ край стремится в SCREEN * 2/3
private const val TECH_LEFT_SCREEN_FRACTION = 2f / 3f

// Минимальные поля, чтобы не обрезало (px)
private const val MIN_LEFT_MARGIN_PX = 0f
private const val MIN_RIGHT_MARGIN_PX = 16f

// Ограничение высоты группы по отношению к экрану (если слишком высокая) — safeguard
private const val MAX_HEIGHT_FRACTION = 0.5f  // 50% высоты экрана (можно увеличить)

// Анимационные тайминги
private const val WASSER_SLIDE_DURATION = 450
private const val PAUSE_BEFORE_LINE = 180
private const val LINE_FALL_DURATION = 300
private const val WIPE_DURATION = 780

// Старт отдаления WASSER (сколько ширин экрана дополнительно слева)
private const val WASSER_START_EXTRA_SCREENS = 1.0f

// Фактор для портретной высоты TECH (если покажется выше — можно < 1f)
private const val PORTRAIT_TECH_HEIGHT_FACTOR = 1.0f

/* ================================================= */

@Composable
fun SplashRouteFixed(
    onFinished: () -> Unit,
    totalMs: Int = 1500
) {
    // Прогрессы
    val slideProgress = remember { Animatable(0f) }
    val lineProgress = remember { Animatable(0f) }
    val wipeProgress = remember { Animatable(0f) }

    var geometryReady by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(geometryReady) {
        if (geometryReady && !started) {
            started = true
            // Прилет wasser
            slideProgress.animateTo(
                1f,
                tween(WASSER_SLIDE_DURATION, easing = FastOutSlowInEasing)
            )
            delay(PAUSE_BEFORE_LINE.toLong())
            // Линия
            lineProgress.animateTo(
                1f,
                tween(LINE_FALL_DURATION, easing = FastOutLinearInEasing)
            )
            // Вайп
            wipeProgress.animateTo(
                1f,
                tween(WIPE_DURATION, easing = LinearOutSlowInEasing)
            )
            val elapsed = WASSER_SLIDE_DURATION + PAUSE_BEFORE_LINE + LINE_FALL_DURATION + WIPE_DURATION
            delay((totalMs - elapsed).coerceAtLeast(0).toLong())
            onFinished()
        }
    }

    SplashLogoCore(
        slideProgress = slideProgress.value,
        lineProgress = lineProgress.value,
        wipeProgress = wipeProgress.value,
        onGeometryReady = { geometryReady = it }
    )
}

@Composable
private fun SplashLogoCore(
    slideProgress: Float,
    lineProgress: Float,
    wipeProgress: Float,
    onGeometryReady: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWdp = config.screenWidthDp
    val screenHdp = config.screenHeightDp
    val screenWpx = with(density) { screenWdp.dp.toPx() }
    val screenHpx = with(density) { screenHdp.dp.toPx() }
    val isLandscape = screenWpx >= screenHpx

    val wasserPainter = painterResource(R.drawable.logo_wasser_red)
    val techBlackPainter = painterResource(R.drawable.logo_tech_black)
    val techWhitePainter = painterResource(R.drawable.logo_tech_white)

    // Intrinsic размеры
    val wasserIntrinsic = wasserPainter.intrinsicSize
    val techIntrinsic = techBlackPainter.intrinsicSize

    val wasserAspect = if (wasserIntrinsic.height > 0 && wasserIntrinsic.width > 0)
        wasserIntrinsic.width / wasserIntrinsic.height else 3f // fallback
    val techAspect = if (techIntrinsic.height > 0 && techIntrinsic.width > 0)
        techIntrinsic.width / techIntrinsic.height else 3f // fallback

    // Целевая общая ширина
    val targetTotalWidth = TOTAL_WIDTH_FRACTION * screenWpx

    // Высота (H) из уравнения: (wasserAspect + techAspect) * H = targetTotalWidth
    var rawH = targetTotalWidth / (wasserAspect + techAspect)

    // Ограничиваем по высоте экрана
    val maxH = screenHpx * MAX_HEIGHT_FRACTION
    if (rawH > maxH) rawH = maxH

    // Фактическая высота картинок
    val techHeight = rawH * (if (isLandscape) 1f else PORTRAIT_TECH_HEIGHT_FACTOR)
    val wasserHeight = rawH // wasser всегда точная высота без фактора

    // Пересчитанные ширины
    val wasserWidth = wasserAspect * wasserHeight
    val techWidth = techAspect * techHeight

    // Итоговая (возможно слегка нарушенная из-за фактора) суммарная ширина
    val totalGroupWidth = wasserWidth + techWidth + GAP_WASSER_LINE_PX + LINE_TO_TECH_GAP_PX

    // Изначально пытаемся поставить левый край TECH на 2/3 экрана
    var techLeft = TECH_LEFT_SCREEN_FRACTION * screenWpx

    // Линия располагается слева от tech
    var lineX = techLeft - LINE_TO_TECH_GAP_PX
    // Правый край wasser = lineX - GAP_WASSER_LINE_PX
    var wasserRight = lineX - GAP_WASSER_LINE_PX
    var wasserLeft = wasserRight - wasserWidth

    // Коррекция: если wasserLeft < MIN_LEFT_MARGIN_PX, двигаем всё вправо
    val shiftRightIfNeeded = (MIN_LEFT_MARGIN_PX - wasserLeft).coerceAtLeast(0f)
    if (shiftRightIfNeeded > 0f) {
        wasserLeft += shiftRightIfNeeded
        wasserRight += shiftRightIfNeeded
        lineX += shiftRightIfNeeded
        techLeft += shiftRightIfNeeded
    }

    // Коррекция справа: если techRight > screenW - MIN_RIGHT_MARGIN_PX — двигаем влево
    val techRight = techLeft + techWidth
    val overflowRight = (techRight + MIN_RIGHT_MARGIN_PX) - screenWpx
    if (overflowRight > 0f) {
        // Сдвиг влево всей группы
        wasserLeft -= overflowRight
        wasserRight -= overflowRight
        lineX -= overflowRight
        techLeft -= overflowRight
    }

    // Вертикально по центру
    val topY = (screenHpx - wasserHeight) / 2f  // высоты совпадают (по условию)
    val techTopY = (screenHpx - techHeight) / 2f // может отличаться, если применён фактор

    // Анимация прилёта wasser: старт далеко слева
    val wasserStartLeft = wasserLeft - screenWpx * WASSER_START_EXTRA_SCREENS - wasserWidth
    val wasserAnimatedLeft = lerp(wasserStartLeft, wasserLeft, slideProgress)

    // Готовность геометрии (у нас всё аналитическое → всегда true, если размеры > 0)
    val ready = wasserHeight > 0 && techHeight > 0 && screenWpx > 0 && screenHpx > 0
    LaunchedEffect(ready) { onGeometryReady(ready) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        // WASSER (летит)
        Image(
            painter = wasserPainter,
            contentDescription = "wasser",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = with(density) { wasserAnimatedLeft.toDp() },
                    y = with(density) { topY.toDp() }
                )
                .height(with(density) { wasserHeight.toDp() })
        )

        // TECH (чёрный) — стоит на месте с начала
        Image(
            painter = techBlackPainter,
            contentDescription = "tech black",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = with(density) { techLeft.toDp() },
                    y = with(density) { techTopY.toDp() }
                )
                .height(with(density) { techHeight.toDp() })
        )

        // После прилёта (slideProgress == 1f) запускаем остальные эффекты
        if (slideProgress >= 1f) {

            // Падающая линия
            Canvas(Modifier.fillMaxSize()) {
                val yEnd = size.height * lineProgress
                drawLine(
                    color = BrandRed,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, yEnd),
                    strokeWidth = LINE_STROKE_PX,
                    cap = StrokeCap.Round
                )
            }

            // Графитовый фон (вайп)
            Canvas(Modifier.fillMaxSize()) {
                val rightNow = lineX + (size.width - lineX) * wipeProgress
                if (rightNow > lineX) {
                    drawRect(
                        color = Graphite,
                        topLeft = Offset(lineX, 0f),
                        size = Size(rightNow - lineX, size.height)
                    )
                }
            }

            // Белый TECH (маска по вайпу) — тот же размер и позиция, что и чёрный
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val maskRight = lineX + (size.width - lineX) * wipeProgress
                        if (maskRight > lineX) {
                            clipRect(
                                left = lineX,
                                top = 0f,
                                right = maskRight,
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
            ) {
                Image(
                    painter = techWhitePainter,
                    contentDescription = "tech white",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = with(density) { techLeft.toDp() },
                            y = with(density) { techTopY.toDp() }
                        )
                        .height(with(density) { techHeight.toDp() })
                )
            }
        }
    }
}

/* Линейная интерполяция */
private fun lerp(start: Float, end: Float, t: Float): Float =
    start + (end - start) * t

