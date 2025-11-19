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
import ru.wassertech.client.data.migrations.MIGRATION_8_9   // ← Добавление полей синхронизации
import ru.wassertech.client.data.migrations.MIGRATION_9_10  // ← Обновление deleted_records
import ru.wassertech.client.data.migrations.MIGRATION_10_11  // ← Добавление origin и created_by_user_id
import ru.wassertech.client.data.migrations.MIGRATION_11_12  // ← Добавление поддержки иконок
import ru.wassertech.client.data.migrations.MIGRATION_12_13  // ← Добавление поля folder в icon_packs
import ru.wassertech.client.data.migrations.MIGRATION_13_14  // ← Добавление таблицы reports
import ru.wassertech.client.data.migrations.MIGRATION_14_15  // ← Добавление таблицы user_membership

@Database(
    version = 15, // ← Обновлено: добавление таблицы user_membership
    exportSchema = true,
    entities = [
        ClientEntity::class,
        ClientGroupEntity::class,
        SiteEntity::class,
        InstallationEntity::class,
        ComponentEntity::class,
        ComponentTemplateEntity::class,
        ChecklistTemplateEntity::class,
        ChecklistFieldEntity::class,
        MaintenanceSessionEntity::class,
        MaintenanceValueEntity::class,
        ObservationEntity::class,
        IssueEntity::class,
        DeletedRecordEntity::class,
        SettingsEntity::class,
        IconPackEntity::class, // ← Новая сущность для паков иконок
        IconEntity::class, // ← Новая сущность для иконок
        ReportEntity::class, // ← Новая сущность для отчётов
        UserMembershipEntity::class // ← Сущность для контроля доступа пользователей
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
    abstract fun componentTemplatesDao(): ComponentTemplatesDao
    abstract fun iconPackDao(): ru.wassertech.client.data.dao.IconPackDao
    abstract fun iconDao(): ru.wassertech.client.data.dao.IconDao
    abstract fun reportsDao(): ru.wassertech.client.data.dao.ReportsDao
    abstract fun userMembershipDao(): ru.wassertech.client.data.dao.UserMembershipDao

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
                        MIGRATION_7_8,   // ← Добавление таблицы settings
                        MIGRATION_8_9,   // ← Добавление полей синхронизации
                        MIGRATION_9_10,  // ← Обновление deleted_records
                        MIGRATION_10_11,  // ← Добавление origin и created_by_user_id
                        MIGRATION_11_12,  // ← Добавление поддержки иконок
                        MIGRATION_12_13,  // ← Добавление поля folder в icon_packs
                        MIGRATION_13_14,  // ← Добавление таблицы reports
                        MIGRATION_14_15  // ← Добавление таблицы user_membership
                    )
                    // Fallback для очистки базы при ошибке миграции (для клиентского приложения это безопасно)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}



