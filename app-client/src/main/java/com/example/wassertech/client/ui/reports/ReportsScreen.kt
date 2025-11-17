package ru.wassertech.client.ui.reports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ReportEntity
import ru.wassertech.client.repository.ReportsRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран списка отчётов для app-client.
 * Показывает отчёты текущего клиента с возможностью их открытия.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: (() -> Unit)? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val reportsRepository = remember { ReportsRepository(context) }
    
    // Состояния
    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0 to 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Получаем список отчётов из БД
    val reportsFlow = remember { reportsRepository.observeReportsForCurrentClient() }
    val reports by reportsFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    // При первом показе экрана запускаем синхронизацию и скачивание
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        
        try {
            // Синхронизируем список отчётов
            val syncResult = reportsRepository.syncReportsForCurrentClient()
            if (syncResult.isFailure) {
                syncResult.exceptionOrNull()?.let { error ->
                    errorMessage = "Ошибка синхронизации: ${error.message}"
                }
                isLoading = false
                isDownloading = false
                return@LaunchedEffect
            }
            
            // После успешной синхронизации скачиваем недостающие файлы
            val reportsToDownload = withContext(Dispatchers.IO) {
                val session = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
                val clientId = session?.clientId
                if (clientId != null) {
                    AppDatabase.getInstance(context).reportsDao().getReportsToDownload(clientId)
                } else {
                    emptyList()
                }
            }
            
            if (reportsToDownload.isNotEmpty()) {
                isDownloading = true
                downloadProgress = 0 to reportsToDownload.size
                
                val downloadResult = reportsRepository.downloadMissingReportsForCurrentClient { current, total ->
                    downloadProgress = current to total
                }
                
                if (downloadResult.isFailure) {
                    downloadResult.exceptionOrNull()?.let { error ->
                        errorMessage = "Не удалось загрузить часть отчётов: ${error.message}"
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка: ${e.message}"
        } finally {
            isLoading = false
            isDownloading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёты") },
                navigationIcon = {
                    onNavigateBack?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(paddingValues)
        ) {
            // Основной контент
            if (reports.isEmpty() && !isLoading && !isDownloading) {
                // Пустое состояние
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Нет отчётов",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Отчёты по техническому обслуживанию появятся здесь после их загрузки инженером.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reports) { report ->
                        ReportItem(
                            report = report,
                            onClick = {
                                openReport(context, report)
                            }
                        )
                    }
                }
            }
            
            // Оверлей загрузки
            if (isLoading || isDownloading) {
                LoadingOverlay(
                    isDownloading = isDownloading,
                    progress = downloadProgress
                )
            }
            
            // Сообщение об ошибке
            errorMessage?.let { error ->
                LaunchedEffect(error) {
                    kotlinx.coroutines.delay(5000) // Скрываем через 5 секунд
                    errorMessage = null
                }
                
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}

/**
 * Элемент списка отчёта.
 */
@Composable
private fun ReportItem(
    report: ReportEntity,
    onClick: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))
            .apply { timeZone = TimeZone.getDefault() }
    }
    
    val dateText = remember(report.createdAtEpoch) {
        dateFormatter.format(Date(report.createdAtEpoch))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (report.isDownloaded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.fileName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!report.isDownloaded) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Не скачан",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (report.isDownloaded) {
                Icon(
                    imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Оверлей загрузки с прогрессом.
 */
@Composable
private fun LoadingOverlay(
    isDownloading: Boolean,
    progress: Pair<Int, Int>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = if (isDownloading) {
                        "Загружаем отчёты…"
                    } else {
                        "Синхронизация…"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (isDownloading && progress.second > 0) {
                    Text(
                        text = "Отчёт ${progress.first} из ${progress.second}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress.first.toFloat() / progress.second.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Открывает PDF-файл отчёта.
 */
private fun openReport(context: android.content.Context, report: ReportEntity) {
    if (!report.isDownloaded || report.localFilePath.isNullOrBlank()) {
        return
    }
    
    val file = File(report.localFilePath)
    if (!file.exists()) {
        return
    }
    
    try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ReportsScreen", "Ошибка открытия PDF", e)
    }
}

