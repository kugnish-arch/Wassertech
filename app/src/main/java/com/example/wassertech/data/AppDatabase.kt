package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wassertech.data.dao.*
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.entities.MaintenanceValueEntity

// Импорты миграций =========================================
import com.example.wassertech.data.migrations.MIGRATION_1_2
import com.example.wassertech.data.migrations.MIGRATION_2_3
import com.example.wassertech.data.migrations.MIGRATION_3_4
import com.example.wassertech.data.migrations.MIGRATION_4_5
import com.example.wassertech.data.migrations.MIGRATION_5_6
import com.example.wassertech.data.migrations.MIGRATION_6_7   // ← Обновление ComponentType

@Database(
    version = 7, // ← Обновлено: обновление ComponentType на COMMON/HEAD
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
        DeletedRecordEntity::class
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
                        MIGRATION_6_7    // ← Обновление ComponentType
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
