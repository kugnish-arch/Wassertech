package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wassertech.data.dao.*
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.entities.MaintenanceValueEntity

//Импорты миграций =========================================
import com.example.wassertech.data.migrations.MIGRATION_1_2
import com.example.wassertech.data.migrations.MIGRATION_2_3

@Database(
    version = 3,
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
        MaintenanceValueEntity::class, // ← ДОБАВЬ ЭТО
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
                    // Подключаем миграции
                    .addMigrations(MIGRATION_1_2,MIGRATION_2_3)


                    // Разрешаем разрушительную миграцию ТОЛЬКО с очень старых версий,
                    //.fallbackToDestructiveMigrationFrom(1, 2, 3, 4)

                    // Разрешаем разрушить БД при переходе с 1-й версии:
                    //.fallbackToDestructiveMigrationFrom(1)

                    //на реальных данных убираем 2 строки ниже, чтобы не убивать базу
                    //.fallbackToDestructiveMigration() // <-- drop & recreate if no full path from current to 7
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
