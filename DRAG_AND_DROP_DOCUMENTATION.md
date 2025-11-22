# Документация: Система Drag-and-Drop перетаскивания элементов

## Обзор

В приложении Wassertech реализована система drag-and-drop для изменения порядка элементов (клиентов, объектов, установок, компонентов, шаблонов) с помощью библиотеки `compose-reorderable`. Система позволяет перетаскивать элементы для изменения их порядка, при этом изменения сохраняются в базу данных только при выходе из режима редактирования.

## Архитектура

### Основные компоненты

1. **`ReorderableLazyColumn`** (`core/ui/reorderable/ReorderableLazyColumn.kt`)
   - Обертка над стандартным `LazyColumn` с поддержкой перетаскивания
   - Использует библиотеку `compose-reorderable` для реализации drag-and-drop
   - Автоматически применяет `detectReorderAfterLongPress` для активации перетаскивания при длительном нажатии

2. **`ReorderableState`**
   - Обертка над `ReorderableLazyListState` из библиотеки
   - Используется для передачи состояния перетаскивания в компоненты элементов

3. **`detectReorder`** и **`detectReorderAfterLongPress`**
   - Модификаторы для активации перетаскивания
   - `detectReorder` - активирует перетаскивание при нажатии на элемент (используется для ручки перетаскивания)
   - `detectReorderAfterLongPress` - активирует перетаскивание при длительном нажатии (применяется к `ReorderableLazyColumn`)

## Режим редактирования

### Управление состоянием

Режим редактирования управляется через `AppTopBar` и передается в экраны через параметры:
- `isEditing: Boolean` - текущее состояние режима редактирования
- `onToggleEdit: (() -> Unit)?` - функция для переключения режима редактирования
- `onSave: (() -> Unit)?` - функция для сохранения изменений (опционально)
- `onCancel: (() -> Unit)?` - функция для отмены изменений (опционально)

### Кнопка в AppTopBar

В `AppTopBar` отображается кнопка редактирования:
- **Вне режима редактирования:** Иконка "Редактировать" (карандаш)
- **В режиме редактирования:** Кнопки "Сохранить" (галочка) и "Отмена" (крестик)

Кнопка показывается только на экранах, где поддерживается редактирование:
- `clients` - список клиентов
- `client/{id}` - детали клиента
- `site/{id}` - детали объекта
- `installation/{id}` - детали установки
- `templates` - шаблоны компонентов
- `reports` - отчеты
- `maintenance_history` - история обслуживания

## Локальное состояние порядка

### Принцип работы

Для корректного отображения перетаскивания используется локальное состояние порядка элементов:

```kotlin
// Локальный порядок элементов (список ID)
var localOrder by remember { mutableStateOf<List<String>>(emptyList()) }

// Инициализация при входе в режим редактирования
LaunchedEffect(items, isEditing) {
    if (!isEditing) {
        // В обычном режиме синхронизируем с данными из БД
        localOrder = items.map { it.id }
    } else {
        // При входе в режим редактирования фиксируем текущий порядок
        localOrder = items.map { it.id }
    }
}
```

### Сохранение порядка

Порядок сохраняется в БД только при выходе из режима редактирования:

```kotlin
LaunchedEffect(isEditing) {
    if (!isEditing && localOrder.isNotEmpty()) {
        // Сохраняем порядок в БД
        scope.launch {
            localOrder.forEachIndexed { index, id ->
                dao.setSortOrder(id, index)
            }
        }
    }
}
```

## Использование ReorderableLazyColumn

### Базовый пример

```kotlin
ReorderableLazyColumn(
    items = localOrder, // Список ID элементов
    onMove = { fromIndex, toIndex ->
        // Обновляем локальное состояние
        val mutable = localOrder.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        localOrder = mutable
    },
    modifier = Modifier.fillMaxSize(),
    key = { it }, // Функция для получения уникального ключа
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) { itemId, isDragging, reorderableState ->
    // itemId - ID элемента из localOrder
    // isDragging - true, если элемент перетаскивается
    // reorderableState - состояние для передачи в detectReorder
    
    val item = itemsById[itemId] ?: return@ReorderableLazyColumn
    
    ItemRow(
        item = item,
        isDragging = isDragging,
        reorderableState = reorderableState,
        isEditing = isEditing,
        onToggleEdit = onToggleEdit
    )
}
```

### Автоматическое включение режима редактирования

При начале перетаскивания (long press) автоматически включается режим редактирования, если он еще не включен:

```kotlin
@Composable
fun ItemRow(
    item: ItemEntity,
    isDragging: Boolean,
    reorderableState: ReorderableState?,
    isEditing: Boolean,
    onToggleEdit: (() -> Unit)?
) {
    var hasTriggeredEditMode by remember { mutableStateOf(false) }
    
    LaunchedEffect(isDragging, isEditing) {
        if (isDragging && !isEditing && !hasTriggeredEditMode && onToggleEdit != null) {
            hasTriggeredEditMode = true
            onToggleEdit()
        }
        if (!isDragging || isEditing) {
            hasTriggeredEditMode = false
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isEditing && onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        // Ручка для перетаскивания (только в режиме редактирования)
        if (isEditing && !isArchived) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Перетащить",
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (reorderableState != null) {
                            Modifier.detectReorder(reorderableState)
                        } else {
                            Modifier
                        }
                    )
            )
        }
        // Остальное содержимое строки
    }
}
```

## Визуальные элементы

### Ручка перетаскивания

