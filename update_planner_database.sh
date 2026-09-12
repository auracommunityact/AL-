sed -i 's/version = 13/version = 14/' app/src/main/java/com/example/data/local/PlannerDatabase.kt

sed -i '/import androidx.room.RoomDatabase/a import androidx.room.migration.Migration\nimport androidx.sqlite.db.SupportSQLiteDatabase' app/src/main/java/com/example/data/local/PlannerDatabase.kt

sed -i '/companion object {/a \        val MIGRATION_13_14 = object : Migration(13, 14) {\n            override fun migrate(database: SupportSQLiteDatabase) {\n                database.execSQL("ALTER TABLE cached_videos ADD COLUMN downloadEnabled INTEGER NOT NULL DEFAULT 0")\n                database.execSQL("ALTER TABLE cached_videos ADD COLUMN authorizedDownloadUrl TEXT NOT NULL DEFAULT \'\'")\n            }\n        }' app/src/main/java/com/example/data/local/PlannerDatabase.kt

sed -i 's/.fallbackToDestructiveMigration()/.addMigrations(MIGRATION_13_14)\n                .fallbackToDestructiveMigration()/' app/src/main/java/com/example/data/local/PlannerDatabase.kt
