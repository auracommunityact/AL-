with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "r") as f:
    content = f.read()

target = """    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null
        fun getDatabase(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }"""

replacement = """    companion object {
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_videos ADD COLUMN downloadEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cached_videos ADD COLUMN authorizedDownloadUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: PlannerDatabase? = null
        fun getDatabase(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_database"
                )
                .addMigrations(MIGRATION_13_14)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }"""

import re
content = re.sub(r'    companion object \{.*\}', replacement, content, flags=re.DOTALL)
with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "w") as f:
    f.write(content)
