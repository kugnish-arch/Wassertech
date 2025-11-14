# Аудит слоя данных и оффлайна - Wassertech CRM

**Дата аудита:** 2024  
**Модуль:** app-crm

---

## 1. Использование прямого подключения к MySQL/JDBC

### Места прямого подключения:

#### 1.1. `app-crm/src/main/java/com/example/wassertech/sync/MySqlSyncService.kt`
- **Назначение:** Синхронизация данных между локальной Room БД и удаленной MySQL БД
- **Методы:**
  - `pushToRemote()` - отправка данных из Room в MySQL
  - `pullFromRemote()` - получение данных из MySQL в Room
  - `migrateRemoteDatabase()` - миграция удаленной БД
  - `registerUser()` - регистрация пользователя через MySQL
  - `loginUser()` - авторизация через MySQL
- **Используется в:**
  - `SettingsScreen.kt` - кнопки синхронизации (push/pull)
  - `LoginScreen.kt` - регистрация и авторизация пользователей

#### 1.2. `core/network/src/main/java/com/example/wassertech/core/network/AuthApiService.kt`
- **Назначение:** API сервис для авторизации (использует прямое JDBC подключение)
- **Методы:**
  - `registerUser()` - регистрация через MySQL
  - `loginUser()` - авторизация через MySQL
- **Используется в:**
  - `feature/auth/src/main/java/com/example/wassertech/feature/auth/LoginScreen.kt`

#### 1.3. `app-client/src/main/java/com/example/wassertech/client/sync/MySqlSyncService.kt`
- **Статус:** `@Deprecated` - помечен как устаревший
- **Назначение:** Получение клиентов и данных клиента из MySQL (только для app-client модуля)
- **Примечание:** В комментариях указано, что следует использовать HTTP API вместо прямого подключения

### Зависимости:
- `mysql:mysql-connector-java:5.1.49` (в `app-crm/build.gradle.kts` и `core/network/build.gradle.kts`)

---

## 2. Репозитории и DAO, работающие через Room

### 2.1. Room Database
**Файл:** `app-crm/src/main/java/com/example/wassertech/data/AppDatabase.kt`
- **Версия:** 8
- **Типы конвертеров:** `Converters` (для ComponentType, FieldType, Severity)

### 2.2. DAO интерфейсы:

#### `ClientDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/ClientDao.kt`)
- Работа с клиентами и группами клиентов
- Методы: `getAllGroupsNow()`, `getClientsNow()`, `upsertClient()`, `upsertGroup()`, `observeClients()`

#### `HierarchyDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/HierarchyDao.kt`)
- Работа с иерархией: Sites, Installations, Components
- Методы: `observeSites()`, `observeInstallations()`, `observeComponents()`, `upsertSite()`, `upsertInstallation()`, `upsertComponent()`

#### `SessionsDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/SessionsDao.kt`)
- Работа с сессиями ТО и значениями
- Методы: `observeSessions()`, `getValuesForSession()`, `insertSessionWithValues()`, `getAllSessionsNow()`, `getAllValuesNow()`

#### `TemplatesDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/TemplatesDao.kt`)
- Работа с шаблонами чеклистов и полями
- Методы: `observeTemplatesByType()`, `observeFields()`, `upsertTemplate()`, `getAllTemplatesNow()`, `getAllFieldsNow()`

#### `ChecklistDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/ChecklistDao.kt`)
- Работа с чеклистами

#### `ArchiveDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/ArchiveDao.kt`)
- Работа с архивированными записями

#### `DeletedRecordsDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/DeletedRecordsDao.kt`)
- Отслеживание удаленных записей для синхронизации

#### `SettingsDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/SettingsDao.kt`)
- Работа с настройками приложения

#### `ComponentTemplatesDao` (`app-crm/src/main/java/com/example/wassertech/data/dao/ComponentTemplatesDao.kt`)
- Работа с шаблонами компонентов

### 2.3. Репозитории:

