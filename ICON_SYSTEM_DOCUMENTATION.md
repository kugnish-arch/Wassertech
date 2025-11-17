# Документация: Система иконок и икон-паков

## Обзор

Система иконок в приложении Wassertech позволяет использовать иконки для визуального обозначения объектов (Sites), установок (Installations) и компонентов (Components). Иконки могут быть:
1. **Встроенными ресурсами Android** (`androidResName`) - хранятся в `res/drawable/`
2. **Загружаемыми с сервера** (`imageUrl`) - загружаются и сохраняются локально в файловой системе

## Архитектура (после рефакторинга)

Система иконок организована в несколько слоёв:

- **`core:ui.icons`** - общие интерфейсы и модели:
  - `IconDataSource` - интерфейс для работы с иконками
  - `IconPickerUiState` - состояние для диалога выбора иконок
  - `IconEntityType` - типы сущностей
  - `IconResolver` - разрешение и отображение иконок

- **`core:ui.components`** - UI-компоненты:
  - `IconPickerDialog` - диалог выбора иконок
  - `IconGrid`, `IconPackCard`, `IconPackBadgeRow` - вспомогательные компоненты

- **`app-crm/data/repository/IconRepository`** и **`app-client/data/repository/IconRepository`** - реализации `IconDataSource` для каждого приложения

## Архитектура системы

### 1. Структура данных

#### База данных (Room)

**Таблица `icon_packs`** (`IconPackEntity`):
- `id` (String, PK) - уникальный идентификатор пака
- `code` (String) - код пака
- `name` (String) - название пака
- `description` (String?) - описание пака
- `isBuiltin` (Boolean) - встроенный ли пак
- `isPremium` (Boolean) - премиум ли пак
- `origin` (String?) - источник: "CRM" или "CLIENT"
- `createdByUserId` (String?) - ID пользователя-создателя
- `createdAtEpoch` (Long) - время создания
- `updatedAtEpoch` (Long) - время обновления

**Таблица `icons`** (`IconEntity`):
- `id` (String, PK) - уникальный идентификатор иконки
- `packId` (String, FK → icon_packs.id) - ID пака, к которому относится иконка
- `code` (String) - код иконки (уникальный в рамках пака)
- `label` (String) - название иконки для отображения
- `entityType` (String) - тип сущности: "SITE", "INSTALLATION", "COMPONENT", "ANY"
- `imageUrl` (String?) - URL изображения на сервере (для загрузки)
- `thumbnailUrl` (String?) - URL миниатюры
- `androidResName` (String?) - имя ресурса Android (например, "ic_site_default")
- `isActive` (Boolean) - активна ли иконка
- `origin` (String?) - источник: "CRM" или "CLIENT"
- `createdByUserId` (String?) - ID пользователя-создателя
- `createdAtEpoch` (Long) - время создания
- `updatedAtEpoch` (Long) - время обновления

**Таблица `icon_pack_sync_status`** (`IconPackSyncStatusEntity`):
- `packId` (String, PK) - ID пака
- `lastSyncEpoch` (Long) - время последней синхронизации
- `isDownloaded` (Boolean) - загружены ли изображения локально
- `totalIcons` (Int) - общее количество иконок в паке
- `downloadedIcons` (Int) - количество загруженных иконок

### 2. Синхронизация с сервером

#### Процесс синхронизации

1. **Запрос данных** (`SyncEngine.processPullResponse()`):
   - При синхронизации запрашиваются `icon_packs` и `icons` через API `/api/public/sync/pull`
   - Сервер возвращает `SyncPullResponse` с массивами `iconPacks` и `icons`

2. **Маппинг DTO → Entity**:
   - `SyncIconPackDto` → `IconPackEntity`
   - `SyncIconDto` → `IconEntity`
   - Данные сохраняются в Room БД по принципу last-write-wins

3. **Структура DTO**:
   ```kotlin
   SyncIconPackDto:
   - id, code, name, description
   - isBuiltin, isPremium
   - origin, createdByUserId
   - createdAtEpoch, updatedAtEpoch
   
   SyncIconDto:
   - id, packId, code, label
   - entityType, imageUrl, thumbnailUrl
   - androidResName, isActive
   - origin, createdByUserId
   - createdAtEpoch, updatedAtEpoch
   ```

### 3. Загрузка изображений с сервера

#### IconDataSource (интерфейс)

**Интерфейс**: `ru.wassertech.core.ui.icons.IconDataSource`

Определяет контракт для работы с иконками:
- `getLocalIconPath(iconId: String)` - получить локальный путь к изображению
- `isIconDownloaded(iconId: String)` - проверить, загружена ли иконка
- `downloadIconImage(iconId: String, imageUrl: String)` - загрузить изображение
- `downloadPackImages(packId: String, onProgress)` - загрузить все изображения пака
- `getPackSyncStatus(packId: String)` - получить статус синхронизации пака
- `clearPackImages(packId: String)` - удалить локальные файлы пака

#### IconRepository (реализация)

