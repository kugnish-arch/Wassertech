package ru.wassertech.ui.templates

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.types.FieldType
import ru.wassertech.util.Translit
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.sync.markUpdatedForSync
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.core.ui.theme.SegmentedButtonStyle

private const val TAG = "TemplateEditorScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: String,
    vm: TemplatesViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val fields by vm.fields.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }

    var templateName by remember { mutableStateOf<String>("Шаблон") }
    var isHeadComponent by remember { mutableStateOf<Boolean>(false) }

    // Локальный порядок полей для drag-and-drop
    var localFieldOrder by remember(fields.size) {
        mutableStateOf(fields.map { it.id })
    }

    // Обновляем локальный порядок при изменении списка полей
    LaunchedEffect(fields.size, fields.map { it.id }.toSet()) {
        val currentIds = fields.map { it.id }
        val newOrder = localFieldOrder.filter { it in currentIds } +
                currentIds.filter { it !in localFieldOrder }
        if (newOrder != localFieldOrder) {
            localFieldOrder = newOrder
        }
    }

    LaunchedEffect(templateId) {
        vm.load(templateId)
        // заголовок шаблона
        withContext(Dispatchers.IO) {
            try {
                val template = db.componentTemplatesDao().getById(templateId)
                template?.let {
                    templateName = it.name
                    isHeadComponent = it.isHeadComponent
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Состояния для тултипов (вынесены на уровень Scaffold)
    var globalShowHeadComponentInfo by remember { mutableStateOf(false) }
    var globalHeadComponentInfoPosition by remember { mutableStateOf<Offset?>(null) }
    var globalShowCharacteristicInfo by remember { mutableStateOf<Pair<String, Offset?>?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        Log.d(TAG, "Сохранение шаблона: templateId=$templateId")
                        vm.saveAll(localFieldOrder)
                        // Сохраняем имя шаблона и флаг isHeadComponent, если они изменились
                        withContext(Dispatchers.IO) {
                            try {
                                val template = db.componentTemplatesDao().getById(templateId)
                                template?.let { currentTemplate ->
                                    if (currentTemplate.name != templateName || currentTemplate.isHeadComponent != isHeadComponent) {
                                        val updatedTemplate = currentTemplate.copy(
                                            name = templateName,
                                            isHeadComponent = isHeadComponent
                                        ).markUpdatedForSync()
                                        Log.d(TAG, "Обновление шаблона: templateId=$templateId, " +
                                                "oldName=${currentTemplate.name}, newName=$templateName, " +
                                                "oldIsHeadComponent=${currentTemplate.isHeadComponent}, newIsHeadComponent=$isHeadComponent, " +
                                                "dirtyFlag=${updatedTemplate.dirtyFlag}, syncStatus=${updatedTemplate.syncStatus}, " +
                                                "updatedAtEpoch=${updatedTemplate.updatedAtEpoch}")
                                        db.componentTemplatesDao().upsert(updatedTemplate)
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "Ошибка при сохранении шаблона", e)
                            }
                        }
                        Toast.makeText(ctx, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                containerColor = Color(0xFF4CAF50), // Чисто зеленый цвет
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Сохранить",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 200.dp // Дополнительный отступ внизу для клавиатуры
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Поле для редактирования имени шаблона и переключатель "Заглавный компонент"
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Название шаблона") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Переключатель "Заглавный компонент"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Заглавный компонент",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IconButton(
                                    onClick = { 
                                        globalShowHeadComponentInfo = true
                                    },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .onGloballyPositioned { coordinates ->
                                            val position = coordinates.localToWindow(Offset.Zero)
                                            globalHeadComponentInfoPosition = position
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Информация",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isHeadComponent,
                                onCheckedChange = { checked ->
                                    isHeadComponent = checked
                                }
                            )
                        }
                    }
                }
                
            }

            items(localFieldOrder, key = { it }) { fieldId ->
                val f = fields.find { it.id == fieldId } ?: return@items
                val index = localFieldOrder.indexOf(fieldId)
                var lastMoveThreshold by remember { mutableStateOf(0f) }


                Box {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек полей
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Заголовок карточки: ручка для перетаскивания, номер, переключатель "Характеристика" справа
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Левая часть: ручка и номер
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Перетащить",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .pointerInput(f.id, index) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        lastMoveThreshold = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        if (dragAmount.y < -60 && lastMoveThreshold >= -60) {
                                                            val pos =
                                                                localFieldOrder.indexOf(fieldId)
                                                            if (pos > 0) {
                                                                val list =
                                                                    localFieldOrder.toMutableList()
                                                                val tmp = list[pos - 1]
                                                                list[pos - 1] = list[pos]
                                                                list[pos] = tmp
                                                                localFieldOrder = list
                                                            }
                                                            lastMoveThreshold = -60f
                                                        } else if (dragAmount.y > 60 && lastMoveThreshold <= 60) {
                                                            val pos =
                                                                localFieldOrder.indexOf(fieldId)
                                                            if (pos >= 0 && pos < localFieldOrder.lastIndex) {
                                                                val list =
                                                                    localFieldOrder.toMutableList()
                                                                val tmp = list[pos + 1]
                                                                list[pos + 1] = list[pos]
                                                                list[pos] = tmp
                                                                localFieldOrder = list
                                                            }
                                                            lastMoveThreshold = 60f
                                                        }
                                                        if (dragAmount.y in -60f..60f) {
                                                            lastMoveThreshold = dragAmount.y
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        lastMoveThreshold = 0f
                                                    }
                                                )
                                            }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${index + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Правая часть: переключатель "Характеристика" с иконкой info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "Характеристика",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        IconButton(
                                            onClick = { 
                                                globalShowCharacteristicInfo = Pair(f.id, null)
                                            },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    val position = coordinates.localToWindow(Offset.Zero)
                                                    globalShowCharacteristicInfo = Pair(f.id, position)
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Информация",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = f.isCharacteristic, // true = характеристика, false = чек-лист ТО
                                        onCheckedChange = { checked ->
                                            vm.update(f.id) { it.copy(isCharacteristic = checked) }
                                        }
                                    )
                                }
                            }

                            // 1) Имя (label). Key скрыт — генерируется автоматически.
                            OutlinedTextField(
                                value = f.label,
                                onValueChange = { newLabel ->
                                    val prevAuto = Translit.ruToEnKey(f.label)
                                    val looksAuto = f.key.isBlank() ||
                                            f.key == prevAuto ||
                                            f.key.startsWith("field_") ||
                                            (f.key.any { it.isDigit() } && f.key.length >= 12)
                                    val newAuto = Translit.ruToEnKey(newLabel)
                                    vm.update(f.id) {
                                        it.copy(
                                            label = newLabel,
                                            key = if (looksAuto) newAuto else it.key
                                        )
                                    }
                                },
                                label = { Text("Имя") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // 2) Сегменты типа поля (уменьшенная ширина) и корзина справа
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                                    var selected by remember(
                                        f.id,
                                        f.type
                                    ) { mutableStateOf(f.type) }
                                    SegmentedButton(
                                        selected = selected == FieldType.TEXT,
                                        onClick = {
                                            selected = FieldType.TEXT; vm.setType(
                                            f.id,
                                            FieldType.TEXT
                                        )
                                        },
                                        shape = SegmentedButtonStyle.getShape(index = 0, count = 3),
                                        label = {
                                            Text(
                                                "TXT",
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )
                                    SegmentedButton(
                                        selected = selected == FieldType.CHECKBOX,
                                        onClick = {
                                            selected = FieldType.CHECKBOX; vm.setType(
                                            f.id,
                                            FieldType.CHECKBOX
                                        )
                                        },
                                        shape = SegmentedButtonStyle.getShape(index = 1, count = 3),
                                        label = {
                                            Text(
                                                "CHK",
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )
                                    SegmentedButton(
                                        selected = selected == FieldType.NUMBER,
                                        onClick = {
                                            selected = FieldType.NUMBER; vm.setType(
                                            f.id,
                                            FieldType.NUMBER
                                        )
                                        },
                                        shape = SegmentedButtonStyle.getShape(index = 2, count = 3),
                                        label = {
                                            Text(
                                                "NUM",
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )
                                }
                                // Корзина справа от сегментированных кнопок
                                IconButton(onClick = { vm.remove(f.id) }) {
                                    Icon(imageVector = ru.wassertech.core.ui.theme.DeleteIcon, contentDescription = "Удалить")
                                }
                            }

                            // 4) Только для NUMBER — ед. изм. + Min/Max
                            if (f.type == FieldType.NUMBER) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = f.unit.orEmpty(),
                                        onValueChange = { new -> vm.update(f.id) { it.copy(unit = new) } },
                                        label = { Text("Ед. изм.") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = f.min.orEmpty(),
                                        onValueChange = { new -> vm.update(f.id) { it.copy(min = new) } },
                                        label = { Text("Min") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = f.max.orEmpty(),
                                        onValueChange = { new -> vm.update(f.id) { it.copy(max = new) } },
                                        label = { Text("Max") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            } else {
                                // Не NUMBER тип, ничего не показываем
                            }

                        }

                    }
                }
            }

            // Кнопка "Добавить поле" внизу списка
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { vm.addField() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935), // Красный цвет
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить поле")
                    }
                }
            }
        }
        
        // Тултипы поверх всего контента
        if (globalShowHeadComponentInfo && globalHeadComponentInfoPosition != null) {
            InfoTooltip(
                text = "Заглавный компонент - в готовом отчёте будет занимать всю ширину листа, а не 1/3 как обычные компоненты. Если заглавный элемент окажется в самом начале или конце компонента, то под него будет выделен отдельный визуальный раздел.",
                anchorPosition = globalHeadComponentInfoPosition,
                onDismiss = { globalShowHeadComponentInfo = false }
            )
        }
        
        globalShowCharacteristicInfo?.let { (fieldId, position) ->
            if (position != null) {
                InfoTooltip(
                    text = "Характеристика - постоянное свойство компонента (паспортные данные железа). Оно будет сохраняться и выводиться в некоторых документах, но его значение не меняется при проведении обслуживания. Если переключатель выключен, поле относится к чек-листу ТО (параметры обслуживания).",
                    anchorPosition = position,
                    onDismiss = { globalShowCharacteristicInfo = null }
                )
            }
        }
    }
}

