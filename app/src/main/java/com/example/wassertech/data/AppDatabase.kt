package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wassertech.data.dao.*
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.migrations.MIGRATION_2_3
import com.example.wassertech.data.migrations.MIGRATION_5_6
import com.example.wassertech.data.migrations.MIGRATION_6_7

@Database(
    version = 7,
    exportSchema = true,
    entities = [
        ClientEntity::class,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wassertech.db"
                )
                    // Known migrations; if some links are missing, we'll fall back to destructive (dev only)
                    .addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration() // <-- drop & recreate if no full path from current to 7
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
