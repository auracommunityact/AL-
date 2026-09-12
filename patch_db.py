with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.data.local.notifications.NotificationEntity", 
"import com.example.data.local.notifications.NotificationEntity\nimport com.example.ai.memory.ChatMessageEntity\nimport com.example.ai.memory.AiMemoryDao")

content = content.replace("ResultAnalysisEntity::class, RecentSearchEntity::class]", 
"ResultAnalysisEntity::class, RecentSearchEntity::class, ChatMessageEntity::class]")

content = content.replace("version = 14", "version = 15")

content = content.replace("abstract fun recentSearchDao(): RecentSearchDao", 
"abstract fun recentSearchDao(): RecentSearchDao\n    abstract fun aiMemoryDao(): AiMemoryDao")

content = content.replace("val MIGRATION_13_14 = object : Migration(13, 14)", 
"""val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14)""")

content = content.replace(".addMigrations(MIGRATION_13_14)", ".addMigrations(MIGRATION_13_14, MIGRATION_14_15)")

with open("app/src/main/java/com/example/data/local/PlannerDatabase.kt", "w") as f:
    f.write(content)
