package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// explicit imports
import com.example.wassertech.data.dao.HierarchyDao
import com.example.wassertech.data.dao.TemplatesDao
import com.example.wassertech.data.dao.SessionsDao
import com.example.wassertech.data.dao.ComponentTemplatesDao

import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.SiteEntity
import com.example.wassertech.data.entities.InstallationEntity
import com.example.wassertech.data.entities.ComponentEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import com.example.wassertech.data.entities.ObservationEntity
import com.example.wassertech.data.entities.IssueEntity
import com.example.wassertech.data.entities.ComponentTemplateEntity

import com.example.wassertech.data.migrations.MIGRATION_2_3
import com.example.wassertech.data.migrations.MIGRATION_5_6

@Database(
    version = 6,
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
        IssueEntity::class,
        ComponentTemplateEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hierarchyDao(): HierarchyDao
    abstract fun templatesDao(): TemplatesDao
    abstract fun sessionsDao(): SessionsDao
    abstract fun componentTemplatesDao(): ComponentTemplatesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wassertech.db"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_5_6
                    )
                    // Dev-only safety: wipe DB if any missing migration is detected.
                    // Remove before production if you need to preserve on-device data.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}