**Класс**: `ru.wassertech.data.repository.IconRepository` (app-crm) или `ru.wassertech.client.data.repository.IconRepository` (app-client)

Реализует интерфейс `IconDataSource` для конкретного приложения.

**Директория хранения**:
- Путь: `context.filesDir/icons/`
- Формат файла: `{iconId}_image.png`
- Пример: `/data/data/ru.wassertech.crm/files/icons/1d662fab-e711-4af0-8690-eb430f6d1e59_image.png`

**Основные методы (через интерфейс IconDataSource)**:

1. **`getLocalIconPath(iconId: String)`** - возвращает абсолютный путь к локальному файлу, если он существует, иначе `null`

2. **`isIconDownloaded(iconId: String)`** - проверяет, загружена ли иконка локально

3. **`downloadIconImage(iconId: String, imageUrl: String)`** - загружает изображение с сервера:
   - Исправляет URL (заменяет `/publicuploads/` на `/uploads/`)
   - Если URL относительный, добавляет базовый URL из `ApiConfig`
   - Использует OkHttp с токеном авторизации
   - Сохраняет файл в `{iconId}_image.png`

4. **`downloadPackImages(packId: String, onProgress: ((Int, Int) -> Unit)?)`** - загружает все изображения из пака:
   - Пропускает иконки без `imageUrl`
   - Пропускает иконки с `androidResName` (встроенные ресурсы)
   - Пропускает уже загруженные иконки
   - Обновляет статус синхронизации в `icon_pack_sync_status` (только в app-crm)

**Внутренние методы (для обратной совместимости)**:

- `getIconsDirectory()` - получает директорию для хранения иконок
- `getIconFile(iconId: String, type: String)` - получает объект `File` для иконки
- `getLocalIconPath(icon: IconEntity)` - @deprecated, используйте `getLocalIconPath(iconId: String)`
- `downloadIconImage(icon: IconEntity)` - @deprecated, используйте `downloadIconImage(iconId: String, imageUrl: String)`

**Логика загрузки**:
```kotlin
// Пропускаем иконки без imageUrl или с androidResName
if (icon.imageUrl.isNullOrBlank() || !icon.androidResName.isNullOrBlank()) {
    return // Пропуск
}

// Пропускаем уже загруженные
if (isIconDownloaded(icon.id)) {
    return // Пропуск
}

// Загружаем
downloadIconImage(icon)
```

### 4. Отображение иконок в UI

#### IconResolver

**Объект**: `ru.wassertech.core.ui.icons.IconResolver`

**Метод `IconImage`** - Composable функция для отображения иконки:

```kotlin
@Composable
fun IconImage(
    androidResName: String?,
    entityType: IconEntityType,
    imageUrl: String? = null,
    localImagePath: String? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    code: String? = null
)
```

**Приоритет отображения**:
1. **Локальный файл** (`localImagePath`) - если файл существует, загружается через `BitmapFactory.decodeFile()`
2. **Android ресурс** (`androidResName`) - если задан, ищется через `context.resources.getIdentifier()`
3. **Fallback по коду** (`code`) - если `androidResName` отсутствует, пробуются варианты:
   - `code.lowercase().replace(" ", "_").replace("-", "_")`
   - `code.lowercase().replace(" ", "").replace("-", "")`
   - `ic_${code.lowercase().replace(" ", "_").replace("-", "_")}`
   - `icon_${code.lowercase().replace(" ", "_").replace("-", "_")}`
4. **Дефолтная иконка** - по типу сущности:
   - `SITE` → `R.drawable.object_house_blue`
   - `INSTALLATION` → `R.drawable.installation`
   - `COMPONENT` → `R.drawable.ui_gear`
   - `ANY` → `R.drawable.ui_gear`

### 5. Использование в UI компонентах

#### IconPickerDialog

**Компонент**: `ru.wassertech.core.ui.components.IconPickerDialog`

**Данные**: `IconUiData`:
```kotlin
data class IconUiData(
    val id: String,
    val packId: String,
    val label: String,
    val entityType: String,
    val androidResName: String?,
    val code: String? = null,
    val localImagePath: String? = null
)
```

**Создание `IconPickerUiState`** (в `HierarchyViewModel.loadIconPacksAndIconsFor()` или аналогичных методах):
```kotlin
val packs = iconPackDao.getAll()
val allIcons = iconDao.getAllActive()
val filteredIcons = allIcons.filter { icon ->
    icon.entityType == "ANY" || icon.entityType == entityType.name
}
val iconsByPack = filteredIcons.groupBy { it.packId }

IconPickerUiState(
    packs = packs.map { 
        IconPackUiData(id = it.id, name = it.name)
    },
    iconsByPack = iconsByPack.mapValues { (_, icons) ->
        icons.map { icon ->
            val localPath = iconRepository.getLocalIconPath(icon.id)
            IconUiData(
                id = icon.id,
                packId = icon.packId,
                label = icon.label,
                entityType = icon.entityType,
                androidResName = icon.androidResName,
                code = icon.code,
                localImagePath = localPath
            )
        }
    }
)
```

