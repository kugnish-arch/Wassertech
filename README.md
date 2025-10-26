# Wassertech CRM — оффлайн-приложение для ТО систем водоподготовки

> Состояние: **Handover #3 (стабильная сборка после soft-delete, переработанный экран УСТАНОВКА).**

## 1) Технический стек
- **Package:** `com.example.wassertech`
- **Min SDK:** 26  **Target/Compile:** 36  **JDK:** 21  
- **Gradle:** 8.13  **AGP:** 8.7.2  **Kotlin:** 1.9.25  
- **Compose Compiler:** 1.5.15  **Compose BOM:** 2024.10.01  
- **Room:** 2.6.1  **KSP:** 1.9.25-1.0.20  

> Все версии зафиксированы; при обновлении — проверять совместимость Compose ↔ Kotlin.

---

## 2) Архитектура проекта
Иерархия данных:  
**Клиенты → Объекты (Sites) → Установки (Installations) → Компоненты (Components)**  
+ модули обслуживания (Maintenance Sessions, Observations, Issues).  

**База данных (Room v3)**  
- `@Database(version = 3, exportSchema = true)`  
- `MIGRATION_2_3` реализована, экспорт схем включён  
- Enum-поля сериализуются как `String` через `Converters`  

**Основные DAO:**  
`HierarchyDao`, `SessionsDao`, `TemplatesDao`, `ArchiveDao`  

**Основные ViewModel:**  
`HierarchyViewModel`, `ClientsViewModel`, `MaintenanceViewModel`

---

## 3) UI-слой (Jetpack Compose)
Экранная структура:
- **Клиенты:** `ClientsScreen`, `ClientDetailScreen`  
  - Переключатель *«Работа с архивом»*  
  - Архивирование / восстановление клиентов  
- **Объекты:** `SitesScreen`, `SiteDetailScreen`  
- **Установки:** `ComponentsScreen`  
  - Редактирование, reorder, добавление и редактирование компонентов  
  - Выпадающие списки (`ExposedDropdownMenuBox`)  
  - Кнопка **«Провести ТО»**, переход на `MaintenanceAllScreen`
- **ТО:** `MaintenanceScreen`, `MaintenanceAllScreen` (подготовлены к интеграции `saveSession(...)`)

---

## 4) Soft-delete (реализовано ✅)
- В `ClientEntity`:  
  ```kotlin
  val isArchived: Boolean = false  
  val archivedAtEpoch: Long? = null
  ```
- DAO скрывают архив по умолчанию.  
- В `ClientsScreen` добавлен тумблер *«Работа с архивом»*.  
- При активном режиме показываются архивированные клиенты, доступно восстановление.  
- Вся логика soft-delete протестирована на уровне ViewModel + UI.

---

## 5) Новое в Handover #3
✅ Исправлен и расширен экран **УСТАНОВКА**:  
- Рабочие выпадающие списки выбора объекта и типа компонента  
- Диалоги добавления и редактирования компонентов  
- Режим *«Изменить порядок»* с крупными стрелками и зелёной кнопкой ✔  
- Кнопка **«Провести ТО»** открывает `MaintenanceAllScreen`  
- Убрана локальная стрелка «Назад» — используется системный TopAppBar  

✅ `HierarchyDao` и `HierarchyViewModel` полностью синхронизированы с UI.  
✅ Soft-delete интегрирован в DAO и ViewModel.  
✅ Сборка стабильна, все экраны подключены к навигации (`Nav.kt`).

---

## 6) Дальнейшие шаги
- [ ] Реализовать `SessionsDao.saveSessionBundle()` и провязать `MaintenanceViewModel.saveSession(...)`  
- [ ] Добавить экран *«История ТО»*  
- [ ] Экспорт PDF-отчёта о сессии и `share`  
- [ ] Валидация полей NUMBER и единый форматер значений  
- [ ] Миграция Room v4 + тест миграции  

---

## 7) Сборка и запуск
1. Клонируйте репозиторий, откройте в **Android Studio Koala/Jellyfish+**  
2. Используйте JDK 21  
3. Выполните: `Build → Clean Project → Rebuild Project`  
4. При первом запуске `TemplateSeeder` создаёт базовые шаблоны  
5. При проблемах с Room/KSP — очистите кеш (`.gradle`, `.kotlin`, `app/build`)

---

## 8) Git-ветвление
- Feature: `feature/<название>`  
- Fix: `fix/<название>`  
- В PR указывать суть, тестирование и необходимость миграций.  
- Для изменений Room — обязательна миграция и тест.

---

## 9) Примечания
- Все enum-типы живут только в `data/types/`.  
- `AppDatabase.getInstance()` использовать вместо устаревших вызовов.  
- Совместимость Compose-Kotlin-AGP зафиксирована; обновлять только пакетно.  

---