#### `ComponentTemplatesRepository` (`app-crm/src/main/java/com/example/wassertech/repository/ComponentTemplatesRepository.kt`)
- Репозиторий для работы с шаблонами компонентов через Room

---

## 3. REST API вызовы к wassertech-server

### 3.1. API интерфейсы:

#### `app-client/src/main/java/com/example/wassertech/client/api/WassertechApi.kt`
- **Базовый URL:** настраивается через `ApiConfig.getBaseUrl()`
- **Эндпоинты:**
  - `POST /auth/login` - авторизация
  - `GET /installations` - получение списка установок

#### `core/network/src/main/java/com/example/wassertech/core/network/ApiClient.kt`
- **Назначение:** Создание Retrofit клиента
- **Использует:** Retrofit 2.9.0, Gson converter

### 3.2. Репозитории с REST API:

#### `app-client/src/main/java/com/example/wassertech/client/repository/InstallationsRepository.kt`
- **Назначение:** Загрузка установок через REST API с сохранением в Room
- **Логика:**
  - Онлайн режим: загружает с сервера через `WassertechApi.getInstallations()` и сохраняет в Room
  - Оффлайн режим: читает из Room через `HierarchyDao.getAllNonArchivedInstallationsNow()`
- **Используется в:** `app-client` модуле

#### `app-client/src/main/java/com/example/wassertech/client/auth/AuthRepository.kt`
- **Назначение:** Авторизация через REST API
- **Использует:** `WassertechApi.login()`

### 3.3. Зависимости:
- `com.squareup.retrofit2:retrofit:2.9.0`
- `com.squareup.retrofit2:converter-gson:2.9.0`

---

## 4. Список сущностей

### 4.1. clients (ClientEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ClientEntity.kt`
- **Поля синхронизации:**
  - ✅ `updatedAtEpoch: Long`
  - ✅ `isArchived: Boolean`
  - ✅ `archivedAtEpoch: Long?`
  - ❌ `deletedAtEpoch` - отсутствует (удаления отслеживаются через `DeletedRecordEntity`)
- **DAO:** `ClientDao`
- **Репозитории:** Нет отдельного репозитория, используется напрямую через `ClientDao` в `ClientsViewModel`

### 4.2. clientGroups (ClientGroupEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ClientGroupEntity.kt`
- **Поля синхронизации:**
  - ✅ `updatedAtEpoch: Long`
  - ✅ `isArchived: Boolean`
  - ✅ `archivedAtEpoch: Long?`
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `ClientDao`
- **Репозитории:** Нет отдельного репозитория

### 4.3. sites (SiteEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/SiteEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ✅ `isArchived: Boolean`
  - ✅ `archivedAtEpoch: Long?`
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `HierarchyDao`
- **Репозитории:** Нет отдельного репозитория, используется через `HierarchyViewModel`

### 4.4. installations (InstallationEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/InstallationEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ✅ `isArchived: Boolean`
  - ✅ `archivedAtEpoch: Long?`
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `HierarchyDao`
- **Репозитории:** 
  - В `app-client`: `InstallationsRepository` (использует REST API + Room)
  - В `app-crm`: используется напрямую через `HierarchyDao`

### 4.5. components (ComponentEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ❌ `isArchived` - отсутствует
  - ❌ `archivedAtEpoch` - отсутствует
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `HierarchyDao`
- **Репозитории:** Нет отдельного репозитория

### 4.6. componentTemplates (ComponentTemplateEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt`
- **Поля синхронизации:**
  - ✅ `updatedAtEpoch: Long`
  - ✅ `isArchived: Boolean`
  - ❌ `archivedAtEpoch` - отсутствует
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `ComponentTemplatesDao`
- **Репозитории:** `ComponentTemplatesRepository`

