package ru.wassertech.core.screens.remote

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
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
                uiState.isLoading && uiState.points.isEmpty() -> {
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
                
                uiState.points.isEmpty() -> {
                    // Пустое состояние
                    AppEmptyState(
                        icon = Icons.Default.DeviceThermostat,
                        title = "Нет данных",
                        description = "Данные о температуре ещё не получены. Пожалуйста, подождите."
                    )
                }
                
                else -> {
                    // График температуры
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Заголовок с информацией о устройстве
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Устройство: ${uiState.deviceId}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Точек данных: ${uiState.points.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // График температуры
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            TemperatureChart(
                                points = uiState.points,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
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
    
    // Сортируем точки по времени
    val sortedPoints = points.sortedBy { it.timestampMillis }
    
    // Создаём entries для графика
    val entries = remember(sortedPoints) {
        sortedPoints.mapIndexed { index, point ->
            FloatEntry(
                x = index.toFloat(),
                y = point.valueCelsius
            )
        }
    }
    
    // Форматтер для оси X (время)
    val xAxisFormatter = remember(sortedPoints) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < sortedPoints.size) {
                val point = sortedPoints[index]
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                format.format(Date(point.timestampMillis))
            } else {
                ""
            }
        }
    }
    
    // Форматтер для оси Y (температура)
    val yAxisFormatter = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            String.format(Locale.getDefault(), "%.1f°C", value)
        }
    }
    
    // Создаём модель данных для графика
    val chartEntryModelProducer = remember {
        ChartEntryModelProducer(listOf(entries))
    }
    
    // Обновляем модель при изменении точек
    LaunchedEffect(entries) {
        chartEntryModelProducer.setEntries(listOf(entries))
    }
    
    val startAxis = rememberStartAxis(
        valueFormatter = yAxisFormatter
    )
    
    val bottomAxis = rememberBottomAxis(
        valueFormatter = xAxisFormatter,
        tickLength = 4.dp
    )
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = MaterialTheme.colorScheme.primary.value.toInt(),
                    lineThicknessDp = 2f,
                    pointSizeDp = 4f
                )
            ),
            spacing = 8.dp
        ),
        chartModelProducer = chartEntryModelProducer,
        startAxis = startAxis,
        bottomAxis = bottomAxis,
        modifier = modifier
    )
}

