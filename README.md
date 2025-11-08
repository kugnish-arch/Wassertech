# Wassertech CRM (Android)

**Текущая сборка:** 0.7.1.x · Room schema v7 · Compile SDK 36 · Kotlin 1.9 · Compose BOM 2024.10.01

---

## Назначение и контекст
Мобильное офлайн-приложение для инженеров Wassertech, обслуживающих системы водоподготовки. Приложение обеспечивает полный цикл полевого обслуживания: ведение базы клиентов и объектов, фиксацию структуры установок и компонентов, проведение регламентных работ, учёт результатов ТО и формирование отчётности в PDF/DOCX.

## Статус проекта
- Реализована цепочка «Клиенты → Объекты → Установки → Компоненты → ТО → История → Отчёты».
- Интерфейс переведён на Jetpack Compose (Material 3, адаптивные секции и режим редактирования).
- Room-база обновлена до версии 7, миграции 1→6 готовы, переход 6→7 (ComponentType) оформлен в коде и требует финальной реализации.
- Поддержано формирование отчётов (HTML, DOCX, PDF) и просмотр готовых файлов внутри приложения.
- Экспериментальная синхронизация с удалённой MySQL-БД доступна из раздела «Настройки», ведётся работа над безопасностью и устойчивостью.

## Технологический стек
| Слой | Технологии | Назначение |
|---|---|---|
| UI | Jetpack Compose · Material 3 · Navigation Compose | Экранный слой, single-activity архитектура, верхний бар с меню, реактивные списки |
| State | ViewModel · StateFlow · CoroutineScope | Управление состояниями, сохранение порядков, асинхронные операции |
| Data | Room 2.6.1 · KSP | Локальные сущности, DAO, миграции и снапшоты схем (`app/schemas`) |
| Reports | Apache POI · HTML шаблоны · WebView PDF | Генерация отчётов на основе `ReportDTO`, экспорт в PDF и DOCX |
| Sync | MySQL JDBC 5.1.49 · SafeDeletionHelper · DeletionTracker | Экспериментальный обмен данными с удалённой БД и фиксация удалений |
| Build | Gradle 8 · Kotlin DSL · version.properties | Автоинкремент `versionCode`, Compose compiler 1.5.15, JVM target 21 |

## Архитектура и ключевые модули
### Пакеты приложения (`app/src/main/java/com/example/wassertech`)
| Каталог | Что внутри | Комментарии |
|---|---|---|
| `data/` | `AppDatabase`, DAO, сущности, миграции, `TemplateSeeder`, типы | Центр данных, подготовка схемы, экспорт миграций 1→6, версия БД 7 |
| `repository/` | `ComponentTemplatesRepository` | Обёртка над DAO для реактивной работы с шаблонами компонентов |
| `viewmodel/` | `ClientsViewModel`, `HierarchyViewModel`, `MaintenanceViewModel`, `TemplatesViewModel`, `TemplateEditorViewModel` | Управление состояниями экранов, работа с DAO, обвязка для Compose |
| `ui/` | Экранные модули (`clients`, `hierarchy`, `maintenance`, `templates`, `reports`, `settings`), `Nav.kt`, общие компоненты | Визуальный слой, маршруты и композиции |
| `report/` | `ReportAssembler`, рендереры (HTML/DOCX), экспортеры PDF, `CompanyConfigLoader`, модели DTO | Генерация отчётов и работы с конфигурацией компании |
| `sync/` | `MySqlSyncService`, `SafeDeletionHelper`, `DeletionTracker` | Экспериментальная синхронизация с MySQL и фиксация удалений |
| `util/` | `Translit.kt` | Вспомогательные утилиты (например, транслитерация для файлов) |

### Ключевые файлы
| Файл | Ответственность |
|---|---|
| `MainActivity.kt` | Точка входа, запуск `WassertechTheme` и `AppNavHost` |
| `ui/Nav.kt` | Описание графа навигации, верхний бар, маршруты экранов |
| `ui/clients/ClientsRoute.kt` | Список клиентов, режим редактирования, сортировка групп |
| `ui/hierarchy/ComponentsScreen.kt` | Управление компонентами установки, переходы к ТО и истории |
| `ui/maintenance/MaintenanceScreen.kt` | Проведение ТО, работа с секциями компонентов, сохранение через `MaintenanceViewModel` |
| `ui/maintenance/MaintenanceSessionDetailScreen.kt` | Просмотр сохранённых сессий, переход к редактированию и отчётам |
| `ui/reports/ReportsScreen.kt` | Список готовых PDF, открытие через `FileProvider` |
| `ui/settings/SettingsScreen.kt` | Вызов push/pull MySQL, вывод логов синхронизации |
| `data/AppDatabase.kt` | Конфигурация Room, регистрация DAO и миграций, singleton-инстанс |
| `report/ReportAssembler.kt` | Сборка `ReportDTO` из DAO, агрегация компонентов и значений |
| `report/HtmlTemplateEngine.kt` / `DocxTemplateEngine.kt` | Рендеринг HTML/DOCX-шаблонов с данными ТО |
| `sync/MySqlSyncService.kt` | Полный цикл push/pull с удалённой MySQL-БД, создание таблиц, экспорт/импорт сущностей |