**Отображение в диалоге**:
```kotlin
IconResolver.IconImage(
    androidResName = icon.androidResName,
    entityType = iconEntityType,
    contentDescription = icon.label,
    size = 48.dp,
    code = icon.code,
    localImagePath = icon.localImagePath
)
```

### 6. Проблема с отображением иконок

#### Текущая проблема

По логам видно, что:
1. **`androidResName` равен `null`** для всех иконок в БД
2. **Fallback по `code` не находит ресурсы** (например, `SITE_FACTORY`, `RO-3x40`, `SOFTENER`)
3. **Используется дефолтная иконка** (`R.drawable.ui_gear`)

#### Причины проблемы

1. **Сервер не отправляет `androidResName`** - поле `android_res_name` в `SyncIconDto` может быть `null`
2. **Ресурсы не существуют в `res/drawable/`** - иконки с кодами `SITE_FACTORY`, `RO-3x40` и т.д. не добавлены в проект как drawable ресурсы
3. **Изображения не загружены локально** - если `imageUrl` задан, но изображения не были загружены через `IconRepository.downloadPackImages()`

#### Решения

**Вариант 1: Загрузить изображения с сервера**
- Если у иконок есть `imageUrl`, нужно вызвать `IconRepository.downloadPackImages(packId)`
- После загрузки `localImagePath` будет доступен и иконки отобразятся

**Вариант 2: Добавить ресурсы в проект**
- Создать drawable ресурсы с именами, соответствующими `code` (например, `ic_site_factory.xml`)
- Или заполнить `androidResName` на сервере для каждой иконки

**Вариант 3: Использовать `imageUrl` напрямую**
- Загружать изображения по требованию через библиотеку типа Coil или Glide
- Требует изменения архитектуры `IconResolver`

### 7. Где должны находиться ресурсы

#### Встроенные ресурсы Android

**Путь**: `app-crm/src/main/res/drawable/` или `core/ui/src/main/res/drawable/`

**Имена файлов**: должны соответствовать `androidResName` из БД или вариантам `code`

**Примеры**:
- `ic_site_factory.xml` или `ic_site_factory.png`
- `ic_ro_3x40.xml` или `ic_ro_3x40.png`
- `ic_softener.xml` или `ic_softener.png`

#### Загружаемые изображения

**Путь**: `context.filesDir/icons/`

**Формат**: `{iconId}_image.png`

**Пример**: `/data/data/ru.wassertech.crm/files/icons/1d662fab-e711-4af0-8690-eb430f6d1e59_image.png`

### 8. Рекомендации по исправлению

1. **Проверить данные на сервере**:
   - Убедиться, что `imageUrl` заполнен для иконок
   - Проверить, что URL доступны и возвращают изображения

2. **Загрузить изображения**:
   - Вызвать `IconRepository.downloadPackImages(packId)` для каждого пака
   - Проверить логи загрузки

3. **Проверить локальные файлы**:
   - Проверить наличие файлов в `context.filesDir/icons/`
   - Убедиться, что `getLocalIconPath()` возвращает правильные пути

4. **Добавить логирование**:
   - Уже добавлено логирование в `IconResolver` и `IconPickerDialog`
   - Проверить логи при открытии диалога выбора иконок

5. **Альтернативное решение**:
   - Если изображения не загружаются, можно использовать библиотеку Coil для загрузки по `imageUrl` напрямую
   - Требует изменения `IconResolver.IconImage()`

## Использование в приложениях

### В app-crm:

```kotlin
// В ViewModel
val iconRepository = IconRepository(context) // реализует IconDataSource

suspend fun loadIconPacksAndIconsFor(entityType: IconEntityType): IconPickerUiState {
    val packs = iconPackDao.getAll()
    val allIcons = iconDao.getAllActive()
    val filteredIcons = allIcons.filter { icon ->
        icon.entityType == "ANY" || icon.entityType == entityType.name
    }
    val iconsByPack = filteredIcons.groupBy { it.packId }
    return IconPickerUiState(
        packs = packs.map { IconPackUiData(id = it.id, name = it.name) },
        iconsByPack = iconsByPack.mapValues { (_, icons) ->
            icons.map { icon ->
                val localPath = iconRepository.getLocalIconPath(icon.id)
                IconUiData(..., localImagePath = localPath)
            }
        }
    )
}
```

### В app-client:

Аналогично app-crm, но использует `ru.wassertech.client.data.repository.IconRepository`.

## Заключение

Система иконок поддерживает два способа отображения:
1. **Встроенные ресурсы** - через `androidResName` (требуют добавления в проект)
2. **Загружаемые изображения** - через `imageUrl` (требуют загрузки через `IconRepository`)

**Архитектура после рефакторинга:**
- ✅ Общие интерфейсы и модели в `core:ui.icons`
- ✅ Реализации в app-модулях через `IconDataSource`
- ✅ Единый API для обоих приложений
- ✅ Обратная совместимость сохранена

**Для загрузки изображений:** Используйте `IconRepository.downloadPackImages(packId)` или добавьте drawable ресурсы в проект.

