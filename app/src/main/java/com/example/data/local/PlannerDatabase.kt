package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.data.local.notifications.NotificationDao
import com.example.data.local.notifications.NotificationEntity
import com.example.ai.memory.ChatMessageEntity
import com.example.ai.memory.AiMemoryDao

@Database(entities = [StudySession::class, PdfAnnotation::class, PdfBookmark::class, OfflineBook::class, CachedBookEntity::class, CachedMaterialEntity::class, CachedVideoEntity::class, NotificationEntity::class, CalculatorHistoryEntity::class, ExamDateSheetEntity::class, NoteEntity::class, WebsiteReaderEntity::class, WebsiteChatEntity::class, ResultAnalysisEntity::class, RecentSearchEntity::class, ChatMessageEntity::class], version = 15, exportSchema = false)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun pdfAnnotationDao(): PdfAnnotationDao
    abstract fun pdfBookmarkDao(): PdfBookmarkDao
    abstract fun offlineBookDao(): OfflineBookDao
    abstract fun cachedBookDao(): CachedBookDao
    abstract fun cachedMaterialDao(): CachedMaterialDao
    abstract fun cachedVideoDao(): CachedVideoDao
    abstract fun notificationDao(): NotificationDao
    abstract fun calculatorHistoryDao(): CalculatorHistoryDao
    abstract fun examDateSheetDao(): ExamDateSheetDao
    abstract fun noteDao(): NoteDao
    abstract fun websiteReaderDao(): WebsiteReaderDao
    abstract fun resultAnalysisDao(): ResultAnalysisDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun aiMemoryDao(): AiMemoryDao

    companion object {
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
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
                .addMigrations(MIGRATION_13_14, MIGRATION_14_15)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
