package ru.wassertech.client.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.wassertech.client.data.dao.*
import ru.wassertech.client.data.entities.*
import ru.wassertech.client.data.entities.MaintenanceValueEntity

@Database(
    version = 17, // ← Обновлено: добавление полей filePath, fileSize, mimeType, createdByUserId в reports
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
                    "wassertech_client_v1.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}