### 4.7. checklistTemplates (ChecklistTemplateEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ChecklistTemplateEntity.kt`
- **Поля синхронизации:**
  - ✅ `updatedAtEpoch: Long?`
  - ✅ `isArchived: Boolean`
  - ✅ `archivedAtEpoch: Long?`
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `TemplatesDao`
- **Репозитории:** Нет отдельного репозитория

### 4.8. checklistFields (ChecklistFieldEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/ChecklistFieldEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ❌ `isArchived` - отсутствует
  - ❌ `archivedAtEpoch` - отсутствует
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `TemplatesDao`
- **Репозитории:** Нет отдельного репозитория

### 4.9. maintenanceSessions (MaintenanceSessionEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/MaintenanceSessionEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ❌ `isArchived` - отсутствует
  - ❌ `archivedAtEpoch` - отсутствует
  - ❌ `deletedAtEpoch` - отсутствует
  - ✅ `synced: Boolean` - флаг синхронизации
- **DAO:** `SessionsDao`
- **Репозитории:** Нет отдельного репозитория

### 4.10. maintenanceValues (MaintenanceValueEntity)
- **Пакет:** `ru.wassertech.data.entities`
- **Файл:** `app-crm/src/main/java/com/example/wassertech/data/entities/MaintananceValueEntity.kt`
- **Поля синхронизации:**
  - ❌ `updatedAtEpoch` - отсутствует
  - ❌ `isArchived` - отсутствует
  - ❌ `archivedAtEpoch` - отсутствует
  - ❌ `deletedAtEpoch` - отсутствует
- **DAO:** `SessionsDao`
- **Репозитории:** Нет отдельного репозитория

### 4.11. Дополнительные сущности:

#### observations (ObservationEntity)
- **Статус:** Legacy формат (старый способ хранения значений ТО)
- **Поля синхронизации:** отсутствуют
- **DAO:** `SessionsDao`

#### issues (IssueEntity)
- **Поля синхронизации:** отсутствуют
- **DAO:** `SessionsDao`

#### deleted_records (DeletedRecordEntity)
- **Поля синхронизации:**
  - ✅ `deletedAtEpoch: Long`
- **DAO:** `DeletedRecordsDao`

---

## 5. Реализация оффлайн-режима

### 5.1. Экраны, читающие только из Room (полностью оффлайн):

#### Клиенты и группы:
- **Экран:** `ClientsScreen.kt`
- **ViewModel:** `ClientsViewModel`
- **Источник данных:** `ClientDao` (Room)
- **Статус:** ✅ Полностью оффлайн

#### Иерархия (Sites, Installations, Components):
- **Экраны:** `SitesScreen.kt`, `InstallationsScreen.kt`, `ComponentsScreen.kt`, `SiteDetailScreen.kt`
- **ViewModel:** `HierarchyViewModel`
- **Источник данных:** `HierarchyDao` (Room)
- **Статус:** ✅ Полностью оффлайн

#### Шаблоны:
- **Экран:** `TemplatesScreen.kt`, `TemplateEditorScreen.kt`
- **ViewModel:** `TemplatesViewModel`, `TemplateEditorViewModel`
- **Источник данных:** `TemplatesDao` (Room)
- **Статус:** ✅ Полностью оффлайн

#### Техническое обслуживание:
- **Экраны:** `MaintenanceScreen.kt`, `MaintenanceHistoryScreen.kt`, `MaintenanceSessionDetailScreen.kt`
- **ViewModel:** `MaintenanceViewModel`
- **Источник данных:** `SessionsDao` (Room)
- **Статус:** ✅ Полностью оффлайн

#### Отчёты:
- **Экран:** `ReportsScreen.kt`
- **Источник данных:** `ReportAssembler` (читает из Room через DAO)
- **Статус:** ✅ Полностью оффлайн (генерирует PDF из локальных данных)

### 5.2. Экраны, завязанные на прямой MySQL:

