package ru.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.wassertech.data.dao.*
import ru.wassertech.data.entities.*
import ru.wassertech.data.entities.MaintenanceValueEntity

// Импорты миграций =========================================
import ru.wassertech.data.migrations.MIGRATION_1_2
import ru.wassertech.data.migrations.MIGRATION_2_3
import ru.wassertech.data.migrations.MIGRATION_3_4
import ru.wassertech.data.migrations.MIGRATION_4_5
import ru.wassertech.data.migrations.MIGRATION_5_6
import ru.wassertech.data.migrations.MIGRATION_6_7   // ← Обновление ComponentType
import ru.wassertech.data.migrations.MIGRATION_7_8   // ← Добавление таблицы settings
import ru.wassertech.data.migrations.MIGRATION_8_9   // ← Добавление полей синхронизации
import ru.wassertech.data.migrations.MIGRATION_9_10  // ← Обновление deleted_records
import ru.wassertech.data.migrations.MIGRATION_10_11  // ← Объединение шаблонов
import ru.wassertech.data.migrations.MIGRATION_11_12  // ← Добавление isHeadComponent

@Database(
    version = 12, // ← Обновлено: добавление поля isHeadComponent в component_templates
    exportSchema = true,
    entities = [
        ClientEntity::class,
        ClientGroupEntity::class,
        SiteEntity::class,
        InstallationEntity::class,
        ComponentEntity::class,
        ComponentTemplateEntity::class,
        ComponentTemplateFieldEntity::class, // ← Новая сущность вместо ChecklistTemplateEntity и ChecklistFieldEntity
        MaintenanceSessionEntity::class,
        MaintenanceValueEntity::class,
        ObservationEntity::class,
        IssueEntity::class,
        DeletedRecordEntity::class,
        SettingsEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hierarchyDao(): HierarchyDao
    abstract fun templatesDao(): TemplatesDao // Оставлен для обратной совместимости
    abstract fun sessionsDao(): SessionsDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun clientDao(): ClientDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun deletedRecordsDao(): DeletedRecordsDao
    abstract fun settingsDao(): SettingsDao
    abstract fun componentTemplatesDao(): ComponentTemplatesDao
    abstract fun componentTemplateFieldsDao(): ComponentTemplateFieldsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wassertech_v1.db"
                )
                    // Подключаем миграции
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,    // ← Обновление ComponentType
                        MIGRATION_7_8,   // ← Добавление таблицы settings
                        MIGRATION_8_9,   // ← Добавление полей синхронизации
                        MIGRATION_9_10,  // ← Обновление deleted_records
                        MIGRATION_10_11,  // ← Объединение шаблонов (component_templates + component_template_fields)
                        MIGRATION_11_12  // ← Добавление isHeadComponent
                    )
                    // В проде обычно не используем destructive-опции, оставляю как у тебя:
                    //.fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
