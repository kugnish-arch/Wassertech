<p align="center">
  <img src="app/src/main/res/drawable/logo_wassertech.webp" alt="Wassertech Logo" width="200"/>
</p>

<h1 align="center">Wassertech CRM</h1>

<p align="center">
  CRM-приложение для компании <b>Wassertech</b> — бизнеса по водоочистке и обслуживанию фильтров.<br/>
  Разработано на <b>Kotlin + Jetpack Compose + Room</b> с использованием архитектуры <b>MVVM</b>.
</p>

---

## 🚀 Возможности

✅ Добавление и удаление клиентов  
✅ Сохранение данных в локальной базе Room  
✅ Переключение светлой / тёмной темы  
✅ Минималистичный дизайн в стиле Material 3  
✅ Поддержка Android 8.0 (API 26) и выше

---

## 🧩 Технологии и стек

| Компонент | Версия | Назначение |
|------------|---------|-------------|
| **Kotlin** | 1.9.25 | основной язык |
| **Jetpack Compose** | 1.5.15 | UI-фреймворк |
| **Material 3** | androidx.compose.material3 | современный дизайн |
| **Room (KSP)** | 2.6.1 | локальное хранилище данных |
| **Gradle / AGP** | 8.13 / 8.7.2 | сборка проекта |
| **JDK** | 21 | среда выполнения |

---

## 🧠 Структура проекта
app/
├─ src/
│ └─ main/
│ ├─ java/com/example/wassertech/
│ │ ├─ data/ # Room: Entity, DAO, Repository
│ │ ├─ ui/ # Экраны Compose
│ │ └─ MainActivity # Точка входа
│ └─ res/ # Ресурсы (иконки, логотип)
├─ build.gradle.kts # настройки модуля
└─ settings.gradle.kts # корневой Gradle


---

## ⚙️ Сборка и запуск

1. Установи **Android Studio Koala+ (или Flamingo+)**
2. Клонируй проект:
   ```bash
   git clone https://github.com/kugnish-arch/Wassertech.git
3. Открой в Android Studio → File → Open → Wassertech
4. Дождись завершения Gradle Sync
5. Запусти ▶ на устройстве или эмуляторе   

🧾 Планы развития

🔹 Экспорт карточки клиента в PDF
🔹 Отправка контакта в WhatsApp
🔹 Поиск и фильтрация по базе
🔹 Синхронизация и резервное копирование
🔹 Поддержка облачного хранения

👤 Автор
Dmitry Shulski
📍 Россия
💼 Разработчик Android / Wassertech Digital 

