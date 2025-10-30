# Wassertech CRM — оффлайн-приложение для ТО систем водоподготовки  

> **Состояние:** Handover #7 (стабильная сборка после слияния ветки `dynamic-templates`, переработанный экран Клиенты и новая архитектура групп)

---

## 1) Технический стек
- **Package:** `com.example.wassertech`  
- **Min SDK:** 26  **Target/Compile:** 36  **JDK:** 21  
- **Gradle:** 8.13  **AGP:** 8.6.1  **Kotlin:** 1.9.25  
- **Compose Compiler:** 1.5.15  **Compose BOM:** 2024.10.01  
- **Room:** 2.6.1  **KSP:** 1.9.25-1.0.20  

> Все версии зафиксированы; при обновлении — проверять Compose ↔ Kotlin совместимость.

---

## 2) Архитектура проекта
Иерархия данных:  
**Клиенты → Объекты (Sites) → Установки (Installations) → Компоненты (Components)**  
+ модули обслуживания (Maintenance Sessions, Observations, Issues).

**Room Database v3**
- `@Database(version = 3, exportSchema = true)`  
- `MIGRATION_2_3` реализована, экспорт схем включён  
- Enum-поля сохраняются через `Converters`

**Основные DAO:**  
`HierarchyDao`, `SessionsDao`, `TemplatesDao`, `ArchiveDao`, `ClientsDao`  

**Основные ViewModel:**  
`HierarchyViewModel`, `ClientsViewModel`, `MaintenanceViewModel`, `TemplatesViewModel`

---

## 3) UI (Compose)
### Экранная структура
- **Клиенты:** `ClientsScreen` / `ClientsRoute`
  - Новая архитектура с группами (`ClientGroupEntity`)
  - Визуальное разделение плашек групп и клиентов (цвет, иконки, отступы)
  - Иконки: `Group`, `Business`, `Person`
  - Подсчёт количества в каждой группе (например «VIP (3)»)
  - Тумблер *«Корзина»* для работы с архивом
  - Диалоги создания группы и нового клиента
- **Объекты:** `SitesScreen`, `SiteDetailScreen`  
- **Установки:** `ComponentsScreen`  
  - Редактирование, добавление и переупорядочивание компонентов  
  - Кнопка **«Провести ТО»** → `MaintenanceAllScreen`
- **ТО:** `MaintenanceScreen`, `MaintenanceAllScreen` — готовы к интеграции `saveSession(...)`

---

## 4) Soft-Delete (реализовано ✅)
- В `ClientEntity`:
  ```kotlin
  val isArchived: Boolean = false
  val archivedAtEpoch: Long? = null
  ```
- DAO фильтруют архив по умолчанию.  
- В `ClientsScreen` добавлен тумблер *«Работа с архивом»*.  
- Доступно восстановление удалённых клиентов.  
- Логика soft-delete отлажена на уровне ViewModel и UI.

---

## 5) Новое в Handover #7
✅ Полностью обновлён экран **Клиенты**:  
- Добавлены группы (создание, разворачивание, подсчёт клиентов)  
- Иконки групп и клиентов для наглядности  
- Единый диалог создания клиента с выбором группы  
- Архив доступен через тумблер в нижней панели  
- Поддержана новая логика `ClientsViewModel` и `ClientsDao`  

✅ Интерфейс установок и компонентов перенесён на динамические шаблоны (подготовка к реализации редактируемых template-структур).  
✅ Кодовая база очищена и слита в ветку `master`.  
✅ Сборка стабильна, все основные экраны подключены к навигации (`Nav.kt`).  

---

## 6) Дальнейшие шаги
- [ ] Реализовать `SessionsDao.saveSessionBundle()` и провязку в `MaintenanceViewModel.saveSession(...)`  
- [ ] Добавить экран **История ТО**  
- [ ] Экспорт PDF и `share`  
- [ ] Добавить редактируемые динамические шаблоны компонентов (`TemplatesScreen`)  
- [ ] Единый формат числовых значений и валидацию NUMBER-полей  
- [ ] Миграция Room v4 и тест миграции

---

## 7) Сборка и запуск
1. Клонировать репозиторий → открыть в **Android Studio Koala/Jellyfish+**  
2. Убедиться, что используется **JDK 21**  
3. `Build → Clean Project → Rebuild Project`  
4. При первом старте `TemplateSeeder` создаёт базовые шаблоны  
5. При ошибках Room/KSP — очистить `.gradle`, `.kotlin`, `app/build`

---

## 8) Git-ветвление
- Основная ветка: `master`  
- Feature-ветки: `feature/<название>`  
- Fix-ветки: `fix/<название>`  
- Для изменений Room — обязательная миграция и тест.  
- Перед слиянием ветки → обязательно `commit + push + merge into current`.

---

## 9) Примечания
- Enum-типы хранятся в `data/types/`.  
- Использовать только `AppDatabase.getInstance()`.  
- Совместимость Compose ↔ Kotlin ↔ AGP зафиксирована; обновлять пакетно.  
- Все иконки и цвета в Material 3 вариантах — через `MaterialTheme.colorScheme`.

---

## 10) Фокус следующего чек-поинта (Handover #8)
🎯 Реализация редактируемых динамических шаблонов для компонентов  
+ экспорт в PDF и тестирование всей цепочки ТО.
