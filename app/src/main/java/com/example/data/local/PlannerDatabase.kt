package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.data.local.notifications.NotificationDao
import com.example.data.local.notifications.NotificationEntity

@Database(entities = [StudySession::class, PdfAnnotation::class, PdfBookmark::class, OfflineBook::class, CachedBookEntity::class, CachedMaterialEntity::class, CachedVideoEntity::class, NotificationEntity::class, CalculatorHistoryEntity::class, ExamDateSheetEntity::class, NoteEntity::class, WebsiteReaderEntity::class, WebsiteChatEntity::class, ResultAnalysisEntity::class], version = 12, exportSchema = false)
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

    companion object {
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
    }
}
