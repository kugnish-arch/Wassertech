# Подготовка сущностей Room к оффлайн-синхронизации

**Дата:** 2024  
**Версия БД:** 8 → 9  
**Миграция:** `MIGRATION_8_9`

---

## Обзор изменений

Все синхронизируемые сущности Room в Wassertech CRM были унифицированы для поддержки оффлайн-синхронизации через `/sync/push` и `/sync/pull` эндпоинты.

### Синхронизируемые сущности

1. `ClientEntity`
2. `ClientGroupEntity`
3. `SiteEntity`
4. `InstallationEntity`
5. `ComponentEntity`
6. `ComponentTemplateEntity`
7. `ChecklistTemplateEntity`
8. `ChecklistFieldEntity`
9. `MaintenanceSessionEntity`
10. `MaintenanceValueEntity`

**Примечание:** `DeletedRecordEntity` остаётся без изменений — это отдельный механизм логирования удалений.

---

## Добавленные поля

### 1. Поля синхронизации (отправляются на сервер)

#### Временные метки:
- `createdAtEpoch: Long` — момент создания записи (по умолчанию 0, если данных нет)
- `updatedAtEpoch: Long` — момент последнего изменения (по умолчанию 0, если данных нет)

#### Архивирование:
- `isArchived: Boolean` — признак архивирования (по умолчанию `false`)
- `archivedAtEpoch: Long?` — момент архивирования (nullable)

#### Логическое удаление:
- `deletedAtEpoch: Long?` — момент логического удаления (nullable)

### 2. Локальные поля для оффлайн-очереди (НЕ отправляются на сервер)

- `dirtyFlag: Boolean` — флаг локальных изменений (`true` = запись изменена локально и должна попасть в `/sync/push`)
- `syncStatus: Int` — статус синхронизации:
  - `0` = `SYNCED` — синхронизировано
  - `1` = `QUEUED` — в очереди на отправку
  - `2` = `CONFLICT` — обнаружен конфликт при синхронизации

---

## Интерфейс SyncMetaEntity

Создан интерфейс `SyncMetaEntity` как контракт для всех синхронизируемых сущностей:

```kotlin
interface SyncMetaEntity {
    var createdAtEpoch: Long
    var updatedAtEpoch: Long
    var isArchived: Boolean
    var archivedAtEpoch: Long?
    var deletedAtEpoch: Long?
    var dirtyFlag: Boolean
    var syncStatus: Int
}
```

**Примечание:** Из-за ограничений Room (data class не поддерживает var с override), интерфейс используется как документация/контракт. Все сущности имеют все необходимые поля, но не реализуют интерфейс явно.

---

## Enum SyncStatus

Создан enum `SyncStatus` для типобезопасной работы со статусами синхронизации:

```kotlin
enum class SyncStatus(val value: Int) {
    SYNCED(0),
    QUEUED(1),
    CONFLICT(2);
}
```

Добавлен `TypeConverter` в `Converters` для конвертации между `SyncStatus` и `Int` в Room.

---

## Миграция MIGRATION_8_9

Миграция добавляет все недостающие поля во все синхронизируемые таблицы с разумными DEFAULT-значениями:

- Для существующих записей все новые поля получают значения по умолчанию
- Созданы индексы для `dirtyFlag` и `syncStatus` для быстрого поиска "грязных" записей
- Все изменения обратно совместимы — существующие данные не затрагиваются

### Добавленные индексы

Для каждой таблицы созданы индексы:
- `index_<table>_dirtyFlag` — для быстрого поиска записей, требующих синхронизации
- `index_<table>_syncStatus` — для фильтрации по статусу синхронизации

---

## Обновлённые DAO

Во все DAO добавлены методы для работы с синхронизацией:

