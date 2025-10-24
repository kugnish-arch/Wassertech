package com.example.wassertech.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClientEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
}
