0) Контекст

Проект: Wassertech CRM (Android, Kotlin, Jetpack Compose, Room).
Цель: офлайн CRM для водоочистки: клиенты → объекты → установки → компоненты; ТО (maintenance) с историей; шаблоны компонентов.
Текущая версия: v0.5.1. Приложение собирается и запускается.

1) Архитектура (слои)

Data: Room (Entities, DAO, AppDatabase + миграции). Версия БД: 4.

Domain/VM: ViewModel без Hilt (фабрики локально). Работа с DAO, выдача Flow/синхронных Now() методов.

UI: Jetpack Compose + Material 3; навигация через androidx.navigation.compose.
Экранные модули: clients, hierarchy (объект/установка/компоненты), templates, maintenance.

2) Основные экраны и маршруты навигации

Маршруты:

clients
templates
template_editor/{templateId}
client/{clientId}
site/{siteId}
installation/{installationId}
maintenance_all/{siteId}/{installationId}/{installationName}
maintenance_history
maintenance_history/{installationId}
maintenance_session/{sessionId}


Файл: ui/Nav.kt — AppTopBar, SectionHeader, NavHost.

3) ViewModel’ы

ClientsViewModel

Стейты: includeArchived: StateFlow<Boolean>, selectedGroupId: StateFlow<String?>, groups, clients.

Операции: CRUD групп/клиентов; архив/восстановление; сортировка стрелками (пересчёт sortOrder), переименование; перенос клиента в группу; быстрый reload на IO.

HierarchyViewModel

Достаёт установку/объект/клиента для подписи «Объект: Клиент — Объект».

Компоненты установки: выборка, добавление из шаблона, удаление, локальная сортировка и фиксация в БД (reorderComponents(installationId, orderedIds)).

TemplatesViewModel

Сейчас опционален (экран работает напрямую с DAO), но планируется унификация и кэш.

4) База данных (сущности и связи)

Ключевые Entities (папка data/entities):

ClientEntity — клиент (поля: id, name, isCorporate?, isArchived?, sortOrder?, контакты и пр., clientGroupId?).

ClientGroupEntity — группа клиентов (id, title, isArchived?, sortOrder?).

SiteEntity — объект клиента (id, clientId, название/адрес).

InstallationEntity — установка на объекте (id, siteId, title, и пр.).

ComponentEntity — компонент в установке (id, installationId, name, sortOrder?, isArchived?, тип и параметры).

ChecklistTemplateEntity (v0.5.1 расширен):
id, title, componentType, componentTemplateId?, sortOrder?, isArchived: Boolean = false, updatedAtEpoch: Long?

ChecklistFieldEntity — поля шаблонов.

MaintenanceSessionEntity — сессия ТО (дата/связи).

MaintenanceValueEntity — значения по полям в сессии ТО.

ObservationEntity, IssueEntity — заметки/проблемы по ТО.

Миграции: migrations/MIGRATION_1_2.kt, MIGRATION_2_3.kt, MIGRATION_3_4.kt
AppDatabase.kt — @Database(version = 4), exportSchema = true, fallbackToDestructiveMigrationOnDowngrade().

5) DAO (ключевые методы)

ClientDao — CRUD клиентов/групп; архив/восстановление; сортировки (нормализация sortOrder); выборки Flow и «Now()».

HierarchyDao — Site/Installation/Component: выборки, добавление/удаление компонентов, заголовки «Клиент — Объект».

TemplatesDao — активные/архивные шаблоны, сохранение порядка (sortOrder), archiveTemplate(...)/unarchiveTemplate(...), upsertTemplate(...).

SessionsDao — список сессий (observeAllSessions(), observeSessionsByInstallation(id)), вставки и чтение конкретной сессии.

ChecklistDao — поля шаблонов и вспомогательное.

В проект добавлены удобные синхронные Now()-методы для сборки отчётов (см. report/ReportAssembler.kt):

getSessionNow(sessionId)

getInstallationNow(id)

getSiteNow(id)

getClientNow(id)

getComponentsNow(installationId)

getObservationsForSessionNow(sessionId)

Если их нет в текущих DAO — добавить с @Query и suspend.

