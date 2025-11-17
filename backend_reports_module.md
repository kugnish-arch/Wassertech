# Подсистема PDF-отчётов для технического обслуживания

**Версия:** 1.0  
**Дата:** 2025-01-13  
**Базовый URL:** `https://api.wassertech.ru/api/public`

---

## Содержание

1. [Структура таблицы reports](#1-структура-таблицы-reports)
2. [Endpoint: загрузка PDF-отчёта](#2-endpoint-загрузка-pdf-отчёта)
3. [Endpoint: получение списка отчётов](#3-endpoint-получение-списка-отчётов)
4. [Типовой сценарий использования](#4-типовой-сценарий-использования)
5. [Безопасность и ограничения доступа](#5-безопасность-и-ограничения-доступа)
6. [Структура хранения файлов](#6-структура-хранения-файлов)

---

## 1. Структура таблицы reports

### 1.1. Поля

Таблица `reports` содержит следующие поля:

| Поле | Тип | Описание | Обязательное |
|------|-----|----------|--------------|
| `id` | VARCHAR(36) | UUID отчёта (первичный ключ) | Да |
| `sessionId` | VARCHAR(36) | UUID сессии ТО (FK → `maintenance_sessions.id`) | Нет |
| `clientId` | VARCHAR(36) | UUID клиента (FK → `clients.id`) | Нет |
| `siteId` | VARCHAR(36) | UUID объекта (FK → `sites.id`) | Нет |
| `installationId` | VARCHAR(36) | UUID установки (FK → `installations.id`) | Нет |
| `fileName` | VARCHAR(255) | Имя файла для отображения пользователю | Да |
| `fileUrl` | VARCHAR(255) | Относительный путь к PDF в `uploads/reports/{clientId}/` | Нет |
| `filePath` | VARCHAR(500) | Полный путь к файлу на диске (legacy, для совместимости) | Нет |
| `fileSize` | BIGINT(20) | Размер файла в байтах | Нет |
| `mimeType` | VARCHAR(100) | MIME-тип файла (по умолчанию `application/pdf`) | Нет |
| `title` | VARCHAR(255) | Заголовок отчёта | Нет |
| `description` | TEXT | Описание отчёта | Нет |
| `generatedAtEpoch` | BIGINT(20) | Время генерации отчёта в мс (epoch) | Да |
| `createdAt` | DATETIME | Дата создания (legacy, для совместимости) | Нет |
| `createdAtEpoch` | BIGINT(20) | Дата создания в мс (epoch) | Да |
| `updatedAtEpoch` | BIGINT(20) | Дата последнего обновления в мс (epoch) | Нет |
| `isArchived` | TINYINT(1) | Флаг архивации (0 = активен, 1 = архивирован) | Да (по умолчанию 0) |
| `archivedAtEpoch` | BIGINT(20) | Время архивации в мс (epoch), NULL если не архивирован | Нет |

### 1.2. Связи

- `sessionId` → `maintenance_sessions.id`
- `installationId` → `installations.id`
- `siteId` → `sites.id`
- `clientId` → `clients.id`

Цепочка связей: `reports.sessionId` → `maintenance_sessions.installationId` → `installations.siteId` → `sites.clientId` → `clients.id`

### 1.3. Индексы

- `PRIMARY KEY` на `id`
- `KEY idx_sessionId` на `sessionId`
- `KEY idx_installationId` на `installationId`
- `KEY idx_clientId` на `clientId`
- `KEY idx_isArchived` на `isArchived`

---

## 2. Endpoint: загрузка PDF-отчёта

### 2.1. Общая информация

**Метод:** POST  
**URL:** `/api/public/reports/upload`  
**Назначение:** Загрузка PDF-отчёта на сервер. Используется CRM-приложением (ENGINEER/ADMIN) для сохранения сгенерированных отчётов.  
**Авторизация:** Требуется JWT токен, роль ADMIN или ENGINEER  
**Статус:** ✅ РЕАЛИЗОВАНО

### 2.2. Требуемая роль

Только пользователи с ролью **ADMIN** или **ENGINEER** могут загружать отчёты.  
Пользователи с ролью **CLIENT** получат ошибку **403 Forbidden**.

### 2.3. Формат запроса

**Content-Type:** `multipart/form-data`

**Поля формы:**

| Поле | Тип | Описание | Обязательное |
|------|-----|----------|--------------|
| `file` | File | Бинарный PDF-файл | Да |
| `sessionId` | String | UUID сессии ТО (`maintenance_sessions.id`) | Да |
| `fileName` | String | Имя файла для отображения (опционально) | Нет |

**Пример запроса (curl):**

```bash
curl -X POST https://api.wassertech.ru/api/public/reports/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@/path/to/report.pdf" \
  -F "sessionId=uuid-сессии-то" \
  -F "fileName=Акт ТО от 2025-11-17.pdf"
```

### 2.4. Логика обработки

1. Проверка токена и роли пользователя (ADMIN или ENGINEER)
2. Валидация файла (должен быть PDF)
3. Поиск сессии ТО по `sessionId`
4. Определение `clientId`, `siteId`, `installationId` через JOIN:
   - `maintenance_sessions.installationId` → `installations.id`
   - `installations.siteId` → `sites.id`
   - `sites.clientId` → `clients.id`
5. Проверка доступа к установке через `checkInstallationAccess()`
6. Создание директорий `uploads/reports/{clientId}/` (если не существуют)
7. Сохранение файла с уникальным именем: `{reportId}.pdf`
8. Запись информации в таблицу `reports`
9. Возврат JSON-ответа с данными созданного отчёта

### 2.5. Формат ответа

**Успешный ответ (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sessionId": "660e8400-e29b-41d4-a716-446655440001",
  "clientId": "770e8400-e29b-41d4-a716-446655440002",
  "siteId": "880e8400-e29b-41d4-a716-446655440003",
  "installationId": "990e8400-e29b-41d4-a716-446655440004",
  "fileName": "Акт ТО от 2025-11-17.pdf",
  "fileUrl": "uploads/reports/770e8400-e29b-41d4-a716-446655440002/550e8400-e29b-41d4-a716-446655440000.pdf",
  "createdAtEpoch": 1731800000000,
  "updatedAtEpoch": 1731800000000,
  "isArchived": 0
}
```

### 2.6. Коды ошибок

| Код | Описание | Причина |
|-----|----------|---------|
| **400** | Bad Request | Отсутствует `sessionId`, файл не загружен, файл не является PDF |
| **401** | Unauthorized | Токен отсутствует или невалиден |
| **403** | Forbidden | Пользователь не имеет роли ADMIN или ENGINEER, или нет доступа к установке |
| **404** | Not Found | Сессия ТО не найдена |
| **500** | Internal Server Error | Ошибка БД, ошибка создания директорий, ошибка сохранения файла |

**Примеры ошибок:**

```json
{
  "error": "sessionId is required"
}
```

```json
{
  "error": "access denied: only ADMIN and ENGINEER can upload reports"
}
```

```json
{
  "error": "maintenance session not found"
}
```

---

## 3. Endpoint: получение списка отчётов

### 3.1. Общая информация

**Метод:** GET  
**URL:** `/api/public/reports/list`  
**Назначение:** Получение списка отчётов. Используется Client-приложением для отображения доступных отчётов клиенту.  
**Авторизация:** Требуется JWT токен  
**Статус:** ✅ РЕАЛИЗОВАНО

### 3.2. Поведение для разных ролей

#### Роль CLIENT

- Возвращаются **только** отчёты клиента, к которому привязан пользователь (`users.client_id`)
- Параметр `clientId` в query **игнорируется** (используется только `users.client_id` из токена)
- Возвращаются только неархивные отчёты (`isArchived = 0`)
- Если у пользователя не установлен `client_id`, возвращается ошибка **403**

#### Роль ADMIN / ENGINEER

- Возвращаются все неархивные отчёты
- Опционально можно фильтровать по `clientId` через query-параметр
- Если `clientId` не указан, возвращаются все неархивные отчёты

### 3.3. Параметры запроса

| Параметр | Тип | Описание | Обязательное |
|----------|-----|----------|--------------|
| `sinceUpdatedAtEpoch` | Long | Unix timestamp в мс. Если указан, возвращаются только отчёты с `updatedAtEpoch > sinceUpdatedAtEpoch` (для инкрементальной синхронизации) | Нет |
| `clientId` | String | UUID клиента (только для ADMIN/ENGINEER, игнорируется для CLIENT) | Нет |

**Пример запроса:**

```
GET /api/public/reports/list?sinceUpdatedAtEpoch=1731800000000
Authorization: Bearer <JWT_TOKEN>
```

### 3.4. Формат ответа

**Успешный ответ (200 OK):**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "sessionId": "660e8400-e29b-41d4-a716-446655440001",
    "clientId": "770e8400-e29b-41d4-a716-446655440002",
    "siteId": "880e8400-e29b-41d4-a716-446655440003",
    "installationId": "990e8400-e29b-41d4-a716-446655440004",
    "fileName": "Акт ТО от 2025-11-17.pdf",
    "fileUrl": "uploads/reports/770e8400-e29b-41d4-a716-446655440002/550e8400-e29b-41d4-a716-446655440000.pdf",
    "createdAtEpoch": 1731800000000,
    "updatedAtEpoch": 1731800050000,
    "isArchived": 0
  },
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440005",
    "sessionId": "bb0e8400-e29b-41d4-a716-446655440006",
    "clientId": "770e8400-e29b-41d4-a716-446655440002",
    "siteId": "cc0e8400-e29b-41d4-a716-446655440007",
    "installationId": "dd0e8400-e29b-41d4-a716-446655440008",
    "fileName": "Акт ТО от 2025-11-18.pdf",
    "fileUrl": "uploads/reports/770e8400-e29b-41d4-a716-446655440002/aa0e8400-e29b-41d4-a716-446655440005.pdf",
    "createdAtEpoch": 1731886400000,
    "updatedAtEpoch": 1731886450000,
    "isArchived": 0
  }
]
```

**Пустой список (если отчётов нет):**

```json
[]
```

### 3.5. Коды ошибок

| Код | Описание | Причина |
|-----|----------|---------|
| **401** | Unauthorized | Токен отсутствует или невалиден |
| **403** | Forbidden | У пользователя с ролью CLIENT не установлен `client_id` |
| **500** | Internal Server Error | Ошибка БД |

**Примеры ошибок:**

```json
{
  "error": "unauthorized"
}
```

```json
{
  "error": "client_id not set for CLIENT user"
}
```

---

## 4. Типовой сценарий использования

### 4.1. Загрузка отчёта из CRM

1. Инженер в CRM-приложении генерирует PDF-отчёт по сессии ТО (`maintenance_session`)
2. CRM вызывает `POST /api/public/reports/upload` с файлом и `sessionId`
3. Сервер:
   - Проверяет роль пользователя (должен быть ADMIN или ENGINEER)
   - Определяет `clientId`, `siteId`, `installationId` через JOIN с `maintenance_sessions`
   - Сохраняет файл в `uploads/reports/{clientId}/{reportId}.pdf`
   - Создаёт запись в таблице `reports`
4. Сервер возвращает JSON с данными созданного отчёта
5. CRM сохраняет информацию об отчёте локально

### 4.2. Получение списка отчётов в Client-приложении

1. Клиент открывает раздел "Отчёты" в Client-приложении
2. Приложение вызывает `GET /api/public/reports/list` с JWT токеном
3. Сервер:
   - Определяет роль пользователя из токена
   - Для роли CLIENT: фильтрует отчёты по `users.client_id` (игнорирует query-параметры)
   - Для роли ADMIN/ENGINEER: возвращает все неархивные отчёты (или фильтрует по `clientId`, если указан)
4. Сервер возвращает массив отчётов с полями `id`, `fileName`, `fileUrl`, `createdAtEpoch` и т.д.
5. Приложение отображает список отчётов пользователю

### 4.3. Скачивание PDF-файла

1. Клиент нажимает на отчёт в списке
2. Приложение формирует URL для скачивания: `{baseUrl}/{fileUrl}`
   - Например: `https://api.wassertech.ru/uploads/reports/{clientId}/{reportId}.pdf`
3. Приложение скачивает файл по этому URL (может потребоваться авторизация через токен)
4. Файл отображается пользователю или сохраняется локально

**Примечание:** Для скачивания файлов также можно использовать существующий endpoint `GET /api/public/reports/{reportId}/pdf`, который проверяет права доступа и отдаёт файл с правильными заголовками.

---

## 5. Безопасность и ограничения доступа

### 5.1. Проверка ролей

- **POST /reports/upload**: Только ADMIN и ENGINEER
- **GET /reports/list**: Все авторизованные пользователи, но с разной логикой фильтрации

### 5.2. Фильтрация данных для CLIENT

Для роли CLIENT:
- Используется **только** `users.client_id` из токена авторизации
- Любые попытки передать другой `clientId` через query-параметры **игнорируются**
- Невозможно получить отчёты другого клиента

### 5.3. Защита от path traversal

При работе с файлами:
- Проверяется отсутствие `..` в путях
- Проверяется, что путь не начинается с `/` (абсолютный путь)
- Имена файлов нормализуются (удаляются опасные символы)

### 5.4. Проверка доступа к установке

При загрузке отчёта проверяется доступ к установке через `checkInstallationAccess()`, которая учитывает:
- Прямой доступ через `user_membership` (scope = 'INSTALLATION')
- Доступ через объект (scope = 'SITE')
- Доступ через клиента (scope = 'CLIENT')

---

## 6. Структура хранения файлов

### 6.1. Директории

Все PDF-отчёты хранятся в следующей структуре:

```
uploads/
└── reports/
    ├── {clientId1}/
    │   ├── {reportId1}.pdf
    │   ├── {reportId2}.pdf
    │   └── ...
    ├── {clientId2}/
    │   ├── {reportId3}.pdf
    │   └── ...
    └── ...
```

Где:
- `uploads/` — корневая директория для загрузок (уже существует в проекте)
- `reports/` — подпапка для отчётов (создаётся автоматически)
- `{clientId}/` — подпапка для каждого клиента (UUID клиента)
- `{reportId}.pdf` — файл отчёта с уникальным именем (UUID отчёта + расширение)

### 6.2. Формат fileUrl

В таблице `reports` поле `fileUrl` содержит относительный путь от корня проекта:

```
uploads/reports/{clientId}/{reportId}.pdf
```

Этот путь используется для формирования URL скачивания файла.

### 6.3. Права доступа

Директории создаются с правами `0755` (rwxr-xr-x), что позволяет:
- Владельцу: чтение, запись, выполнение
- Группе и остальным: чтение и выполнение

### 6.4. Миграция существующих файлов

Если в таблице `reports` уже есть записи с заполненным `filePath`, но пустым `fileUrl`, миграция `015_reports_extended.sql` копирует значение из `filePath` в `fileUrl` (если `filePath` содержит относительный путь).

---

## 7. Миграция БД

Для применения изменений необходимо выполнить миграцию:

```sql
-- Файл: migrations/015_reports_extended.sql
```

Миграция:
1. Добавляет поля `clientId`, `fileUrl`, `updatedAtEpoch`, `createdAt`
2. Заполняет `clientId`, `siteId`, `installationId` для существующих записей через JOIN
3. Заполняет `createdAtEpoch` из `createdAt` (или текущим временем)
4. Заполняет `updatedAtEpoch` = `createdAtEpoch`
5. Копирует `filePath` в `fileUrl` (если `fileUrl` пустой)
6. Создаёт индексы для новых полей

---

## 8. Примеры использования

### 8.1. Загрузка отчёта (PHP)

```php
$ch = curl_init('https://api.wassertech.ru/api/public/reports/upload');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $jwtToken
]);
curl_setopt($ch, CURLOPT_POSTFIELDS, [
    'file' => new CURLFile('/path/to/report.pdf', 'application/pdf', 'report.pdf'),
    'sessionId' => 'uuid-сессии-то',
    'fileName' => 'Акт ТО от 2025-11-17.pdf'
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 201) {
    $report = json_decode($response, true);
    echo "Отчёт загружен: " . $report['fileUrl'];
} else {
    $error = json_decode($response, true);
    echo "Ошибка: " . $error['error'];
}
```

### 8.2. Получение списка отчётов (JavaScript)

```javascript
async function getReportsList(sinceUpdatedAtEpoch = null) {
    const url = new URL('https://api.wassertech.ru/api/public/reports/list');
    if (sinceUpdatedAtEpoch) {
        url.searchParams.set('sinceUpdatedAtEpoch', sinceUpdatedAtEpoch);
    }
    
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${jwtToken}`
        }
    });
    
    if (response.ok) {
        const reports = await response.json();
        return reports;
    } else {
        const error = await response.json();
        throw new Error(error.error);
    }
}

// Использование
const reports = await getReportsList();
reports.forEach(report => {
    console.log(`${report.fileName}: ${report.fileUrl}`);
});
```

### 8.3. Скачивание PDF-файла

```javascript
async function downloadReport(report) {
    // Вариант 1: Прямая ссылка на файл (если файлы доступны публично)
    const fileUrl = `https://api.wassertech.ru/${report.fileUrl}`;
    
    // Вариант 2: Через endpoint с проверкой доступа
    const fileUrl = `https://api.wassertech.ru/api/public/reports/${report.id}/pdf`;
    
    const response = await fetch(fileUrl, {
        headers: {
            'Authorization': `Bearer ${jwtToken}`
        }
    });
    
    if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = report.fileName;
        a.click();
    }
}
```

---

## 9. Примечания для разработчиков

1. **Совместимость**: Старое поле `filePath` сохраняется для совместимости, но приоритет отдаётся `fileUrl`
2. **Инкрементальная синхронизация**: Параметр `sinceUpdatedAtEpoch` позволяет получать только новые/обновлённые отчёты
3. **Архивация**: Архивные отчёты (`isArchived = 1`) не возвращаются в обычных запросах
4. **Валидация файлов**: При загрузке проверяется MIME-тип файла (должен быть `application/pdf`)
5. **Уникальность имён**: Имена файлов на диске генерируются на основе UUID отчёта, что гарантирует уникальность

---

**Конец документации**

