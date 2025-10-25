package com.example.wassertech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.wassertech.data.dao.*
import com.example.wassertech.data.entities.*
import com.example.wassertech.data.types.Converters

@Database(
    version = 5,
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE components ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                }
            }
            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(dbx: SupportSQLiteDatabase) {
                    dbx.execSQL("ALTER TABLE clients ADD COLUMN isCorporate INTEGER NOT NULL DEFAULT 0")
                }
            }
            val MIGRATION_4_5 = object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE sites ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE installations ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                }
            }
            return Room.databaseBuilder(context, AppDatabase::class.java, "wassertech.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }
}
