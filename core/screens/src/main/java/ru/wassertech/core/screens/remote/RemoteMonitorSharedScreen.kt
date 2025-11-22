package ru.wassertech.core.screens.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.chart.line.LineChart
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
            TopAppBar(
                title = { Text("Мониторинг температуры") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
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
                    // Два графика температуры
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
                                        text = uiState.deviceId1,
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
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color(0xFF4CAF50) // Ярко-зеленый цвет
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
                                        text = uiState.deviceId2,
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
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color(0xFF4CAF50) // Ярко-зеленый цвет
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

    // Мемоизируем отсортированные точки и ограничиваем до последних 50 точек для хранения
    // Но для отображения показываем только последние 30 точек, чтобы автоматически видеть последние данные
    val sortedPoints = remember(points) {
        val sorted = points.sortedBy { it.timestampMillis }
        val allLimited = sorted.takeLast(50) // Храним последние 50 для истории
        android.util.Log.e(
            "TemperatureChart",
            "=== ОГРАНИЧЕНИЕ ТОЧЕК: было=${sorted.size}, храним=${allLimited.size} ==="
        )
        allLimited
    }

    // Для отображения берем только последние 30 точек, чтобы автоматически показывать последние данные
    val displayPoints = remember(sortedPoints) {
        sortedPoints.takeLast(30) // Показываем только последние 30 точек
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

    // Разделяем entries: основная линия без последней точки и отдельная линия только с последней точкой
    val mainEntries = remember(entries) {
        if (entries.size > 1) {
            entries.dropLast(1)
        } else {
            emptyList()
        }
    }

    val lastEntry = remember(entries) {
        if (entries.isNotEmpty()) {
            listOf(entries.last())
        } else {
            emptyList()
        }
    }

    // Значение последней точки для подписи (если понадобится)
    val lastPointValue = remember(displayPoints) {
        displayPoints.lastOrNull()?.valueCelsius
    }

    Column(modifier = modifier) {

        // Producer создаём ОДИН РАЗ
        val chartEntryModelProducer = remember { ChartEntryModelProducer() }

        // Обновляем данные при изменении entries - используем points как ключ для стабильности
        LaunchedEffect(
            points.size,
            points.firstOrNull()?.timestampMillis,
            points.lastOrNull()?.timestampMillis
        ) {
            android.util.Log.e(
                "TemperatureChart",
                "=== ОБНОВЛЕНИЕ ГРАФИКА: points=${points.size}, " +
                        "sortedPoints=${sortedPoints.size}, entries=${entries.size} ==="
            )

            // Устанавливаем две линии: основную и последнюю точку отдельно
            val linesToSet = mutableListOf<List<FloatEntry>>()
            if (mainEntries.isNotEmpty()) {
                linesToSet.add(mainEntries)
            }
            if (lastEntry.isNotEmpty()) {
                linesToSet.add(lastEntry)
            }

            if (linesToSet.isNotEmpty()) {
                chartEntryModelProducer.setEntries(linesToSet)
                android.util.Log.d(
                    "TemperatureChart",
                    "Entries set successfully, main=${mainEntries.size}, last=${lastEntry.size}"
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
                val intValue = realValue.toInt()
                String.format(Locale.getDefault(), "%d°C", intValue)
            }
        }

        // Форматтер оси X - показываем метки только каждые 4 точки (1 через 3)
        val xAxisFormatter = remember(displayPoints) {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                val index = value.toInt()
                if (index in displayPoints.indices) {
                    // Показываем метку только каждые 4 точки (index % 4 == 0), плюс первую и последнюю
                    val shouldShow = index == 0 ||
                            index == displayPoints.size - 1 ||
                            index % 4 == 0

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

        // Стиль графика (нужен, чтобы взять дефолтный point-компонент)
        val chartStyle = currentChartStyle

        // Создаём две линии: основная (тонкая) и последняя точка (ярко-зеленая, крупная)
        val lineSpecs = remember(mainEntries, lastEntry, chartStyle) {
            val specs = mutableListOf<LineChart.LineSpec>()

            // Общий point-компонент из текущего стиля (чтобы точки реально рисовались)
            val defaultPointComponent = chartStyle.lineChart.lines.firstOrNull()?.point

            // Основная линия без последней точки
            if (mainEntries.isNotEmpty()) {
                specs.add(
                    lineSpec(
                        lineColor = MaterialTheme.colorScheme.primary,
                        lineThickness = 2.dp,
                        point = defaultPointComponent,
                        pointSize = 6.dp,
                        pointConnector = DefaultPointConnector(cubicStrength = 0f)
                    )
                )
            }

            // Последняя точка - ярко-зеленая и крупная (линии нет, только точка)
            if (lastEntry.isNotEmpty()) {
                specs.add(
                    lineSpec(
                        lineColor = Color(0xFF4CAF50),
                        lineThickness = 0.dp,
                        point = defaultPointComponent,
                        pointSize = 12.dp,
                        pointConnector = DefaultPointConnector(cubicStrength = 0f)
                    )
                )
            }

            specs
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Chart(
                chart = lineChart(
                    lines = lineSpecs,
                    spacing = 8.dp
                ),
                chartModelProducer = chartEntryModelProducer,
                startAxis = startAxis,
                bottomAxis = bottomAxis,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
