
# Wassertech CRM — Code Audit (Branch: `dinamic-tamplates`) & Handover 4 Plan

**Status snapshot (read from ZIP):**
- App package: `com.example.wassertech`
- Stack: AGP 8.7.2, Kotlin 1.9.25, Compose BOM 2024.10.01, Compose Compiler 1.5.15, Room 2.6.1, KSP 1.9.25-1.0.20
- DB schemas present: versions **1..5** (`app/schemas/com.example.wassertech.data.AppDatabase`)
- `AppDatabase.kt` (redacted in ZIP printout, but schema shows **version 5**)
- Entities present:
  - Core: `ClientEntity`, `SiteEntity`, `InstallationEntity`, `ComponentEntity`
  - Maintenance: `MaintenanceSessionEntity`, `ChecklistTemplateEntity`, `ChecklistFieldEntity`, `IssueEntity`, `ObservationEntity`
- DAOs present: `HierarchyDao`, `SessionsDao`, **`TemplatesDao` (for maintenance checklist templates & fields)**.
- UI: `ComponentsScreen`, `MaintenanceAllScreen`, `ClientsScreen`, `Sites/Installations` screens.
- ViewModels: `HierarchyViewModel`, `MaintenanceViewModel`, `ClientsViewModel`.
- `Converters.kt` contains converters for `ComponentType`, `FieldType`, `Severity`.
- `TemplateSeeder` seeds **checklist templates** per `ComponentType` (not component templates).

**Important conclusion:** текущая ветка уже реализует **динамические шаблоны ЧЕК-ЛИСТОВ ТО** (ChecklistTemplate/Field) на каждый `ComponentType`, но **компоненты установок до сих пор опираются на enum `ComponentType`** и **не используют компонентные шаблоны**.

---

## Что уже готово и ценно для Handover 4
- База и UI для ведения **шаблонов чек-листа обслуживания**: гибкие поля (`TEXT/NUMBER/CHECKBOX`) и их сидирование по типам компонентов.
- Экран MaintenanceAll оперирует группировкой по компонентам и может использовать поля шаблонов для рендера UI (по коду видны заготовки).
- Иерархия клиент→объект→установка→компоненты отлажена (CRUD, упорядочивание, диалоги).

Это всё — отличная база для следующего шага: **ввести редактируемые _компонентные_ шаблоны**, чтобы отказаться от жёсткого enum при добавлении компонентов к установкам.

---

## Предлагаемый дизайн для компонентных шаблонов (минимально-инвазивный)

### 1) Новая сущность: `ComponentTemplateEntity` (НЕ путать с ChecklistTemplateEntity)
Создаём ПАРАЛЛЕЛЬНУЮ ветку шаблонов именно для компонентов установки:

```kotlin
// app/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt
package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "component_templates")
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,              // "Filter / RO / Softener / Valve / Other"
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val defaultParamsJson: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)
```

> Почему отдельная сущность: чтобы не смешивать домены — **чек-листы ТО** (TemplatesDao) и **типы оборудования**.

### 2) Новое DAO: `ComponentTemplatesDao`
```kotlin
// app/src/main/java/com/example/wassertech/data/dao/ComponentTemplatesDao.kt
package com.example.wassertech.data.dao

import androidx.room.*
import com.example.wassertech.data.entities.ComponentTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentTemplatesDao {
    @Query("SELECT * FROM component_templates WHERE isArchived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<ComponentTemplateEntity>>

    @Query("SELECT * FROM component_templates ORDER BY isArchived, sortOrder, name")
    fun observeAll(): Flow<List<ComponentTemplateEntity>>

    @Query("SELECT * FROM component_templates WHERE id = :id")
    suspend fun getById(id: String): ComponentTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ComponentTemplateEntity)

    @Update suspend fun update(item: ComponentTemplateEntity)

    @Query("UPDATE component_templates SET isArchived = :arch WHERE id = :id")
    suspend fun setArchived(id: String, arch: Boolean)

    @Query("UPDATE component_templates SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Delete suspend fun delete(item: ComponentTemplateEntity)
}
```

### 3) Расширение `ComponentEntity` (мягко)
Добавляем связь с шаблоном **без разрушения обратной совместимости**:

```kotlin
// app/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: com.example.wassertech.data.types.ComponentType, // оставить (compat)
    val orderIndex: Int = 0,

    // NEW:
    val templateId: String? = null,      // ссылка на component_templates.id
    val paramsJson: String? = null,      // instance-override параметров
    val nameOverride: String? = null     // локальное переименование, если нужно
)
```