#### Настройки синхронизации:
- **Экран:** `SettingsScreen.kt`
- **Использование MySQL:**
  - Кнопка "Отправить на сервер" → `MySqlSyncService.pushToRemote()`
  - Кнопка "Загрузить с сервера" → `MySqlSyncService.pullFromRemote()`
  - Кнопка "Мигрировать БД" → `MySqlSyncService.migrateRemoteDatabase()`
- **Статус:** ⚠️ Прямое подключение к MySQL

#### Авторизация:
- **Экран:** `LoginScreen.kt` (app-crm)
- **Использование MySQL:**
  - Регистрация → `MySqlSyncService.registerUser()`
  - Авторизация → `MySqlSyncService.loginUser()`
- **Статус:** ⚠️ Прямое подключение к MySQL

### 5.3. Экраны, использующие REST API:

#### app-client модуль:
- **Экран:** `InstallationsReportsScreen.kt` (app-client)
- **Репозиторий:** `InstallationsRepository`
- **Логика:**
  - Онлайн: загружает через `WassertechApi.getInstallations()`
  - Оффлайн: читает из Room
- **Статус:** ✅ Гибридный режим (REST API + Room)

#### Авторизация (app-client):
- **Экран:** `LoginScreen.kt` (feature/auth)
- **Репозиторий:** `AuthRepository`
- **API:** `WassertechApi.login()`
- **Статус:** ✅ REST API

---

## 6. Выводы и рекомендации

### 6.1. Части приложения, требующие наибольшей переделки:

#### 🔴 Критичные изменения:

1. **MySqlSyncService.kt** (app-crm)
   - **Текущее состояние:** Прямое JDBC подключение для push/pull синхронизации
   - **Требуется:** Замена на REST API эндпоинты `/sync/push` и `/sync/pull`
   - **Сложность:** Высокая - нужно переписать всю логику синхронизации
   - **Затронутые сущности:** Все (clients, clientGroups, sites, installations, components, maintenanceSessions, maintenanceValues, checklistTemplates, checklistFields)

2. **Авторизация (app-crm)**
   - **Текущее состояние:** `MySqlSyncService.registerUser()` и `loginUser()` используют прямое JDBC
   - **Требуется:** Переход на REST API `/auth/register` и `/auth/login`
   - **Сложность:** Средняя - уже есть пример в `app-client` модуле
   - **Файлы:** `LoginScreen.kt` (app-crm), `MySqlSyncService.kt`

3. **AuthApiService.kt** (core/network)
   - **Текущее состояние:** Прямое JDBC подключение
   - **Требуется:** Замена на REST API или удаление (если используется только в app-crm)
   - **Сложность:** Средняя

#### 🟡 Средние изменения:

4. **Отсутствие полей `updatedAtEpoch` в некоторых сущностях**
   - **Проблема:** `sites`, `installations`, `components` не имеют `updatedAtEpoch`
   - **Требуется:** Добавить поля для отслеживания изменений (для синхронизации)
   - **Сложность:** Средняя - нужны миграции БД

5. **Отсутствие единообразной системы отслеживания удалений**
   - **Текущее состояние:** Удаления отслеживаются через `DeletedRecordEntity`, но не все сущности имеют `deletedAtEpoch`
   - **Требуется:** Унифицировать подход (либо все через `DeletedRecordEntity`, либо добавить `deletedAtEpoch` везде)
   - **Сложность:** Низкая-Средняя

### 6.2. Части, уже готовые и почти не нуждающиеся в изменениях:

#### ✅ Готовые к использованию:

1. **Все экраны работы с данными (Clients, Hierarchy, Templates, Maintenance)**
   - **Статус:** Уже работают полностью через Room
   - **Изменения:** Минимальные - возможно добавление вызовов синхронизации после изменений
   - **Файлы:** Все ViewModels и экраны в `ui/` директории

2. **DAO интерфейсы**
   - **Статус:** Полностью готовы, имеют методы `getAll*Now()` для синхронизации
   - **Изменения:** Не требуются