/**
 * Компонент подсказки с жёлтым фоном, иконкой лампочки и крестиком для закрытия
 * Позиционируется относительно anchorPosition (координаты иконки Info)
 */
@Composable
private fun InfoTooltip(
    text: String,
    anchorPosition: Offset?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current

    // Используем Box с fillMaxSize для верхнего слоя
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
            .zIndex(1000f), // Верхний слой
        contentAlignment = Alignment.Center
    ) {
        // Полупрозрачный фон
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxSize()
        ) {}

        // Карточка с подсказкой - позиционируется относительно иконки
        if (anchorPosition != null) {
            Card(
                modifier = Modifier
                    .offset(
                        x = with(density) { anchorPosition.x.toDp() - 250.dp }, // Смещаем влево от иконки
                        y = with(density) { anchorPosition.y.toDp() - 50.dp } // Позиционируем на уровне иконки (чуть выше)
                    )
                    .widthIn(max = 300.dp)
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.85f) // Жёлтый фон с прозрачностью 85%
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок с иконкой лампочки и крестиком
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFF57F17), // Тёмно-жёлтый для иконки
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Подсказка",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1A1A1A) // Тёмный текст на жёлтом фоне
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = Color(0xFF1A1A1A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Текст подсказки
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        } else {
            // anchorPosition == null, карточка не показывается
        }
    }
}

