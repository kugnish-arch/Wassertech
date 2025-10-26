# Wassertech CRM — оффлайн-приложение для ТО систем водоподготовки

> Состояние: Handover #3 после стабилизации сборки (soft-delete на уровне модели готов, UI в работе).

## 1) Технический стек
- **Package**: `com.example.wassertech`
- **Min SDK**: 26
- **Target/Compile**: 36
- **JDK**: 21
- **Gradle wrapper**: 8.13
- **AGP**: 8.7.2
- **Kotlin**: 1.9.25
- **Compose Compiler**: 1.5.15
- **Compose BOM**: 2024.10.01
- **Room**: 2.6.1
- **KSP**: 1.9.25-1.0.20

> Все версии зафиксированы согласно текущей сборке. При обновлении — сверять совместимость Compose/Kotlin.
## 2) Структура проекта (основные файлы)
**Сущности (Room entities):**
- `app/src/main/java/com/example/wassertech/data/entities/ChecklistFieldEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/ChecklistTemplateEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/ClientEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/InstallationEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/IssueEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/MaintenanceSessionEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/ObservationEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/SiteEntity.kt`
**DAO:**
- `app/src/main/java/com/example/wassertech/data/dao/ArchiveDao.kt`
- `app/src/main/java/com/example/wassertech/data/dao/HierarchyDao.kt`
- `app/src/main/java/com/example/wassertech/data/dao/SessionsDao.kt`
- `app/src/main/java/com/example/wassertech/data/dao/TemplatesDao.kt`
**ViewModel:**
- `app/src/main/java/com/example/wassertech/viewmodel/ClientsViewModel.kt`
- `app/src/main/java/com/example/wassertech/viewmodel/HierarchyViewModel.kt`
- `app/src/main/java/com/example/wassertech/viewmodel/MaintenanceViewModel.kt`
**Экраны (Compose Screens):**
- `app/src/main/java/com/example/wassertech/ui/clients/ClientDetailScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/clients/ClientsScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/empty/EmptyScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/hierarchy/ComponentsScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/hierarchy/InstallationsScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/hierarchy/SiteDetailScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/hierarchy/SitesScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/maintenance/MaintenanceAllScreen.kt`
- `app/src/main/java/com/example/wassertech/ui/maintenance/MaintenanceScreen.kt`
**Конвертеры:**
- `app/src/main/java/com/example/wassertech/data/Converters.kt`
- `app/src/main/java/com/example/wassertech/data/types/Converters.kt`
- `app/src/main/java/com/example/wassertech/data/Converters.kt`
- `app/src/main/java/com/example/wassertech/data/types/Converters.kt`
**База данных:**
- `app/src/main/java/com/example/wassertech/data/AppDatabase.kt`
**Сидеры:**
- `app/src/main/java/com/example/wassertech/data/TemplateSeeder.kt`
- `app/src/main/java/com/example/wassertech/data/seed/TemplateSeeder.kt`
- `app/src/main/java/com/example/wassertech/data/TemplateSeeder.kt`
- `app/src/main/java/com/example/wassertech/data/seed/TemplateSeeder.kt`

## 3) Архитектура данных (Room v3)
- Иерархия: **Клиенты → Объекты (Sites) → Установки (Installations) → Компоненты (Components)**.
- Шаблоны чек-листов и поля: `ChecklistTemplateEntity`, `ChecklistFieldEntity` (типы: `FieldType`).
- ТО (сессии): `MaintenanceSessionEntity`, `ObservationEntity`, `IssueEntity`.
- Конвертеры: `Converters` — enum хранятся как `String` (надёжнее для миграций).
- `@Database(version = 3, exportSchema = true)`; зарегистрирована `MIGRATION_2_3`.

## 4) Soft-delete (текущее состояние)
- Поля в `ClientEntity`: `isArchived:Boolean=false`, `archivedAtEpoch:Long?`.
- DAO-выборки клиентов скрывают архив по умолчанию.
- **Осталось**: UI-тумблер «Показывать архив», действия «Архивировать/Восстановить», бейдж в карточке, тесты DAO.

## 5) DAO / ViewModel / UI
- `HierarchyDao` / `TemplatesDao` синхронизированы с текущими экранами; для reorder есть перегрузки по спискам ID.
- `HierarchyViewModel` предоставляет потоки для clients/sites/installations/components и CRUD-обёртки.
- `MaintenanceViewModel` содержит перегрузки `saveSession(...)` — **пока заглушки** (реализация через `SessionsDao.saveSessionBundle`).

## 6) Ближайшие задачи (чек-лист)
- [ ] UI soft-delete: тумблер «Показывать архив», кнопка «Архивировать/Восстановить», бейдж «Архив».
- [ ] Реализовать `SessionsDao.saveSessionBundle(...)` и провязать `MaintenanceViewModel.saveSession(...)`.
- [ ] «История ТО»: экран списка сессий и деталка (observations + issues).
- [ ] Экспорт PDF отчёта о сессии + `share` (минимальная реализация).
- [ ] Валидация NUMBER + единый форматер (локаль, unit, min/max, подсветка ошибок).
- [ ] Room v4 миграция (+ тест миграции).

## 7) Онбординг/сборка
1. Клонируйте репозиторий; JDK 21.
2. Откройте проект в Android Studio (Koala/Jellyfish ok); **не обновляйте** версии плагинов без необходимости.
3. Build → **Clean Project** → **Assemble Project**.
4. Первый запуск — `TemplateSeeder` создаст базовые шаблоны.

> При проблемах с KSP/Room: выполните `Clean`, удалите локальные `app/build`, `.gradle`, `.kotlin`, затем пересоберите.
## 8) Ветвление и PR
- Фичи: `feature/<short-name>`; багфиксы: `fix/<short-name>`.
- В PR указывать: суть изменений, как тестировать, нужны ли миграции/сидер.
- Для любой правки Room — обязательна миграция и тест миграции.

## 9) Риски и примечания
- Дубли enum/типов ломают KSP/Room — держим их только в `data/types`.
- `AppDatabase.getInstance(...)` не путать с устаревшим `get(...)`.
- В UI встречаются вызовы VM со специфичными перегрузками; поддерживаем обратную совместимость, но в долгосрочной перспективе унифицируем сигнатуры.