3. **app-client модуль (InstallationsRepository)**
   - **Статус:** Уже использует REST API + Room с оффлайн режимом
   - **Изменения:** Может служить шаблоном для app-crm

4. **ReportAssembler**
   - **Статус:** Читает данные из Room через DAO
   - **Изменения:** Не требуются

5. **Сущности с полями синхронизации:**
   - `ClientEntity` - ✅ готов
   - `ClientGroupEntity` - ✅ готов
   - `ChecklistTemplateEntity` - ✅ готов
   - `ComponentTemplateEntity` - ✅ готов

### 6.3. Рекомендации по миграции:

1. **Этап 1: Авторизация**
   - Заменить `MySqlSyncService.registerUser()` и `loginUser()` на REST API
   - Использовать `AuthRepository` из `app-client` как пример

2. **Этап 2: Синхронизация**
   - Создать REST API эндпоинты `/sync/push` и `/sync/pull` на сервере
   - Заменить `MySqlSyncService.pushToRemote()` и `pullFromRemote()` на вызовы REST API
   - Сохранить логику работы с Room (она уже правильная)

3. **Этап 3: Унификация полей**
   - Добавить `updatedAtEpoch` в `SiteEntity`, `InstallationEntity`, `ComponentEntity`
   - Создать миграции БД

4. **Этап 4: Удаление прямых подключений**
   - Удалить `mysql-connector-java` зависимость
   - Удалить или пометить как deprecated `MySqlSyncService` и `AuthApiService`

### 6.4. Архитектурные улучшения:

1. **Создать единый репозиторийный слой**
   - Сейчас большинство ViewModels работают напрямую с DAO
   - Рекомендуется создать репозитории для каждой сущности (как `InstallationsRepository` в app-client)
   - Это упростит добавление REST API вызовов

2. **Добавить механизм автоматической синхронизации**
   - После изменений в Room автоматически помечать записи как "требующие синхронизации"
   - Фоновая синхронизация при наличии интернета

3. **Унифицировать обработку ошибок**
   - Единый подход к обработке ошибок сети и синхронизации

---

## 7. Сводная таблица сущностей

| Сущность | Пакет | updatedAtEpoch | isArchived | archivedAtEpoch | deletedAtEpoch | DAO | Репозиторий |
|----------|-------|---------------|------------|-----------------|----------------|-----|-------------|
| clients | `ru.wassertech.data.entities` | ✅ | ✅ | ✅ | ❌ | ClientDao | - |
| clientGroups | `ru.wassertech.data.entities` | ✅ | ✅ | ✅ | ❌ | ClientDao | - |
| sites | `ru.wassertech.data.entities` | ❌ | ✅ | ✅ | ❌ | HierarchyDao | - |
| installations | `ru.wassertech.data.entities` | ❌ | ✅ | ✅ | ❌ | HierarchyDao | InstallationsRepository (app-client) |
| components | `ru.wassertech.data.entities` | ❌ | ❌ | ❌ | ❌ | HierarchyDao | - |
| componentTemplates | `ru.wassertech.data.entities` | ✅ | ✅ | ❌ | ❌ | ComponentTemplatesDao | ComponentTemplatesRepository |
| checklistTemplates | `ru.wassertech.data.entities` | ✅ | ✅ | ✅ | ❌ | TemplatesDao | - |
| checklistFields | `ru.wassertech.data.entities` | ❌ | ❌ | ❌ | ❌ | TemplatesDao | - |
| maintenanceSessions | `ru.wassertech.data.entities` | ❌ | ❌ | ❌ | ❌ | SessionsDao | - |
| maintenanceValues | `ru.wassertech.data.entities` | ❌ | ❌ | ❌ | ❌ | SessionsDao | - |
| deleted_records | `ru.wassertech.data.entities` | ❌ | ❌ | ❌ | ✅ | DeletedRecordsDao | - |

---

**Примечание:** Этот аудит основан на анализе кода на момент создания документа. При внесении изменений в код рекомендуется обновить данный документ.

