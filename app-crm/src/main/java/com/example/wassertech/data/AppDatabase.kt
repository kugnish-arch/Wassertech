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
import ru.wassertech.data.migrations.MIGRATION_12_13  // ← Добавление origin и created_by_user_id
import ru.wassertech.data.migrations.MIGRATION_13_14  // ← Добавление поддержки иконок
import ru.wassertech.data.migrations.MIGRATION_14_15  // ← Добавление таблицы icon_pack_sync_status
import ru.wassertech.data.migrations.MIGRATION_15_16  // ← Добавление поля folder в icon_packs
import ru.wassertech.data.migrations.MIGRATION_16_17  // ← Добавление таблицы user_membership
import ru.wassertech.data.migrations.MIGRATION_17_18  // ← Добавление поля thumbnailLocalPath в icons
import ru.wassertech.data.migrations.MIGRATION_18_19  // ← Добавление таблицы reports
import ru.wassertech.data.migrations.MIGRATION_19_20  // ← Добавление таблицы sensor_temperature_logs

@Database(
    version = 20, // ← Обновлено: добавление таблицы sensor_temperature_logs
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
        SettingsEntity::class,
        IconPackEntity::class, // ← Новая сущность для паков иконок
        IconEntity::class, // ← Новая сущность для иконок
        IconPackSyncStatusEntity::class, // ← Сущность для отслеживания статуса загрузки
        UserMembershipEntity::class, // ← Сущность для контроля доступа пользователей
        ReportEntity::class, // ← Сущность для PDF-отчётов
        SensorTemperatureLogEntity::class // ← Сущность для логов температуры датчиков
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
    abstract fun iconPackDao(): ru.wassertech.data.dao.IconPackDao
    abstract fun iconDao(): ru.wassertech.data.dao.IconDao
    abstract fun iconPackSyncStatusDao(): ru.wassertech.data.dao.IconPackSyncStatusDao
    abstract fun userMembershipDao(): ru.wassertech.data.dao.UserMembershipDao
    abstract fun reportDao(): ru.wassertech.data.dao.ReportDao
    abstract fun sensorTemperatureLogsDao(): SensorTemperatureLogsDao

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
                        MIGRATION_11_12,  // ← Добавление isHeadComponent
                        MIGRATION_12_13,  // ← Добавление origin и created_by_user_id
                        MIGRATION_13_14,  // ← Добавление поддержки иконок
                        MIGRATION_14_15,  // ← Добавление таблицы icon_pack_sync_status
                        MIGRATION_15_16,  // ← Добавление поля folder в icon_packs
                        MIGRATION_16_17,  // ← Добавление таблицы user_membership
                        MIGRATION_17_18,  // ← Добавление поля thumbnailLocalPath в icons
                        MIGRATION_18_19,  // ← Добавление таблицы reports
                        MIGRATION_19_20  // ← Добавление таблицы sensor_temperature_logs
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