---

## Структура репозитория
| Путь | Содержимое | Описание |
|---|---|---|
| `/app/build.gradle.kts` | Конфигурация модуля, автоинкремент `version.properties` | Формирует `versionName = 0.7.1.build`, задаёт Compose, Room, POI, MySQL |
| `/app/src/main/AndroidManifest.xml` | Декларация activity, `FileProvider` | Настройки прав доступа к файлам отчётов |
| `/app/src/main/assets/` | `config/company_config.json`, HTML/DOCX шаблоны, логотип | Настройки компании для отчётов и макеты PDF |
| `/app/src/main/res/` | Иконки, темы, `xml/file_paths.xml` | Векторные ресурсы и настройки FileProvider |
| `/app/schemas/...` | JSON-снимки Room для версий 1–6 | Экспорт схем, обновляются KSP при сборке |
| `/gradle/`, `/gradle.properties`, `/settings.gradle.kts` | Системные файлы Gradle | Настройка сборки проекта |
| `/README_FOR_AI.md` | Вспомогательное описание для ассистентов | Краткие соглашения для автоматизации |

---

## Основные экраны и навигация
| Маршрут | Компонент | Назначение |
|---|---|---|
| `clients` | `ClientsRoute` | Список клиентов с группами, редактирование и архив |
| `client/{clientId}` | `ClientDetailScreen` | Карточка клиента, переходы к объектам и установкам |
| `site/{siteId}` | `SiteDetailScreen` | Просмотр объекта, список установок |
| `installation/{installationId}` | `ComponentsScreen` | Компоненты установки, запуск ТО, история |
| `maintenance_all/{siteId}/{installationId}/{installationName}` | `MaintenanceScreen` | Проведение ТО по установке, новая сессия |
| `maintenance_edit/{sessionId}/{siteId}/{installationId}/{installationName}` | `MaintenanceScreen` | Повторное открытие сохранённой сессии для корректировок |
| `maintenance_history` и `maintenance_history/{installationId}` | `MaintenanceHistoryScreen` | Общая/фильтрованная история ТО, переход к отчётам |
| `maintenance_session/{sessionId}` | `MaintenanceSessionDetailScreen` | Подробности сессии, запуск отчётов и редактирования |
| `templates` | `TemplatesScreen` | Управление шаблонами чек-листов и их порядком |
| `template_editor/{templateId}` | `TemplateEditorScreen` | Редактирование полей шаблона, флаги `isForMaintenance`, диапазоны |
| `reports` | `ReportsScreen` | Хранилище готовых PDF-файлов |
| `settings` | `SettingsScreen` | Пуш/пул с MySQL, статус операций |
| `about` | `AboutScreen` (заглушка) | Планируется инфоблок о компании и версии приложения |

---

## ViewModel и управление состояниями
| ViewModel | Зона ответственности |
|---|---|
| `ClientsViewModel` | CRUD клиентов и групп, архивирование, сортировка, перенос между группами |
| `HierarchyViewModel` (`HierarchyViewModel_editComponent`) | Работа с деревом «объекты → установки → компоненты», локальные перестановки, применение в БД |
| `MaintenanceViewModel` | Построение секций ТО по всем компонентам, валидация, сохранение `MaintenanceSessionEntity` и `MaintenanceValueEntity`, редактирование ранее сохранённых сессий |
| `TemplatesViewModel` | Загрузка и фильтрация шаблонов, архив, сортировка, сохранение порядков |
| `TemplateEditorViewModel` | Работа с полями шаблонов, изменение `min/max/unit/isForMaintenance`, синхронизация с DAO |

---

## Структура данных (Room)
База данных `wassertech_v1.db` версии 7 работает через `AppDatabase`. Включены конвертеры (`data/Converters.kt`, `data/types/Converters.kt`) для enum-типов и списков, DAO покрывают как реактивные (`Flow`), так и синхронные `Now()`-методы для отчётности.