### Методы получения "грязных" записей:
- `getDirtyClientsNow(): List<ClientEntity>`
- `getDirtyGroupsNow(): List<ClientGroupEntity>`
- `getDirtySitesNow(): List<SiteEntity>`
- `getDirtyInstallationsNow(): List<InstallationEntity>`
- `getDirtyComponentsNow(): List<ComponentEntity>`
- `getDirtyComponentTemplatesNow(): List<ComponentTemplateEntity>`
- `getDirtyTemplatesNow(): List<ChecklistTemplateEntity>`
- `getDirtyFieldsNow(): List<ChecklistFieldEntity>`
- `getDirtySessionsNow(): List<MaintenanceSessionEntity>`
- `getDirtyValuesNow(): List<MaintenanceValueEntity>`

### Методы обновления статусов синхронизации:

Для каждой сущности добавлены методы:
- `mark*AsSynced(ids: List<String>)` — пометить как синхронизированные (`dirtyFlag = false`, `syncStatus = 0`)
- `mark*AsConflict(ids: List<String>)` — пометить как конфликтные (`dirtyFlag = false`, `syncStatus = 2`)
- `clear*DirtyFlag(ids: List<String>)` — снять флаг "грязный" (`dirtyFlag = false`)

---

## Обновлённые файлы

### Новые файлы:
- `app-crm/src/main/java/com/example/wassertech/data/types/SyncStatus.kt`
- `app-crm/src/main/java/com/example/wassertech/data/entities/SyncMetaEntity.kt`
- `app-crm/src/main/java/com/example/wassertech/data/migrations/MIGRATION_8_9.kt`

### Обновлённые файлы:
- `app-crm/src/main/java/com/example/wassertech/data/Converters.kt` — добавлен TypeConverter для SyncStatus
- `app-crm/src/main/java/com/example/wassertech/data/AppDatabase.kt` — версия 8 → 9, добавлена миграция
- Все сущности в `app-crm/src/main/java/com/example/wassertech/data/entities/`:
  - `ClientEntity.kt`
  - `ClientGroupEntity.kt`
  - `SiteEntity.kt`
  - `InstallationEntity.kt`
  - `ComponentEntity.kt`
  - `ComponentTemplateEntity.kt`
  - `ChecklistTemplateEntity.kt`
  - `ChecklistFieldEntity.kt`
  - `MaintenanceSessionEntity.kt`
  - `MaintananceValueEntity.kt`
- Все DAO в `app-crm/src/main/java/com/example/wassertech/data/dao/`:
  - `ClientDao.kt`
  - `HierarchyDao.kt`
  - `SessionsDao.kt`
  - `TemplatesDao.kt`
  - `ComponentTemplatesDao.kt`

---

## Использование

### Пример: пометить запись как изменённую

```kotlin
// При изменении клиента
val updatedClient = client.copy(
    name = "Новое имя",
    updatedAtEpoch = System.currentTimeMillis(),
    dirtyFlag = true,
    syncStatus = SyncStatus.QUEUED.value
)
clientDao.upsertClient(updatedClient)
```

### Пример: получить все записи для синхронизации

```kotlin
val dirtyClients = clientDao.getDirtyClientsNow()
val dirtySites = hierarchyDao.getDirtySitesNow()
// ... и т.д.
```

### Пример: пометить записи как синхронизированные

```kotlin
val syncedIds = listOf("id1", "id2", "id3")
clientDao.markClientsAsSynced(syncedIds)
```

---

## Следующие шаги

1. **Создать SyncEngine** — сервис для синхронизации через `/sync/push` и `/sync/pull`
2. **Автоматически помечать записи как "грязные"** — при любых изменениях через DAO
3. **Фоновая синхронизация** — периодическая проверка и отправка изменений
4. **Обработка конфликтов** — логика разрешения конфликтов при синхронизации

---

## Примечания

- Все новые поля имеют разумные значения по умолчанию, чтобы не сломать существующие данные
- Миграция безопасна — не удаляет и не изменяет существующие данные
- Поля `dirtyFlag` и `syncStatus` являются локальными и не должны отправляться на сервер
- Поле `synced` в `MaintenanceSessionEntity` оставлено для обратной совместимости, но рекомендуется использовать `syncStatus`


