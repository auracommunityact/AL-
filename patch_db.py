with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "r") as f:
    content = f.read()

content = content.replace("version = 15", "version = 16")

migration_14_15 = """        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }"""

migration_15_16 = """        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_chat_messages ADD COLUMN imageUri TEXT")
            }
        }"""

content = content.replace(migration_14_15, migration_15_16)
content = content.replace(".addMigrations(MIGRATION_13_14, MIGRATION_14_15)", ".addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)")

with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "w") as f:
    f.write(content)