### Сущности
| Entity | Таблица | Ключевые поля | Назначение и связи |
|---|---|---|---|
| `ClientGroupEntity` | `client_groups` | `title`, `sortOrder`, `isArchived` | Группы клиентов, используются при группировке списков |
| `ClientEntity` | `clients` | Контакты, адрес, `sortOrder`, `clientGroupId`, архивные поля | Корневая сущность, связывается с объектами (`SiteEntity`) |
| `SiteEntity` | `sites` | `clientId`, `name`, `address`, `orderIndex`, архивные флаги | Объекты клиента, родитель установок |
| `InstallationEntity` | `installations` | `siteId`, `name`, `orderIndex`, `isArchived` | Установки на объекте, родитель компонентов |
| `ComponentEntity` | `components` | `installationId`, `name`, `type`, `orderIndex`, `templateId` | Оборудование установки, привязывается к шаблону ТО |
| `ComponentTemplateEntity` | `component_templates` | `category`, `sortOrder`, `defaultParamsJson` | Базовые заготовки компонентов, архивируются отдельно |
| `ChecklistTemplateEntity` | `checklist_templates` | `componentType`, `sortOrder`, `isArchived`, `updatedAtEpoch` | Шаблон чек-листа, привязывается к типу или компоненту |
| `ChecklistFieldEntity` | `checklist_fields` | `templateId`, `key`, `type`, `unit`, `min/max`, `isForMaintenance` | Поля шаблонов, фильтруются на экране ТО |
| `MaintenanceSessionEntity` | `maintenance_sessions` | `siteId`, `installationId`, `startedAtEpoch`, `technician`, `synced` | Сессии ТО, связывают объекты/установки и результаты |
| `MaintenanceValueEntity` | `maintenance_values` | `sessionId`, `componentId`, `fieldKey`, `valueText`, `valueBool` | Значения полей, индексированы по сессии и компоненту |
| `ObservationEntity` | `observations` | `sessionId`, `componentId`, `fieldKey`, значения | Дополнительные наблюдения по ТО |
| `IssueEntity` | `issues` | `sessionId`, `componentId`, `severity`, `description` | Новые проблемы, фиксируемые во время ТО |
| `DeletedRecordEntity` | `deleted_records` | `tableName`, `recordId`, `deletedAtEpoch` | Трекер удалений для синхронизации с MySQL |

### Перечисления и конвертеры
- `ComponentType` (COMMON, HEAD) — разделяет общие и головные компоненты.
- `FieldType` (CHECKBOX, NUMBER, TEXT) — типы полей чек-листа, цитируются в UI.
- `Severity` (LOW, MEDIUM, HIGH) — уровень критичности `IssueEntity`.
- `Converters.kt` сохраняет enum-значения в строковом виде для Room и обратных преобразований.

### DAO
| DAO | Назначение |
|---|---|
| `ClientDao` | CRUD клиентов и групп, архив/restore, `getAllClientsNow()` для отчётов |
| `HierarchyDao` | Выборка дерева, операции с компонентами, `getAllSites/Installations/ComponentsNow()` для синхронизации |
| `TemplatesDao` / `ComponentTemplatesDao` | Управление шаблонами полей и готовыми компонентами, поддержка порядка и архива |
| `SessionsDao` | Создание/редактирование сессий, сохранение значений, агрегирующие выборки |
| `ChecklistDao` | Вспомогательные запросы для редактора шаблонов |
| `ArchiveDao`, `DeletedRecordsDao` | Жёсткое удаление и учёт удалённых записей для синхронизации |

### Миграции
| Версия | Файл | Суть изменений |
|---|---|---|
| 1→2 | `Migration_1_2.kt` | Ранняя нормализация клиентов и объектов |
| 2→3 | `Migration_2_3.kt` | Добавлены таблицы ТО (`maintenance_sessions`, `maintenance_values`) и индексы |
| 3→4 | `Migration_3_4.kt` | Расширение шаблонов, новые поля и индексы |
| 4→5 | `MIGRATION_4_5.kt` | Добавлены `component_templates`, связка шаблонов по ID |
| 5→6 | `MIGRATION_5_6.kt` | Архивирование `sites` и `installations`, индексы по `isArchived` |
| 6→7 | `MIGRATION_6_7` (в разработке) | Обновление `ComponentType` (COMMON/HEAD), требуется реализация SQL в миграции |

### Сидеры и схемы
- `TemplateSeeder.seedOnce()` создаёт базовые шаблоны `COMMON` и `HEAD` с тестовыми полями.
- JSON-снимки Room (`app/schemas/com.example.wassertech.data.AppDatabase/1...6.json`) обновляются при сборке KSP и входят в контроль версий.

---