### 4) Миграция БД **5 → 6**
- Создать таблицу `component_templates`.
- Добавить колонки `templateId`, `paramsJson`, `nameOverride` в `components`.
- Индекс на `components(templateId)` (FK можно отложить и держать логическим).
- Seed: перенести часть текущего enum в базовые компонентные шаблоны (по необходимости).
- Backfill: попытаться связать существующие `components` c шаблонами по имени/типу.

> Миграцию делаем через `Migration(5, 6)` + транзакцию. Схему сохранить в `/schemas`.

### 5) Репозиторий и VM
- `ComponentTemplatesRepository` (инкапсулирует `ComponentTemplatesDao` + seed).
- `HierarchyViewModel`: метод добавления компонента получает `templateId` и копирует `defaultParamsJson → paramsJson`.
- Новый `TemplatesViewModel` **для компонентных шаблонов** и экран управления ими.

### 6) UI
- Экран **“Шаблоны компонентов”** (отдельный от чеклистов): список, поиск, архив/восстановление, сортировка, FAB “+ шаблон”.
- Диалог “+ Компонент” в `ComponentsScreen`: вместо enum — поиск по шаблонам, кнопка “создать шаблон” из диалога.

---

## Быстрые проверки/замечания по текущему коду (по ZIP)

1) **`MaintenanceViewModel`** — `saveSession(...)` ещё TODO. План: сохранить snapshot полей и состояния компонентов, связать с `installationId`/`siteId`.
2) **`TemplatesDao`** имена конфликтны по смыслу. Рекомендую переименовать в `ChecklistTemplatesDao` для ясности (миграций это не касается).
3) **Схемы 1..5** присутствуют, но `AppDatabase.kt` выводился редактированным. В любом случае, версию поднимем до **6**.
4) **UI ComponentsScreen** — диалоги и reorder уже есть; вставка выбора шаблона — точка встраивания очевидна (диалог добавления).
5) **Seed чеклистов** уже есть (per `ComponentType`). Для компонентных шаблонов можно добавить отдельный `ComponentTemplatesSeeder`.

---

## Мини-патчи (готовые вставки)

### A) Добавить сущность ComponentTemplateEntity
`app/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt` — см. блок кода выше.

### B) Добавить DAO ComponentTemplatesDao
`app/src/main/java/com/example/wassertech/data/dao/ComponentTemplatesDao.kt` — см. блок кода выше.

### C) Расширить ComponentEntity
Добавить поля `templateId`, `paramsJson`, `nameOverride` (см. блок выше).

### D) AppDatabase.kt — обновить
- `@Database(…, version = 6, exportSchema = true)`
- Включить новую сущность в список.
- Добавить `abstract fun componentTemplatesDao(): ComponentTemplatesDao`
- Добавить `MIGRATION_5_6` с SQL:
  ```sql
  CREATE TABLE IF NOT EXISTS component_templates (... как в сущности ...);
  ALTER TABLE components ADD COLUMN templateId TEXT;
  ALTER TABLE components ADD COLUMN paramsJson TEXT;
  ALTER TABLE components ADD COLUMN nameOverride TEXT;
  CREATE INDEX IF NOT EXISTS index_components_templateId ON components(templateId);
  ```
- (Опционально) seed в `RoomDatabase.Callback.onOpen` при пустой таблице.

### E) Диалог “+ Компонент”
- Заменить enum-селектор на поиск по `ComponentTemplatesDao.observeActive()`.
- Кнопка “Создать шаблон” → модалка → upsert → сразу выбрать созданный шаблон.

---

## Definition of Done для Handover 4
- Проект мигрирован до **DB v6**, схемы обновлены.
- Экран “Шаблоны компонентов” доступен из меню/навигации.
- Добавление компонента работает **через шаблоны** (enum в UI больше не используется).
- Компоненты, созданные ранее, не ломаются; новые пишут `templateId` и `paramsJson`.
- Регрессия и reorder по всем уровням — зелёные.
- (Опционально) Переименование `TemplatesDao` → `ChecklistTemplatesDao`.

---

## Что потребуется от тебя
- Подтверди, ок ли поднять версию БД до **6** на этой ветке.
- Если есть желаемый стартовый набор **компонентных шаблонов** — пришли списком (имя, категория, ключевые параметры). Если нет — соберу первичный seed из текущего enum и практики.