Ручка перетаскивания (иконка меню) отображается только:
- В режиме редактирования (`isEditing == true`)
- Для неархивных элементов (`!isArchived`)

Ручка применяет модификатор `detectReorder(reorderableState)` для активации перетаскивания при нажатии.

### Состояние перетаскивания

При перетаскивании элемент:
- Отображается поверх остальных (`zIndex = 1f`)
- Имеет анимацию перемещения (`animateItemPlacement()`)
- Визуально выделяется (тень, изменение прозрачности)

## Примеры реализации

### 1. Экран списка клиентов (ClientsScreen)

**Особенности:**
- Поддержка группировки клиентов по группам
- Перетаскивание между группами
- Сохранение порядка для каждой группы отдельно

**Ключевые моменты:**
```kotlin
// Локальный порядок для общей секции
var localOrderGeneral by remember { mutableStateOf<List<String>>(emptyList()) }

// Локальный порядок по группам
var localOrderByGroup by remember {
    mutableStateOf<MutableMap<String, List<String>>>(mutableMapOf())
}

// Сохранение при выходе из режима редактирования
LaunchedEffect(isEditing) {
    if (!isEditing && localOrderGeneral.isNotEmpty()) {
        onReorderGroupClients(null, localOrderGeneral)
        groups.forEach { g ->
            onReorderGroupClients(g.id, localOrderByGroup[g.id] ?: emptyList())
        }
    }
}
```

### 2. Экран деталей клиента (ClientDetailScreen)

**Особенности:**
- Перетаскивание объектов (sites)
- Вложенное перетаскивание установок внутри объектов
- Поддержка архивных элементов в режиме редактирования

**Ключевые моменты:**
```kotlin
// Локальный порядок объектов
var localOrder by remember { mutableStateOf(sitesToShow.map { it.id }) }

// Локальный порядок установок по объектам
var localInstallationOrders by remember {
    mutableStateOf<Map<String, List<String>>>(emptyMap())
}

// Вложенный ReorderableLazyColumn для установок
ReorderableLazyColumn(
    items = filteredInstallationOrder,
    onMove = { fromIndex, toIndex ->
        // Обновляем локальный порядок установок
        val mutable = currentFilteredOrder.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        localInstallationOrders = localInstallationOrders.toMutableMap().apply {
            put(siteId, newOrder)
        }
    }
) { instId, isDraggingInstallation, installationReorderableState ->
    // Компонент установки
}
```

### 3. Экран шаблонов компонентов (TemplatesScreen)

**Особенности:**
- Простое перетаскивание без группировки
- Сохранение порядка через `sortOrder` в БД

**Ключевые моменты:**
```kotlin
// Локальный порядок шаблонов
var localOrder by remember(templates, isEditing) { 
    mutableStateOf(templates.map { it.id }) 
}

// Сохранение порядка
LaunchedEffect(isEditing) {
    if (!isEditing && localOrder.isNotEmpty()) {
        scope.launch {
            localOrder.forEachIndexed { index, id ->
                dao.setSortOrder(id, index)
            }
        }
    }
}
```

## Обработка архивных элементов

### Фильтрация

В обычном режиме архивные элементы скрыты:
```kotlin
val visibleItems = if (isEditing && includeArchived) {
    allItems // Показываем все в режиме редактирования
} else {
    allItems.filter { it.isArchived != true } // Скрываем архивные
}
```

### Перетаскивание архивных элементов

Архивные элементы:
- Не показывают ручку перетаскивания
- Не могут быть перетащены (блокируются в UI)
- Отображаются с пониженной прозрачностью

## Сохранение изменений

### Стратегия сохранения

1. **Локальное состояние** - изменения применяются сразу к `localOrder` для визуального отображения
2. **Отложенное сохранение** - изменения сохраняются в БД только при выходе из режима редактирования
3. **Отмена изменений** - при отмене локальное состояние восстанавливается из БД

### Пример сохранения

```kotlin
LaunchedEffect(isEditing) {
    if (!isEditing && localOrder.isNotEmpty()) {
        // Сохраняем порядок в БД
        scope.launch(Dispatchers.IO) {
            localOrder.forEachIndexed { index, id ->
                dao.setSortOrder(id, index)
            }
        }
    }
}
```

## Интеграция с EntityRowWithMenu

Компонент `EntityRowWithMenu` поддерживает drag-and-drop через параметры:
- `reorderableState: ReorderableState?` - состояние для перетаскивания
- `showDragHandle: Boolean` - показывать ли ручку перетаскивания
- `isDragging: Boolean` - перетаскивается ли элемент
- `onToggleEdit: (() -> Unit)?` - функция для включения режима редактирования

## Рекомендации по использованию

1. **Всегда используйте `ReorderableLazyColumn`** вместо обычного `LazyColumn` для списков с возможностью перетаскивания
2. **Инициализируйте `localOrder`** при входе в режим редактирования
3. **Сохраняйте порядок** только при выходе из режима редактирования
4. **Используйте `detectReorder`** для ручки перетаскивания
5. **Автоматически включайте режим редактирования** при начале перетаскивания через `LaunchedEffect(isDragging)`

## Известные ограничения

1. Перетаскивание работает только в пределах одного `ReorderableLazyColumn`
2. Для перетаскивания между группами требуется дополнительная логика (как в `ClientsScreen`)
3. Архивные элементы не могут быть перетащены
4. Изменения сохраняются только при выходе из режима редактирования