## Отчётность и шаблоны
- `ReportAssembler` собирает `ReportDTO` через `Now()`-методы DAO: клиент, объект, установка, значения полей, наблюдения и проблемы.
- HTML-шаблоны (`assets/templates/maintenance_v1-3.html`) и DOCX-шаблон (`assets/templates/Report_Template_Wassertech.docx`) позволяют выпускать отчёты разного уровня подробности.
- `HtmlTemplateEngine` и `DocxTemplateEngine` внедряют данные из DTO, поддерживают логотип (`CompanyConfigLoader.logoToDataUri`), подписи и анализ воды.
- `PdfExporter` / `WebViewPdfExporter` сохраняют отчёт в каталог `Android/data/<pkg>/files/Reports`, откуда его можно открыть или расшарить.
- На экране деталей сессии можно сформировать отчёт и попасть в `ReportsScreen` для просмотра истории экспорта.

---

## Синхронизация и обмен данными
- `MySqlSyncService.pushToRemote()` и `.pullFromRemote()` организуют полнодуплексный обмен с MySQL (создание таблиц, UPSERT, batch-операции, удаление).
- `SafeDeletionHelper` и `DeletionTracker` фиксируют удаления в `deleted_records`, чтобы MySQL-слой мог повторить операции.
- Требуется вынесение учётных данных (URL, логин, пароль) из кода в конфигурацию и внедрение шифрования/HTTPS-туннеля перед выходом в прод.
- На экране «Настройки» выводится статус обмена и последняя сводка (список сущностей, количество записей).

---

## Конфигурация и ресурсы
| Ресурс | Описание |
|---|---|
| `assets/config/company_config.json` | Юр. данные, телефоны и шаблон подписи для отчётов |
| `assets/templates/maintenance_v*.html` | HTML-макеты отчётов (v1–v3) |
| `assets/templates/Report_Template_Wassertech.docx` | Шаблон Word для дальнейшего экспорта |
| `assets/img/logo-wassertech-bolder.png` | Логотип компании для отчётов и AppBar |
| `res/xml/file_paths.xml` | Настройка `FileProvider` для доступа к PDF |
| `local.properties.example` | Шаблон настроек окружения (SDK path, keystore) |

---

## Сборка и запуск
1. Установите Android Studio Giraffe+ (Kotlin 1.9, Gradle 8) и Android SDK 36.
2. Создайте `local.properties` (можно скопировать из `local.properties.example`), пропишите пути SDK/NDK и при необходимости keystore.
3. Откройте проект, дождитесь синхронизации Gradle и генерации схем Room (папка `app/schemas`).
4. Соберите `app` (Build → Make Module) — при каждой сборке обновится `version.properties` и `versionCode`.
5. Запустите на устройстве/эмуляторе с Android 8.0+ (API 26+) — отчётные функции требуют доступ к хранилищу (по SAF).

---

## Тестирование и качество
- Unit-тесты Room и бизнес-логики пока не реализованы — рекомендовано покрыть DAO in-memory тестами (миграции, сортировки, архивы).
- UI-тесты Compose отсутствуют; приоритет — smoke-тесты основных экранов (клиенты, ТО, история).
- Логи синхронизации выводятся в `Logcat` (`MySQLSync`), можно воспроизводить сценарии вручную.
- Для отчётов полезно иметь референсные PDF в каталоге `Reports` и проверять соответствие шаблонов до, после изменений.

---

## План развития
### Ближайшие спринты (0–4 недели)
- Завершить реализацию `MIGRATION_6_7` и обновить JSON-схемы Room до версии 7.
- Добавить валидацию полей ТО (min/max, обязательные отмеченные `isForMaintenance`).
- Доработать `TemplateEditorScreen`: drag&drop порядка полей, маски для числовых значений.
- Переписать `AboutScreen` на конкретный блок с версией, ссылками и контактами.
- Перенести MySQL креды в конфигурацию и прикрыть push/pull авторизацией.

### Среднесрочно (1–2 квартала)
- Реализовать черновики и возобновление незавершённых сессий ТО.
- Добавить экспорт/импорт базы (backup/restore) и планировщик напоминаний.
- Разделить `MySqlSyncService` на слой API + off-line очередь, внедрить retry/backoff.
- Настроить автоматическую генерацию отчётов (HTML/PDF) в фоне и отправку по почте.
- Ввести поддержку нескольких брендов через конфигурацию и ресурсы (white-label).

### Исследования и долгий горизонт
- Облачная синхронизация через REST/GraphQL с авторизацией и конфликт-менеджментом.
- Desktop/web-клиент для диспетчеров (общая БД, отчёты, аналитика).
- Интеграция датчиков (IoT) с загрузкой телеметрии в карточки компонентов.
- Полная локализация (ru/en) и ночная тема.

---

© Wassertech, 2025. Документ отражает состояние ветки `cursor/update-project-readme-with-detailed-descriptions-a189` на 08.11.2025.
