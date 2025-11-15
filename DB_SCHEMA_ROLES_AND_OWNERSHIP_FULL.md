# Полная схема базы данных: Роли, владельцы и права доступа

**Проект:** Wassertech CRM API  
**Версия:** 1.0  
**Дата:** 2025-01-13

---

## Вступление

Этот документ описывает полную структуру базы данных для системы ролей и владения данными в Wassertech API. Схема предназначена для использования при разработке и оптимизации Android-приложений (app-crm и app-client) под новую систему прав доступа.

### Ключевые концепции:

1. **Роли пользователей:** ADMIN, ENGINEER, CLIENT
2. **Привязка к клиенту:** Пользователи CLIENT привязаны к одному клиенту через `users.client_id`
3. **Владение данными:** Все бизнес-сущности имеют поле `origin` ('CRM' или 'CLIENT')
4. **Авторство:** Все бизнес-сущности имеют поле `created_by_user_id` для отслеживания создателя

---

## Таблица users

**Назначение:** Пользователи системы (инженеры, администраторы, клиенты)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `login` | VARCHAR(255) | Логин пользователя, UNIQUE |
| `password` | VARCHAR(255) | Хеш пароля (password_hash) |
| `passwordIsHashed` | TINYINT(1) | Флаг: 1 = хешированный пароль, 0 = plaintext (legacy) |
| `name` | VARCHAR(255) | Имя пользователя |
| `email` | VARCHAR(255) | Email |
| `phone` | VARCHAR(255) | Телефон |
| `role` | VARCHAR(32) | Роль: 'ADMIN', 'ENGINEER', 'CLIENT' (NOT NULL, DEFAULT 'ENGINEER') |
| `client_id` | VARCHAR(255) | FK → clients.id (NULL для ADMIN/ENGINEER, обязателен для CLIENT) |
| `lastLoginAtEpoch` | BIGINT | Unix timestamp последнего входа (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Индексы:

- PRIMARY KEY (`id`)
- UNIQUE KEY (`login`)
- INDEX `idx_role` (`role`)
- INDEX `idx_client_id` (`client_id`)

### Внешние ключи:

- `fk_users_client_id` → `clients(id)` ON DELETE SET NULL ON UPDATE CASCADE

### Правила:

- Если `role = 'CLIENT'`, то `client_id` должен быть NOT NULL
- Если `role = 'ADMIN'` или `ENGINEER`, то `client_id` может быть NULL

### Взаимосвязи:

- Один пользователь может быть привязан к одному клиенту (для роли CLIENT)
- Один клиент может иметь множество пользователей CLIENT

---

## Таблица clients

**Назначение:** Клиенты Wassertech

### Структура (ключевые поля):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `name` | VARCHAR(255) | Название клиента |
| `clientGroupId` | VARCHAR(255) | FK → client_groups.id (NULL для клиентов без группы) |
| `isArchived` | TINYINT(1) | Флаг архивации (0 = активен, 1 = архивирован) |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Взаимосвязи:

- Один клиент может иметь множество объектов (sites)
- Один клиент может иметь множество пользователей CLIENT (через `users.client_id`)

---

## Таблица sites

**Назначение:** Объекты клиентов (адреса, где установлено оборудование)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `clientId` | VARCHAR(255) | FK → clients.id (NOT NULL) |
| `name` | VARCHAR(255) | Название объекта |
| `address` | VARCHAR(500) | Адрес объекта |
| `orderIndex` | INT | Порядок сортировки |
| `origin` | VARCHAR(16) | Источник: 'CRM' или 'CLIENT' (NOT NULL, DEFAULT 'CRM') |
| `created_by_user_id` | VARCHAR(255) | FK → users.id (NULL для исторических данных) |
| `isArchived` | TINYINT(1) | Флаг архивации |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Индексы:

- PRIMARY KEY (`id`)
- INDEX `idx_clientId` (`clientId`)
- INDEX `idx_origin` (`origin`)
- INDEX `idx_isArchived` (`isArchived`)

### Внешние ключи:

- `fk_sites_client` → `clients(id)` ON DELETE CASCADE
- `fk_sites_created_by` → `users(id)` ON DELETE SET NULL ON UPDATE CASCADE

### Правила:

- `origin = 'CRM'` — объект создан инженером в CRM (клиент видит, но не может редактировать)
- `origin = 'CLIENT'` — объект создан клиентом в app-client (клиент может редактировать)

### Взаимосвязи:

- Один объект принадлежит одному клиенту (`clientId`)
- Один объект может иметь множество установок (installations)
- Один объект может быть создан одним пользователем (`created_by_user_id`)

---

## Таблица installations

**Назначение:** Установки на объектах (водоочистные системы)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `siteId` | VARCHAR(255) | FK → sites.id (NOT NULL) |
| `name` | VARCHAR(255) | Название установки |
| `orderIndex` | INT | Порядок сортировки |
| `origin` | VARCHAR(16) | Источник: 'CRM' или 'CLIENT' (NOT NULL, DEFAULT 'CRM') |
| `created_by_user_id` | VARCHAR(255) | FK → users.id (NULL для исторических данных) |
| `isArchived` | TINYINT(1) | Флаг архивации |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Индексы:

- PRIMARY KEY (`id`)
- INDEX `idx_siteId` (`siteId`)
- INDEX `idx_origin` (`origin`)
- INDEX `idx_isArchived` (`isArchived`)

### Внешние ключи:

- `fk_installations_site` → `sites(id)` ON DELETE CASCADE
- `fk_installations_created_by` → `users(id)` ON DELETE SET NULL ON UPDATE CASCADE

### Правила:

- `origin = 'CRM'` — установка создана инженером в CRM
- `origin = 'CLIENT'` — установка создана клиентом в app-client
- Связь с клиентом: через `sites.clientId` (JOIN installations → sites → clients)

### Взаимосвязи:

- Одна установка принадлежит одному объекту (`siteId`)
- Одна установка может иметь множество компонентов (components)
- Одна установка может иметь множество сессий ТО (maintenance_sessions)

---

## Таблица components

**Назначение:** Компоненты установок (фильтры, насосы и т.д.)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `installationId` | VARCHAR(255) | FK → installations.id (NOT NULL) |
| `name` | VARCHAR(255) | Название компонента |
| `type` | VARCHAR(32) | Тип: 'COMMON', 'HEAD' |
| `templateId` | VARCHAR(255) | FK → component_templates.id (NULL) |
| `orderIndex` | INT | Порядок сортировки |
| `origin` | VARCHAR(16) | Источник: 'CRM' или 'CLIENT' (NOT NULL, DEFAULT 'CRM') |
| `created_by_user_id` | VARCHAR(255) | FK → users.id (NULL для исторических данных) |
| `isArchived` | TINYINT(1) | Флаг архивации |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Индексы:

- PRIMARY KEY (`id`)
- INDEX `idx_installationId` (`installationId`)
- INDEX `idx_templateId` (`templateId`)
- INDEX `idx_origin` (`origin`)
- INDEX `idx_isArchived` (`isArchived`)

### Внешние ключи:

- `fk_components_installation` → `installations(id)` ON DELETE CASCADE
- `fk_components_template` → `component_templates(id)` ON DELETE SET NULL
- `fk_components_created_by` → `users(id)` ON DELETE SET NULL ON UPDATE CASCADE

### Правила:

- `origin = 'CRM'` — компонент создан инженером в CRM
- `origin = 'CLIENT'` — компонент создан клиентом в app-client
- Связь с клиентом: через `installations.siteId` → `sites.clientId`

### Взаимосвязи:

- Один компонент принадлежит одной установке (`installationId`)
- Один компонент может использовать один шаблон (`templateId`)
- Один компонент может иметь множество значений ТО (maintenance_values)

---

## Таблица component_templates

**Назначение:** Шаблоны компонентов (динамические типы оборудования)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `name` | VARCHAR(255) | Название шаблона |
| `description` | TEXT | Описание шаблона |
| `category` | VARCHAR(255) | Категория (например, 'Filter', 'Softener', 'RO') |
| `orderIndex` | INT | Порядок сортировки |
| `origin` | VARCHAR(16) | Источник: 'CRM' или 'CLIENT' (NOT NULL, DEFAULT 'CRM') |
| `created_by_user_id` | VARCHAR(255) | FK → users.id (NULL для исторических данных) |
| `isArchived` | TINYINT(1) | Флаг архивации |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Индексы:

- PRIMARY KEY (`id`)
- INDEX `idx_category` (`category`)
- INDEX `idx_origin` (`origin`)
- INDEX `idx_isArchived` (`isArchived`)

### Внешние ключи:

- `fk_component_templates_created_by` → `users(id)` ON DELETE SET NULL ON UPDATE CASCADE

### Правила:

- `origin = 'CRM'` — шаблон создан инженером в CRM
- `origin = 'CLIENT'` — шаблон создан клиентом в app-client
- Шаблоны не привязаны к конкретному клиенту (глобальные)

### Взаимосвязи:

- Один шаблон может использоваться множеством компонентов (components.templateId)
- Один шаблон может иметь множество полей (component_template_fields)

---

## Таблица component_template_fields

**Назначение:** Поля шаблонов компонентов (характеристики оборудования)

### Структура (ключевые поля):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | VARCHAR(255) | UUID, PRIMARY KEY |
| `templateId` | VARCHAR(255) | FK → component_templates.id (NOT NULL) |
| `name` | VARCHAR(255) | Название поля |
| `key` | VARCHAR(255) | Ключ поля (для программного доступа) |
| `label` | VARCHAR(255) | Метка поля (для отображения) |
| `type` | VARCHAR(32) | Тип: 'string', 'number', 'boolean' и т.д. |
| `unit` | VARCHAR(32) | Единица измерения |
| `required` | TINYINT(1) | Обязательное поле (0/1) |
| `isCharacteristic` | TINYINT(1) | Является ли характеристикой (0/1) |
| `minValue` | DOUBLE | Минимальное значение (для чисел) |
| `maxValue` | DOUBLE | Максимальное значение (для чисел) |
| `orderIndex` | INT | Порядок сортировки |
| `isArchived` | TINYINT(1) | Флаг архивации |
| `archivedAtEpoch` | BIGINT | Unix timestamp архивации (миллисекунды) |
| `createdAtEpoch` | BIGINT | Unix timestamp создания (миллисекунды) |
| `updatedAtEpoch` | BIGINT | Unix timestamp обновления (миллисекунды) |

### Взаимосвязи:

- Одно поле принадлежит одному шаблону (`templateId`)

---

## Таблица user_membership

**Назначение:** Связь пользователей с установками/объектами/клиентами (для контроля доступа)

### Структура:

| Поле | Тип | Описание |
|------|-----|----------|
| `user_id` | VARCHAR(255) | FK → users.id (NOT NULL) |
| `scope` | VARCHAR(32) | Область: 'CLIENT', 'SITE', 'INSTALLATION' |
| `target_id` | VARCHAR(255) | ID целевой сущности (клиента, объекта или установки) |

### Индексы:

- PRIMARY KEY (`user_id`, `scope`, `target_id`)
- INDEX `idx_user_id` (`user_id`)
- INDEX `idx_scope` (`scope`)

### Правила:

- Используется для контроля доступа к установкам/объектам/клиентам
- Для пользователей CLIENT доступ контролируется через `users.client_id` + `user_membership`

---

## Логика прав доступа

### Правила редактирования:

1. **ADMIN и ENGINEER:**
   - Могут редактировать все сущности (независимо от `origin` и `client_id`)

2. **CLIENT:**
   - Может редактировать только сущности, которые:
     - Принадлежат его клиенту (`client_id = user.client_id`)
     - Имеют `origin = 'CLIENT'` (не может редактировать данные, созданные инженерами в CRM)
   - Может просматривать все данные своего клиента (включая `origin = 'CRM'`), но редактировать только свои

### Примеры SQL-запросов:

#### Получить все объекты клиента (для CLIENT):
```sql
SELECT s.* 
FROM sites s
WHERE s.clientId = ? -- client_id пользователя
  AND (s.isArchived = 0 OR s.isArchived IS NULL)
```

#### Получить объекты, которые клиент может редактировать:
```sql
SELECT s.* 
FROM sites s
WHERE s.clientId = ? -- client_id пользователя
  AND s.origin = 'CLIENT'
  AND (s.isArchived = 0 OR s.isArchived IS NULL)
```

#### Получить установки клиента (через объекты):
```sql
SELECT i.*, s.clientId
FROM installations i
INNER JOIN sites s ON i.siteId = s.id
WHERE s.clientId = ? -- client_id пользователя
  AND (i.isArchived = 0 OR i.isArchived IS NULL)
```

---

## Миграция данных

### Существующие данные:

- Все существующие пользователи получат `role = 'ENGINEER'` по умолчанию
- Все существующие записи в `sites`, `installations`, `components`, `component_templates` получат `origin = 'CRM'`
- Поле `created_by_user_id` для существующих записей останется NULL (исторические данные)

---

## Рекомендации для Android-приложений

### app-crm (для инженеров):

1. При создании/обновлении сущностей устанавливать:
   - `origin = 'CRM'`
   - `created_by_user_id = currentUser.id`

2. Не нужно фильтровать по `client_id` — инженеры видят все данные

3. При синхронизации (`/sync/push`):
   - Все отправляемые сущности должны иметь `origin = 'CRM'`
   - Устанавливать `created_by_user_id` при создании

### app-client (для клиентов):

1. При создании/обновлении сущностей устанавливать:
   - `origin = 'CLIENT'`
   - `created_by_user_id = currentUser.id`
   - `client_id` должен соответствовать `user.clientId`

2. Фильтровать данные по `client_id = user.clientId`

3. При редактировании проверять:
   - Сущность принадлежит клиенту пользователя (`client_id = user.clientId`)
   - Сущность имеет `origin = 'CLIENT'` (нельзя редактировать данные CRM)

4. При синхронизации (`/sync/push`):
   - Все отправляемые сущности должны иметь `origin = 'CLIENT'`
   - Устанавливать `created_by_user_id` при создании

---

## Заключение

Эта схема БД обеспечивает разделение прав доступа между инженерами и клиентами, отслеживание авторства данных и гибкую систему ролей. Все изменения обратно совместимы с существующими данными.

---

**Автор:** AI Assistant (Cursor)  
**Дата:** 2025-01-13

