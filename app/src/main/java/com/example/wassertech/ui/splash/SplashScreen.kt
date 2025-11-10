package com.example.wassertech.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.wassertech.R
import kotlinx.coroutines.delay

// Цвета бренда
private val BrandRed = Color(0xFFE53935)
private val Graphite = Color(0xFF1E1E1E)

// Отступы
private val SPACING_BETWEEN_PARTS_DP = 8.dp
private val SPACING_LINE_TO_TECH_DP = 4.dp

// Настраиваемые коэффициенты
private const val PORTRAIT_SCALE_DIVIDER = 1.5f          // уменьшение масштаба в портретном
private const val LANDSCAPE_SHIFT_FRACTION = 0.5f        // доля ширины TECH для сдвига влево в ландшафте
private const val PORTRAIT_SHIFT_FRACTION = 0.5f         // доля ширины TECH для сдвига влево в портрете

@Composable
fun SplashRouteFixed(
    onFinished: () -> Unit,
    totalMs: Int = 1500,
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.96f) }
    val lineProgress = remember { Animatable(0f) }
    val wipeProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
        scale.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        delay(180)
        lineProgress.animateTo(1f, tween(570, easing = FastOutLinearInEasing))
        wipeProgress.animateTo(1f, tween(780, easing = LinearOutSlowInEasing))
        val elapsed = 450 + 180 + 570 + 780
        delay((totalMs - elapsed).coerceAtLeast(0).toLong())
        onFinished()
    }

    SplashLogoFixed(
        alpha = alpha.value,
        scale = scale.value,
        lineProgress = lineProgress.value,
        wipeProgress = wipeProgress.value
    )
}

@Composable
private fun SplashLogoFixed(
    alpha: Float,
    scale: Float,
    lineProgress: Float,
    wipeProgress: Float
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }

    var wasserRightX by remember { mutableStateOf<Float?>(null) }
    var wasserTopY by remember { mutableStateOf<Float?>(null) }
    var wasserHeight by remember { mutableStateOf<Float?>(null) }

    // Painter TECH для вычисления аспект‑рейшо
    val techPainter = painterResource(R.drawable.logo_tech_black)
    val techIntrinsic = techPainter.intrinsicSize
    val techAspectRatio = remember(techIntrinsic) {
        if (techIntrinsic.height > 0) techIntrinsic.width / techIntrinsic.height else 1f
    }

    // Базовый масштаб ряда (как было) + корректировка для портретного режима
    val baseRowScale = scale * 0.7f * if (isLandscape) 1f else (1f / PORTRAIT_SCALE_DIVIDER)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenW = it.size.width.toFloat()
                screenH = it.size.height.toFloat()
            }
            .clipToBounds()
    ) {
        // Только WASSER по центру + смещение влево после вычисления ширины TECH
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(baseRowScale)
                .alpha(alpha)
                // Смещение добавим позже, когда появится wasserHeight (через remember/derived)
                .let { baseMod ->
                    val h = wasserHeight
                    if (h != null) {
                        val techWidthPx = h * techAspectRatio // ширина TECH при FillHeight
                        val shiftFraction = if (isLandscape) LANDSCAPE_SHIFT_FRACTION else PORTRAIT_SHIFT_FRACTION
                        val shiftPx = techWidthPx * shiftFraction
                        baseMod.offset(x = with(density) { (-shiftPx).toDp() })
                    } else baseMod
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_wasser_red),
                contentDescription = "wasser red",
                contentScale = ContentScale.Fit,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val b = coords.boundsInRoot()
                    wasserRightX = b.right
                    wasserTopY = b.top
                    wasserHeight = b.height
                }
            )
        }

        // Локальные snapshot-значения
        val wasserRightXVal = wasserRightX
        val wasserTopYVal = wasserTopY
        val wasserHeightVal = wasserHeight

        // lineX = правый край wasser + spacing
        val lineX = remember(wasserRightXVal) {
            wasserRightXVal?.let { right ->
                right + with(density) { SPACING_BETWEEN_PARTS_DP.toPx() }
            }
        }

        // techLeftX = lineX + spacing от линии до TECH
        val techLeftX = remember(lineX) {
            lineX?.let {
                it + with(density) { SPACING_LINE_TO_TECH_DP.toPx() }
            }
        }

        // Черный TECH
        if (techLeftX != null && wasserTopYVal != null && wasserHeightVal != null) {
            Image(
                painter = painterResource(R.drawable.logo_tech_black),
                contentDescription = "tech black",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = with(density) { techLeftX.toDp() },
                        y = with(density) { wasserTopYVal.toDp() }
                    )
                    .height(with(density) { wasserHeightVal.toDp() })
            )
        }

        // Графитовый фон (вайп)
        if (lineX != null) {
            Canvas(Modifier.fillMaxSize()) {
                val rightNow = lineX + (screenW - lineX) * wipeProgress
                if (rightNow > lineX) {
                    drawRect(
                        color = Graphite,
                        topLeft = Offset(lineX, 0f),
                        size = androidx.compose.ui.geometry.Size(rightNow - lineX, screenH)
                    )
                }
            }
        }

        // Белый TECH (проявляется маской)
        if (lineX != null && techLeftX != null && wasserTopYVal != null && wasserHeightVal != null) {
            val maskRight = lineX + (screenW - lineX) * wipeProgress

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
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
                    painter = painterResource(R.drawable.logo_tech_white),
                    contentDescription = "tech white",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = with(density) { techLeftX.toDp() },
                            y = with(density) { wasserTopYVal.toDp() }
                        )
                        .height(with(density) { wasserHeightVal.toDp() })
                )
            }
        }

        // Красная линия поверх
        if (lineX != null) {
            Canvas(Modifier.fillMaxSize()) {
                val yEnd = screenH * lineProgress
                drawLine(
                    color = BrandRed,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, yEnd),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}