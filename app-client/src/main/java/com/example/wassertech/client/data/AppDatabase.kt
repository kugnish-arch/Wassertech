package ru.wassertech.client.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.wassertech.client.data.dao.*
import ru.wassertech.client.data.entities.*
import ru.wassertech.client.data.entities.MaintenanceValueEntity

// Импорты миграций =========================================
import ru.wassertech.client.data.migrations.MIGRATION_1_2
import ru.wassertech.client.data.migrations.MIGRATION_2_3
import ru.wassertech.client.data.migrations.MIGRATION_3_4
import ru.wassertech.client.data.migrations.MIGRATION_4_5
import ru.wassertech.client.data.migrations.MIGRATION_5_6
import ru.wassertech.client.data.migrations.MIGRATION_6_7   // ← Обновление ComponentType
import ru.wassertech.client.data.migrations.MIGRATION_7_8   // ← Добавление таблицы settings

@Database(
    version = 8, // ← Обновлено: добавлена таблица settings
    exportSchema = true,
    entities = [
        ClientEntity::class,
        ClientGroupEntity::class,
        SiteEntity::class,
        InstallationEntity::class,
        ComponentEntity::class,
        ChecklistTemplateEntity::class,
        ChecklistFieldEntity::class,
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
    abstract fun templatesDao(): TemplatesDao
    abstract fun sessionsDao(): SessionsDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun clientDao(): ClientDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun deletedRecordsDao(): DeletedRecordsDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wassertech_client_v1.db"  // ← Другое имя файла для app-client
                )
                    // Подключаем миграции
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,    // ← Обновление ComponentType
                        MIGRATION_7_8    // ← Добавление таблицы settings
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


