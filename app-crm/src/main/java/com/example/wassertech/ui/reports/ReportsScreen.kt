package ru.wassertech.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.ui.res.painterResource
import ru.wassertech.crm.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.theme.PdfIconColor
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val reportsDir = remember {
        File(context.getExternalFilesDir(null), "Reports").apply { mkdirs() }
    }
    
    var pdfFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
    }
    
    // Функция для обновления списка файлов
    fun refreshFiles() {
        pdfFiles = reportsDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".pdf", ignoreCase = true) || it.name.endsWith(".html", ignoreCase = true)) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    // Обновляем список файлов при появлении экрана
    LaunchedEffect(Unit) {
        refreshFiles()
    }
    
    // Диалог подтверждения удаления выбранных файлов
    if (showDeleteDialog && selectedFiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить выбранные отчёты?") },
            text = { 
                Text("Вы уверены, что хотите удалить ${selectedFiles.size} отчёт${if (selectedFiles.size == 1) "" else if (selectedFiles.size < 5) "а" else "ов"}?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            selectedFiles.forEach { file ->
                                file.delete()
                            }
                            selectedFiles = emptySet()
                            refreshFiles()
                            showDeleteDialog = false
                        } catch (e: Exception) {
                            android.util.Log.e("ReportsScreen", "Error deleting files", e)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    if (pdfFiles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(ru.wassertech.core.ui.theme.CustomIcons.UiPdf),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    //tint = PdfIconColor
                )
                Text(
                    "Отчёты ТО отсутствуют",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Создайте отчёт на экране деталей ТО",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем системные отступы
            floatingActionButton = {
                if (isEditing && selectedFiles.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showDeleteDialog = true },
                        containerColor = Color(0xFFD32F2F), // Красный цвет
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                            contentDescription = "Удалить выбранные",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp) // Уменьшаем отступ от апбара
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(horizontal = padding.calculateStartPadding(LocalLayoutDirection.current)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pdfFiles, key = { it.absolutePath }) { file ->
                    val isSelected = selectedFiles.contains(file)
                    val isPdf = file.name.endsWith(".pdf", ignoreCase = true)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isEditing) {
                                    // В режиме редактирования - переключаем выбор
                                    selectedFiles = if (isSelected) {
                                        selectedFiles - file
                                    } else {
                                        selectedFiles + file
                                    }
                                } else {
                                    // Обычный режим - открываем файл
                                    if (isPdf) {
                                        openPdf(context, file)
                                    } else {
                                        openHtml(context, file)
                                    }
                                }
                            },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек PDF отчетов
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Чекбокс - только в режиме редактирования
                                if (isEditing) {
                                    IconButton(
                                        onClick = {
                                            selectedFiles = if (isSelected) {
                                                selectedFiles - file
                                            } else {
                                                selectedFiles + file
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = if (isSelected) "Снять выделение" else "Выбрать",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    painter = if (isPdf) {
                                        painterResource(R.drawable.document_pdf)
                                    } else {
                                        // HTML файл - используем кастомную иконку html.xml
                                        painterResource(R.drawable.html)
                                    },
                                    contentDescription = null,
                                    tint = if (isPdf) PdfIconColor else MaterialTheme.colorScheme.onSurface, // Красный для PDF, черный для HTML
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.nameWithoutExtension,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        dateFormatter.format(Date(file.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatFileSize(file.length()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Кнопка удаления - только в режиме редактирования и если ничего не выбрано
                            if (isEditing && selectedFiles.isEmpty()) {
                                IconButton(
                                    onClick = {
                                        selectedFiles = setOf(file)
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error
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

private fun openPdf(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ReportsScreen", "Error opening PDF", e)
    }
}

private fun openHtml(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("ReportsScreen", "Error opening HTML", e)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.2f МБ".format(mb)
        kb >= 1 -> "%.2f КБ".format(kb)
        else -> "$bytes Б"
    }
}

