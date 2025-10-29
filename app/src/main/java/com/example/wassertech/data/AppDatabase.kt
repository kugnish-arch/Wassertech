package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wassertech.data.dao.*
import com.example.wassertech.data.entities.*


@Database(
    version = 2,
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
        ObservationEntity::class,
        IssueEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hierarchyDao(): HierarchyDao
    abstract fun templatesDao(): TemplatesDao
    abstract fun sessionsDao(): SessionsDao
    abstract fun checklistDao(): ChecklistDao

    abstract fun clientDao(): ClientDao


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wassertech_v1.db"
                )
                    // Known migrations; if some links are missing, we'll fall back to destructive (dev only)
                    //.addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)

                    // Разрешаем разрушительную миграцию ТОЛЬКО с очень старых версий,
                    //.fallbackToDestructiveMigrationFrom(1, 2, 3, 4)

                    // Разрешаем разрушить БД при переходе с 1-й версии:
                    .fallbackToDestructiveMigrationFrom(1)

                    //на реальных данных убираем 2 строки ниже, чтобы не убивать базу
                    .fallbackToDestructiveMigration() // <-- drop & recreate if no full path from current to 7
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
