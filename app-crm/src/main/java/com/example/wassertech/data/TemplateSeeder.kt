package com.example.wassertech.data

import com.example.wassertech.data.seed.TemplateSeeder as NewSeeder

/**
 * Legacy wrapper: keep backward compatibility with old imports.
 * New code should use com.example.wassertech.data.seed.TemplateSeeder.
 */
object TemplateSeeder {
    suspend fun seedOnce(db: AppDatabase) = NewSeeder.seedOnce(db)
}
