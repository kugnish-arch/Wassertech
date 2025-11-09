package com.example.wassertech.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import com.example.wassertech.R

private val BrandRed = Color(0xFFE53935)
private val Graphite = Color(0xFF1E1E1E)

@Composable
fun SplashRouteV2(
    onFinished: () -> Unit,
    totalMs: Int = 1500
) {
    // 1) Появление и «дыхание»
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.96f) }

    // 2) Рост вертикальной красной линии (0→1 по высоте)
    val lineProgress = remember { Animatable(0f) }

    // 3) Наползание графитового фона (0→1 по ширине правой части)
    val wipeProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Параллели/стыковки таймингов
        alpha.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))

        // Чуть позже стартует линия, растёт сверху вниз
        delay(120L)
        lineProgress.animateTo(1f, tween(380, easing = FastOutLinearInEasing))

        // Фон начинает наползать одновременно с линией, но немного позже
        // чтобы линия была видна сначала
        delay(200L)
        wipeProgress.animateTo(1f, tween(500, easing = LinearOutSlowInEasing))

        // Небольшая пауза и переход дальше
        delay((totalMs - 300 - 380 - 500 - 200).coerceAtLeast(0).toLong())
        onFinished()
    }

    SplashAnimatedLogoV2(
        alpha = alpha.value,
        scale = scale.value,
        lineProgress = lineProgress.value,
        wipeProgress = wipeProgress.value
    )
}

@Composable
private fun SplashAnimatedLogoV2(
    alpha: Float,
    scale: Float,
    lineProgress: Float,
    wipeProgress: Float
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        // Геометрия логотипа
        var wasserW by remember { mutableStateOf(0f) }
        var wasserH by remember { mutableStateOf(0f) }
        var techW by remember { mutableStateOf(0f) }
        var techH by remember { mutableStateOf(0f) }
        var lineX by remember { mutableStateOf(0f) } // X координата красной линии (между wasser и tech)
        var logoContainerX by remember { mutableStateOf(0f) } // X координата контейнера логотипа

        // Логотип размещается в центре экрана с небольшим смещением вправо
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Контейнер для логотипа, который позволяет нам вычислить позицию линии
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .scale(scale * 0.4f) // Уменьшаем в 2.5 раза
                    .alpha(alpha)
                    .padding(start = (screenWidthPx * 0.15f).dp) // Смещение вправо
                    .onGloballyPositioned { layoutCoordinates ->
                        // Получаем позицию контейнера относительно окна
                        logoContainerX = layoutCoordinates.localToWindow(Offset.Zero).x
                        // Обновляем позицию линии когда известны размеры wasser
                        if (wasserW > 0) {
                            val spacerWidthPx = with(density) { 8.dp.toPx() }
                            lineX = logoContainerX + wasserW + spacerWidthPx / 2f
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 1) WASSER (красный) — слева
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .onGloballyPositioned {
                                wasserW = it.size.width.toFloat()
                                wasserH = it.size.height.toFloat()
                                // Обновляем позицию линии когда известны размеры wasser
                                if (logoContainerX > 0) {
                                    val spacerWidthPx = with(density) { 8.dp.toPx() }
                                    lineX = logoContainerX + wasserW + spacerWidthPx / 2f
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_wasser_red),
                            contentDescription = "wasser (red)",
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 2) Невидимый спейсер для красной линии (между wasser и tech)
                    Spacer(modifier = Modifier.width(8.dp))

                    // 3) TECH (чёрный и белый) — справа
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .onGloballyPositioned {
                                techW = it.size.width.toFloat()
                                techH = it.size.height.toFloat()
                            }
                    ) {
                        // Базовый слой - TECH черный
                        Image(
                            painter = painterResource(R.drawable.logo_tech_black),
                            contentDescription = "tech (black)",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(if (wasserH > 0) wasserH.dp else Dp.Unspecified)
                        )

                        // 4) ГРАФИТОВЫЙ ФОН: наползает от красной линии вправо на tech
                        // Фон появляется только когда линия прошла достаточно далеко (после 20% прогресса)
                        if (techW > 0 && techH > 0 && wipeProgress > 0f && lineProgress > 0.2f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clipToBounds()
                            ) {
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Фон начинается от левого края tech (где красная линия)
                                    val wipeWidth = techW * wipeProgress
                                    drawRect(
                                        color = Graphite,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(
                                            width = wipeWidth,
                                            height = techH
                                        )
                                    )
                                }
                            }
                        }

                        // 5) TECH (белый) — проявляем ТОЛЬКО В ЗОНЕ графитового фона (поверх графита)
                        // Белый tech появляется вместе с графитовым фоном
                        if (techW > 0 && techH > 0 && wipeProgress > 0f && lineProgress > 0.2f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clipToBounds()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .drawWithContent {
                                            val clipRight = techW * wipeProgress
                                            clipRect(
                                                left = 0f,
                                                top = 0f,
                                                right = clipRight,
                                                bottom = techH
                                            ) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.logo_tech_white),
                                        contentDescription = "tech (white masked)",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.height(techH.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Красная линия - на всю высоту экрана, падает сверху вниз
            // Рисуем её поверх всего контента
            if (lineX > 0 && screenHeightPx > 0) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Линия падает сверху вниз на всю высоту экрана
                        val lineHeight = screenHeightPx * lineProgress

                        drawLine(
                            color = BrandRed,
                            start = Offset(lineX, 0f),
                            end = Offset(lineX, lineHeight),
                            strokeWidth = 12f, // Увеличена толщина линии
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}


