package ru.wassertech.core.screens.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.screens.remote.ui.RemoteMonitorUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared-экран для удалённого мониторинга температуры
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteMonitorSharedScreen(
    uiState: RemoteMonitorUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            // Заголовок убран, используется заголовок внешнего экрана
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.points1.isEmpty() && uiState.points2.isEmpty() -> {
                    // Индикатор загрузки при первой загрузке
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    // Сообщение об ошибке
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ошибка загрузки данных",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                uiState.points1.isEmpty() && uiState.points2.isEmpty() -> {
                    // Пустое состояние
                    AppEmptyState(
                        icon = Icons.Default.DeviceThermostat,
                        title = "Нет данных",
                        description = "Данные о температуре ещё не получены. Пожалуйста, подождите."
                    )
                }

                else -> {
                    // График(и) температуры
                    if (uiState.isSingleDeviceMode) {
                        // Режим с одним графиком
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    // Заголовок с именем датчика и текущим значением
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uiState.deviceId1} (${uiState.points1.size})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        // Текущее значение температуры
                                        if (uiState.points1.isNotEmpty()) {
                                            val lastValue = uiState.points1.lastOrNull()?.valueCelsius
                                            if (lastValue != null) {
                                                Text(
                                                    text = String.format(
                                                        Locale.getDefault(),
                                                        "%.1f°C",
                                                        lastValue
                                                    ),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF4CAF50), // Ярко-зеленый цвет
                                                    modifier = Modifier
                                                        .background(
                                                            Color.White,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (uiState.points1.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Нет данных",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        TemperatureChart(
                                            points = uiState.points1,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        )
                                    }
                                }
                            }
                            
                            if (uiState.isLoading) {
                                // Индикатор загрузки при обновлении
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } else {
                        // Режим с двумя графиками
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // График первого устройства
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    // Заголовок с именем датчика и текущим значением
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uiState.deviceId1} (${uiState.points1.size})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        // Текущее значение температуры
                                        if (uiState.points1.isNotEmpty()) {
                                            val lastValue = uiState.points1.lastOrNull()?.valueCelsius
                                            if (lastValue != null) {
                                                Text(
                                                    text = String.format(
                                                        Locale.getDefault(),
                                                        "%.1f°C",
                                                        lastValue
                                                    ),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF4CAF50), // Ярко-зеленый цвет
                                                    modifier = Modifier
                                                        .background(
                                                            Color.White,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (uiState.points1.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Нет данных",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        TemperatureChart(
                                            points = uiState.points1,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        )
                                    }
                                }
                            }

                            // График второго устройства
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    // Заголовок с именем датчика и текущим значением
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uiState.deviceId2} (${uiState.points2.size})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        // Текущее значение температуры
                                        if (uiState.points2.isNotEmpty()) {
                                            val lastValue = uiState.points2.lastOrNull()?.valueCelsius
                                            if (lastValue != null) {
                                                Text(
                                                    text = String.format(
                                                        Locale.getDefault(),
                                                        "%.1f°C",
                                                        lastValue
                                                    ),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF4CAF50), // Ярко-зеленый цвет
                                                    modifier = Modifier
                                                        .background(
                                                            Color.White,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (uiState.points2.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Нет данных",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        TemperatureChart(
                                            points = uiState.points2,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        )
                                    }
                                }
                            }

                            if (uiState.isLoading) {
                                // Индикатор загрузки при обновлении
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Компонент графика температуры с использованием Vico
 */
@Composable
fun TemperatureChart(
    points: List<TemperaturePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет данных для отображения",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // Мемоизируем отсортированные точки и ограничиваем до последних 1000 точек (самые новые)
    // Это ограничение нужно только для оптимизации памяти, так как сервер может вернуть больше данных
    val sortedPoints = remember(points) {
        val sorted = points.sortedBy { it.timestampMillis }
        val allLimited = sorted.takeLast(1000) // Храним последние 1000 точек (самые новые)
        android.util.Log.e(
            "TemperatureChart",
            "=== ОГРАНИЧЕНИЕ ТОЧЕК: было=${sorted.size}, храним=${allLimited.size} ==="
        )
        allLimited
    }

    // Для отображения берем только последние 60 точек, чтобы автоматически показывать последние данные
    val displayPoints = remember(sortedPoints) {
        sortedPoints.takeLast(48) // Показываем только последние 60 точек
    }

    // Вычисляем диапазон оси Y: от min - 2 до max + 2 градуса (на основе отображаемых точек)
    val yRange = remember(displayPoints) {
        if (displayPoints.isEmpty()) {
            Pair(0f, 10f) // Значения по умолчанию
        } else {
            val minY = displayPoints.minOf { it.valueCelsius }
            val maxY = displayPoints.maxOf { it.valueCelsius }
            val padding = 2f // Отступ ±2 градуса
            Pair(
                (minY - padding).coerceAtLeast(0f), // Не ниже 0
                maxY + padding
            )
        }
    }

    // Нормализуем значения Y в диапазон 0-100 для лучшего отображения
    // Затем в форматтере оси Y будем преобразовывать обратно
    val entries = remember(displayPoints, yRange) {
        val (minY, maxY) = yRange
        val range = maxY - minY
        if (range > 0) {
            displayPoints.mapIndexed { index, point ->
                // Нормализуем: (value - min) / range * 100
                val normalizedY = ((point.valueCelsius - minY) / range) * 100f
                FloatEntry(
                    x = index.toFloat(),
                    y = normalizedY
                )
            }
        } else {
            displayPoints.mapIndexed { index, _ ->
                FloatEntry(
                    x = index.toFloat(),
                    y = 50f // Среднее значение, если диапазон нулевой
                )
            }
        }
    }

    // Используем все entries для одной линии, но последнюю точку выделим отдельно
    val allEntries = remember(entries) {
        entries
    }

    // Значение последней точки для подписи (если понадобится)
    val lastPointValue = remember(displayPoints) {
        displayPoints.lastOrNull()?.valueCelsius
    }

    Column(modifier = modifier) {

        // Producer создаём ОДИН РАЗ
        val chartEntryModelProducer = remember { ChartEntryModelProducer() }

        // Обновляем данные при изменении entries - используем более надежный ключ для обновления
        LaunchedEffect(
            entries.size,
            entries.firstOrNull()?.y,
            entries.lastOrNull()?.y,
            points.size,
            points.lastOrNull()?.timestampMillis
        ) {
            android.util.Log.e(
                "TemperatureChart",
                "=== ОБНОВЛЕНИЕ ГРАФИКА: points=${points.size}, " +
                        "sortedPoints=${sortedPoints.size}, entries=${entries.size} ==="
            )

            // Устанавливаем одну линию со всеми точками
            val linesToSet = mutableListOf<List<FloatEntry>>()
            if (allEntries.isNotEmpty()) {
                // Одна линия со всеми точками
                linesToSet.add(allEntries)
            }

            if (linesToSet.isNotEmpty()) {
                chartEntryModelProducer.setEntries(linesToSet)
                android.util.Log.d(
                    "TemperatureChart",
                    "Entries set successfully, all=${allEntries.size}"
                )
            } else {
                android.util.Log.w("TemperatureChart", "Entries is empty!")
                chartEntryModelProducer.setEntries(listOf(emptyList()))
            }
        }

        // Форматтер оси Y - преобразуем нормализованные значения обратно в реальные температуры
        val yAxisFormatter = remember(yRange) {
            val (minY, maxY) = yRange
            val range = maxY - minY
            AxisValueFormatter<AxisPosition.Vertical.Start> { normalizedValue, _ ->
                // Денормализуем: normalizedValue / 100 * range + minY
                val realValue = if (range > 0) {
                    (normalizedValue / 100f) * range + minY
                } else {
                    minY
                }
                String.format(Locale.getDefault(), "%.1f°C", realValue)
            }
        }

        // Форматтер оси X - показываем метки каждые 5 точек, считая справа (от последней точки)
        val xAxisFormatter = remember(displayPoints) {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                val index = value.toInt()
                if (index in displayPoints.indices) {
                    val totalPoints = displayPoints.size
                    // Вычисляем расстояние от последней точки (справа)
                    val distanceFromEnd = totalPoints - 1 - index
                    // Показываем метку, если это последняя точка или каждая 3-я точка, считая справа
                    val shouldShow = distanceFromEnd % 3 == 0

                    if (shouldShow) {
                        val point = displayPoints[index]
                        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                        format.format(Date(point.timestampMillis))
                    } else {
                        "" // Не показываем метку
                    }
                } else {
                    ""
                }
            }
        }

        val startAxis = rememberStartAxis(
            valueFormatter = yAxisFormatter,
            guideline = null // Отключаем сетку по оси Y
        )

        val bottomAxis = rememberBottomAxis(
            valueFormatter = xAxisFormatter,
            tickLength = 4.dp,
            labelRotationDegrees = 270f
        )

        // Получаем цвета в Composable контексте
        val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
        
        // Создаём одну линию со всеми точками (все точки красные) с градиентом под линией
        val lineSpecs = remember(allEntries, primaryColorArgb) {
            if (allEntries.isNotEmpty()) {
                // Создаём градиент от цвета линии до прозрачного
                val gradientStartColor = Color(primaryColorArgb).copy(alpha = 0.3f).toArgb()
                val gradientEndColor = Color(primaryColorArgb).copy(alpha = 0f).toArgb()
                
                listOf(
                    LineChart.LineSpec(
                        lineColor = primaryColorArgb,
                        lineThicknessDp = 2f,
                        point = ShapeComponent(
                            shape = Shapes.pillShape,
                            color = primaryColorArgb
                        ),
                        pointSizeDp = 4f,
                        pointConnector = DefaultPointConnector(cubicStrength = 0f)
                        // Примечание: В Vico 1.13.1 градиент под линией может не поддерживаться
                        // через lineBackgroundShader. Если градиент не отображается, 
                        // потребуется обновление библиотеки до более новой версии.
                    )
                )
            } else {
                emptyList()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Chart(
                chart = lineChart(
                    lines = lineSpecs,
                    spacing = 2.67.dp // Уменьшили spacing еще в 1.5 раза для устранения скролла
                ),
                chartModelProducer = chartEntryModelProducer,
                startAxis = startAxis,
                bottomAxis = bottomAxis,
                modifier = Modifier
                    .fillMaxSize()
                    // Блокируем горизонтальный скролл и зум жестами
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ ->
                            // Игнорируем горизонтальные жесты для блокировки скролла
                        }
                    }
            )
        }
    }
}
