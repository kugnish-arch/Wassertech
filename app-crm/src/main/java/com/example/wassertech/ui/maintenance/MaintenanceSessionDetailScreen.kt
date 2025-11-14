package ru.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.res.painterResource
import ru.wassertech.crm.R
import ru.wassertech.core.ui.R as CoreR
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.MaintenanceSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import ru.wassertech.feature.reports.ReportAssembler
import ru.wassertech.feature.reports.HtmlTemplateEngine
import ru.wassertech.feature.reports.PdfExporter
import ru.wassertech.feature.reports.ShareUtils
import android.util.Log;

@Composable
fun MaintenanceSessionDetailScreen(
    sessionId: String,
    onNavigateToEdit: (sessionId: String, siteId: String, installationId: String, installationName: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    var session by remember { mutableStateOf<MaintenanceSessionEntity?>(null) }
    var header by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<DetailComponentGroup>>(emptyList()) }
    var clientName by remember { mutableStateOf<String>("Клиент") }
    var isCorporate by remember { mutableStateOf<Boolean>(false) }
    var installationName by remember { mutableStateOf<String>("Установка") }
    var dateTimeText by remember { mutableStateOf<String>("") }

    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy (HH:mm)", Locale.forLanguageTag("ru"))
            .apply { timeZone = TimeZone.getDefault() }
    }

    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionId) {
        withContext(Dispatchers.IO) {
            val s = db.sessionsDao().getSessionById(sessionId)
            session = s

            // Names and header
            val instName = s?.installationId?.let { id ->
                db.hierarchyDao().getInstallation(id)?.name ?: "Установка"
            } ?: "Установка"
            installationName = instName
            val dt = s?.startedAtEpoch?.let { dateFormatter.format(Date(it)) } ?: ""
            dateTimeText = dt
            header = "$instName — $dt"

            // Client
            val siteId = s?.siteId
            if (siteId != null) {
                val site = db.hierarchyDao().getSite(siteId)
                val client = site?.let { db.clientDao().getClient(it.clientId) }
                if (client != null) {
                    clientName = client.name
                    isCorporate = client.isCorporate
                }
            }

            // Values grouped by component, with field label resolution
            val values = db.sessionsDao().getValuesForSession(sessionId)
            val byComponent = values.groupBy { it.componentId }
            
            // Получаем все компоненты установки для сортировки по orderIndex
            val installationId = s?.installationId
            val allComponents = if (installationId != null) {
                db.hierarchyDao().getComponentsNow(installationId)
            } else {
                emptyList()
            }
            val componentOrderMap = allComponents.associate { it.id to it.orderIndex }
            
            val list = mutableListOf<DetailComponentGroup>()
            for ((componentId, vals) in byComponent) {
                val comp = db.hierarchyDao().getComponent(componentId)
                val compName = comp?.name ?: componentId

                // Map fieldKey -> label via template, if available
                val labels: Map<String, String> = comp?.templateId?.let { tid ->
                    try {
                        db.templatesDao().getMaintenanceFieldsForTemplate(tid)
                            .associate { it.key to it.label } // label не nullable, elvis не нужен
                    } catch (e: Exception) {
                        emptyMap()
                    }
                } ?: emptyMap()

                val rows = vals.map { v ->
                    val rawKey = v.fieldKey // fieldKey не nullable, elvis не нужен
                    val label = labels[rawKey] ?: rawKey.substringBefore('_', rawKey)
                    val valueText = when {
                        v.valueText != null -> v.valueText
                        v.valueBool != null -> if (v.valueBool == true) "Да" else "Нет"
                        else -> ""
                    }
                    DetailFieldRow(fieldLabel = label, value = valueText)
                }.sortedBy { it.fieldLabel.lowercase(Locale.getDefault()) }
                val orderIndex = componentOrderMap[componentId] ?: Int.MAX_VALUE
                list.add(DetailComponentGroup(componentName = compName, rows = rows, orderIndex = orderIndex))
            }
            // Сортируем по orderIndex, затем по имени
            groups = list.sortedWith(compareBy({ it.orderIndex }, { it.componentName.lowercase(Locale.getDefault()) }))
        }
    }

    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Сессия не найдена")
        }
        return
    }

    // Функция для создания PDF
    val createPdf: () -> Unit = {
        scope.launch {
            exporting = true
            try {
                Log.d("PDF", "Starting PDF generation for sessionId: $sessionId")
                
                // Подготовка DTO на IO потоке
                Log.d("PDF", "Assembling report DTO...")
                val dto = withContext(Dispatchers.IO) {
                    val appDb = AppDatabase.getInstance(context)
                    ReportAssembler.assemble(appDb, context, sessionId)
                }
                Log.d("PDF", "Report DTO assembled successfully, reportNumber: ${dto.reportNumber}")

                // Путь для PDF (заменяем "/" на "_" в номере отчета для имени файла)
                val reportsDir = File(
                    context.getExternalFilesDir(null),
                    "Reports"
                ).apply { mkdirs() }
                val fileName = "Report_${dto.reportNumber.replace("/", "_")}.pdf"
                val out = File(reportsDir, fileName)
                Log.d("PDF", "PDF file path: ${out.absolutePath}")

                // HTML шаблон
                Log.d("PDF", "Rendering HTML template...")
                val html = withContext(Dispatchers.IO) {
                    HtmlTemplateEngine.render(
                        context = context,
                        templateAssetPath = "templates/maintenance_v3.html",
                        dto = dto
                    )
                }
                Log.d("PDF", "HTML rendered, length: ${html.length}")
                
                // Проверяем настройку сохранения HTML
                Log.d("PDF", "Checking save_html setting...")
                val shouldSaveHtml = withContext(Dispatchers.IO) {
                    val setting = AppDatabase.getInstance(context).settingsDao().getValueSync("save_html")
                    setting?.toBoolean() ?: false
                }
                Log.d("PDF", "save_html setting: $shouldSaveHtml")
                
                // Сохраняем HTML рядом с PDF, если настройка включена
                if (shouldSaveHtml) {
                    val htmlFile = File(reportsDir, fileName.replace(".pdf", ".html"))
                    htmlFile.writeText(html, Charsets.UTF_8)
                    Log.d("PDF", "HTML saved to: ${htmlFile.absolutePath}")
                }
                
                // HTML -> PDF (на Main потоке, так как WebView требует Main thread)
                Log.d("PDF", "Starting PDF export on Main thread...")
                withContext(Dispatchers.Main) {
                    PdfExporter.exportHtmlToPdf(context, html, out, dto.reportNumber)
                }
                Log.d("PDF", "PDF export completed successfully")

                // Шарим созданный файл
                Log.d("PDF", "Sharing PDF file...")
                ShareUtils.sharePdf(context, out)
                Log.d("PDF", "PDF shared successfully")
                
                // Показываем информацию об успешной генерации PDF
                snackbarHostState.showSnackbar(
                    "PDF создан успешно",
                    duration = SnackbarDuration.Short
                )
            } catch (t: Throwable) {
                Log.e("PDF", "Error creating PDF", t)
                val errorMsg = when {
                    t is kotlinx.coroutines.TimeoutCancellationException -> "Превышено время ожидания (30 сек). Попробуйте еще раз."
                    t.cause is kotlinx.coroutines.TimeoutCancellationException -> "Превышено время ожидания. Попробуйте еще раз."
                    t.message?.contains("WebView") == true -> "Ошибка загрузки HTML: ${t.message}"
                    t.message?.contains("timeout") == true -> "Превышено время ожидания. Попробуйте еще раз."
                    t.message?.contains("Renderer process") == true -> "Ошибка рендеринга PDF. Попробуйте еще раз."
                    else -> "Ошибка при создании PDF: ${t.message ?: t.javaClass.simpleName}"
                }
                snackbarHostState.showSnackbar(errorMsg)
            } finally {
                Log.d("PDF", "PDF generation finished, setting exporting = false")
                exporting = false
            }
        }
    }

    // Функция для редактирования
    val navigateToEdit: () -> Unit = {
        session?.let { s ->
            onNavigateToEdit(
                sessionId,
                s.siteId,
                s.installationId ?: "",
                installationName
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Два FABа: черный Edit сверху, красный PDF снизу
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Черный FAB для редактирования (сверху) - скрываем во время экспорта
                if (!exporting) {
                    AppFloatingActionButton(
                        template = FABTemplate(
                            icon = Icons.Filled.Edit,
                            containerColor = Color(0xFF1E1E1E), // Черный цвет
                            contentColor = Color.White,
                            onClick = navigateToEdit
                        )
                    )
                }
                
                // Красный FAB для PDF (снизу) - показываем CircularProgressIndicator во время экспорта
                FloatingActionButton(
                    onClick = {
                        if (!exporting) {
                            createPdf()
                        }
                    },
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    shape = CircleShape, // Явно указываем круглую форму
                    modifier = Modifier.size(56.dp) // Размер для круглой формы
                ) {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp), // Увеличена иконка
                            strokeWidth = 3.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.document_pdf),
                            contentDescription = "Создать PDF",
                            modifier = Modifier.size(28.dp) // Увеличена иконка PDF
                        )
                    }
                }
            }
        }
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header card (используем стиль из темы)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                                ),
                                shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(ru.wassertech.core.ui.theme.HeaderCardStyle.padding),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Выбираем иконку клиента в зависимости от типа
                                        val clientIconRes = if (isCorporate) CoreR.drawable.person_client_corporate_blue else CoreR.drawable.person_client_blue
                                        Image(
                                            painter = painterResource(id = clientIconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(clientName, style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle, color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = CoreR.drawable.equipment_filter_triple),
                                            contentDescription = null,
                                            modifier = Modifier.size(ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            installationName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(dateTimeText, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            // Бордер снизу как у групп клиентов
                            HorizontalDivider(
                                color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                                thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                            )
                        }
                    }

                    items(groups) { g ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                        ) {
                            ListItem(
                                headlineContent = { Text(g.componentName) },
                                supportingContent = {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        g.rows.forEach { row ->
                                            Text("• ${row.fieldLabel}: ${row.value}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    // Конец записей
                    item {
                        Spacer(Modifier.height(12.dp)) // Уменьшенный отступ от последней карточки
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center // Выравнивание по центру
                        ) {
                            Text(
                                text = "Конец записей...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Renamed to avoid clash with MaintenanceAllScreen.kt
private data class DetailComponentGroup(
    val orderIndex: Int = Int.MAX_VALUE,
    val componentName: String,
    val rows: List<DetailFieldRow>
)

private data class DetailFieldRow(
    val fieldLabel: String,
    val value: String
)