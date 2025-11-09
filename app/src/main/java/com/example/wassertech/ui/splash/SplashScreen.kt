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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
    var wasserW by remember { mutableStateOf(0f) }
    var wasserH by remember { mutableStateOf(0f) }
    var techW by remember { mutableStateOf(0f) }
    var techH by remember { mutableStateOf(0f) }
    var totalW by remember { mutableStateOf(0f) }
    var totalH by remember { mutableStateOf(0f) }

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
                    .onGloballyPositioned {
                        totalW = it.size.width.toFloat()
                        totalH = it.size.height.toFloat()
                    }
            ) {
                // Row для размещения wasser и tech рядом
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1) WASSER (красный) — слева
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .onGloballyPositioned {
                                wasserW = it.size.width.toFloat()
                                wasserH = it.size.height.toFloat()
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_wasser_red),
                            contentDescription = "wasser (red)",
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 2) Вертикальная КРАСНАЯ линия: растёт сверху вниз между wasser и tech
                    if (wasserH > 0) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(wasserH.dp * lineProgress)
                        ) {
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val lineHeight = wasserH * lineProgress
                                drawLine(
                                    color = BrandRed,
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, lineHeight),
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // 3) TECH (чёрный) — базовый слой справа, с той же высотой что и wasser
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .onGloballyPositioned {
                                techW = it.size.width.toFloat()
                                techH = it.size.height.toFloat()
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_tech_black),
                            contentDescription = "tech (black)",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(if (wasserH > 0) wasserH.dp else Dp.Unspecified)
                        )

                        // 4) ГРАФИТОВЫЙ ФОН: наползает справа налево на tech (поверх чёрного tech)
                        if (techW > 0 && techH > 0) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clipToBounds()
                            ) {
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    drawRect(
                                        color = Graphite,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(
                                            width = techW * wipeProgress,
                                            height = techH
                                        )
                                    )
                                }
                            }
                        }

                        // 5) TECH (белый) — проявляем ТОЛЬКО В ЗОНЕ графитового фона (поверх графита)
                        if (techW > 0 && techH > 0 && wipeProgress > 0f) {
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
        }
    }
}