6) Экранная логика (коротко)

Clients (ui/clients/…)

Секции: «Общая» (без группы) + группы.

Стрелочная сортировка (в UI, тут же или на «Готово» — зависит от экрана).

Переименование, архив/восстановление.

Перенос клиента в группу (через меню). DnD пока не включали в прод (эксперимент откатили).

Общий нижний бар EditDoneBottomBar (включает режим редактирования и «Готово» с фиксацией порядка).

Hierarchy / Components

Заголовок установки + подпись «Объект: Клиент — Объект».

Кнопки: «Провести ТО» → maintenance_all/...; «История ТО» → общий или фильтрованный экран.

Локальная сортировка компонентов и фиксация на «Готово».

Templates

FAB «+ Шаблон» → диалог имени → создание ChecklistTemplateEntity → открытие редактора.

Активные/архивные, перестановка (локально) + фиксация sortOrder в DAO.

Maintenance

История ТО (вся/по установке), детальный просмотр сессии.

Массовое ТО для установки.

7) Технические соглашения

Любая фиксация порядка в БД → нормализация sortOrder с нуля подряд.

Архив: isArchived = true скрывает из основного списка (видимость зависит от флагов/режима редактирования).

Навигация: строковые маршруты; имена в параметрах энкодим через Uri.encode(...).

Потоки: I/O в coroutine Dispatchers.IO, UI — через StateFlow/collectAsState().

8) Печать/Отчёты (план С)

Цель: Генерация PDF из HTML-шаблона, собранного по данным сессии ТО.
Структура (папка report/):

ReportDTO.kt — DTO для отчёта (шапка клиента/объекта/установки, дата, список компонентов, измерения/наблюдения/проблемы).

ReportAssembler.kt — сборщик ReportDTO из БД (использует Now() методы DAO).

HtmlTemplateRenderer.kt — подстановка данных в HTML-шаблон (папка assets/reports/).

PdfExporter.kt — печать HTML в PDF (через WebView → print(PDF)).

В Android API используется createPrintDocumentAdapter("file"), затем onLayout/onWrite callbacks; учтены нюансы видимости LayoutResultCallback/WriteResultCallback.

Минимальный сценарий:

Выбрать MaintenanceSession → ReportAssembler.toReportDTO(sessionId).

HtmlTemplateRenderer.render(dto) → HTML-строка.

PdfExporter.export(context, html, fileUri) → сохраняем PDF (в SAF или приложенческую папку).

9) Планы ближайшего развития

Отчёты: довести HTML→PDF; добавить фирменный шаблон, логотип, таблицы значений, подписи/печати.

Экспорт/импорт: выгрузка БД (backup), импорт из файла.

Тесты: DAO (in-memory Room) + простые UI-тесты.

Унификация VM для Templates (сейчас часть через DAO).

(Позже) Синхронизация с сервером (многопользовательский режим, подписки, отчёты для заказчиков).

10) Руководство для AI-ассистента (Copilot/Cursor)

Рабочая среда: Android Studio (Gradle 8.x, Kotlin 1.9.x, Compose Material 3, Room 2.6.x).

Пакет: ru.wassertech.

Если добавляешь новые DAO-методы:

давай осмысленные имена: observeXyz(), getXyzNow() (suspend).

сохраняй миграции: при добавлении колонок — ALTER TABLE, не ломай version.

Любая сортировка «стрелками»/DnD:

сначала меняем локальный список id в UI,

при «Готово» — пишем sortOrder батчем (перенумерация 0..n),

избегай смешивания «сохранить сразу» и «сохранить на Готово» в одном экране.

Навигация: не меняй сигнатуры маршрутов без обновления Nav.kt.

UI: используй уже существующий EditDoneBottomBar.

Локализация: тексты сейчас на русском (ориентируйся на Material-копирайт в проекте).

Не вводи Hilt/модуляризацию без явной задачи (пока фабрики VM локальные).

11) Что можно брать в работу «с ходу»

Реализация HTML-шаблона отчёта + печать в PDF (см. report/*).

Добавление недостающих Now()-методов в DAO.

Юнит-тесты для DAO: проверки сортировок/архива/миграций.