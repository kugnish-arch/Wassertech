package com.example.wassertech.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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

        // После появления линии начинаем фон-впайку справа
        wipeProgress.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))

        // Небольшая пауза и переход дальше
        delay((totalMs - 300 - 380 - 520).coerceAtLeast(0).toLong())
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
    // Геометрия логотипа
    var logoW by remember { mutableStateOf(0f) }
    var logoH by remember { mutableStateOf(0f) }

    // Смещение вправо ~ на треть ширины экрана
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(0.33f)) // ≈ 1/3 вправо от центра
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .scale(scale)
                    .alpha(alpha)
            ) {
                // Слой: полотно для графики
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .onGloballyPositioned {
                            logoW = it.size.width.toFloat()
                            logoH = it.size.height.toFloat()
                        }
                ) {
                    // 1) WASSER (красный) — статичен
                    Image(
                        painter = painterResource(R.drawable.logo_wasser_red),
                        contentDescription = "wasser (red)",
                        contentScale = ContentScale.Fit
                    )

                    // 2) TECH (чёрный) — базовый слой
                    Image(
                        painter = painterResource(R.drawable.logo_tech_black),
                        contentDescription = "tech (black)",
                        contentScale = ContentScale.Fit
                    )

                    // 3) Вертикальная КРАСНАЯ линия: растёт сверху вниз
                    Canvas(
                        modifier = Modifier.matchParentSize()
                    ) {
                        val xLine = (logoW * 0.63f) // позиция разделения между "wasser" и "tech"
                        val yEnd = logoH * lineProgress
                        drawLine(
                            color = BrandRed,
                            start = Offset(xLine, 0f),
                            end = Offset(xLine, yEnd),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 4) ГРАФИТОВЫЙ ФОН: наползает справа налево от красной линии
                    Canvas(
                        modifier = Modifier.matchParentSize()
                    ) {
                        val xLine = (logoW * 0.63f)
                        val startX = xLine
                        val wipeX = startX + (logoW - startX) * wipeProgress
                        drawRect(
                            color = Graphite,
                            topLeft = Offset(startX, 0f),
                            size = Size(
                                width = (wipeX - startX).coerceAtLeast(0f),
                                height = logoH
                            )
                        )
                    }

                    // 5) TECH (белый) — проявляем ТОЛЬКО В ЗОНЕ графитового фона
                    // Белый TECH, обрезанный расширяющимся прямоугольником
                    if (logoW > 0 && logoH > 0) {
                        val xLine = logoW * 0.63f
                        val wipeWidth = (logoW - xLine) * wipeProgress
                        
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clipToBounds()
                        ) {
                            // Белый TECH изображение с клиппингом через draw modifier
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .drawWithContent {
                                        val clipRight = xLine + wipeWidth
                                        clipRect(
                                            left = xLine,
                                            top = 0f,
                                            right = clipRight,
                                            bottom = logoH
                                        ) {
                                            this@drawWithContent.drawContent()
                                        }
                                    }
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.logo_tech_white),
                                    contentDescription = "tech (white masked)",
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


