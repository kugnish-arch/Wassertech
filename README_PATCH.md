# Patch Step 1 — DB v6 migration & dynamic component templates (data layer)

Apply the following changes in your project:

## 1) Add new files
- `app/src/main/java/com/example/wassertech/data/entities/ComponentTemplateEntity.kt`
- `app/src/main/java/com/example/wassertech/data/dao/ComponentTemplatesDao.kt`
- `app/src/main/java/com/example/wassertech/data/migrations/Migration_5_6.kt`

## 2) Replace existing files with provided updated versions
- `app/src/main/java/com/example/wassertech/data/entities/ComponentEntity.kt`
- `app/src/main/java/com/example/wassertech/data/entities/ChecklistTemplateEntity.kt`

## 3) Update AppDatabase.kt
- Increase version to **6** in the `@Database` annotation.
- Include `ComponentTemplateEntity::class` in the entities list.
- Add `abstract fun componentTemplatesDao(): ComponentTemplatesDao`.
- In the builder chain, add the new migration:
  ```kotlin
  .addMigrations(MIGRATION_2_3, MIGRATION_5_6) // keep existing ones, append MIGRATION_5_6
  ```
  Import: `import com.example.wassertech.data.migrations.MIGRATION_5_6`

> If you have more migrations (e.g. MIGRATION_1_2, _3_4, _4_5), keep them intact and append MIGRATION_5_6.

## 4) Rebuild to generate Room schema v6
On first build, Room will emit a new schema file at:
`app/schemas/com.example.wassertech.data.AppDatabase/6.json`

## 5) Notes
- We keep `ComponentEntity.type` for backward compatibility; new UI will start populating `templateId` and `paramsJson`.
- `ChecklistTemplateEntity` now can bind directly to a `componentTemplateId`; UI logic will use it first, fallback to enum when null.
- Foreign keys are not enforced yet; logical linkage suffices for now and simplifies migration on existing databases.
