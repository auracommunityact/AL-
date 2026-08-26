package com.example.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bannerUrl: String = "",
    val provider: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L,
    val role: String = "user",
    val savedBooks: List<String> = emptyList(),
    val savedVideos: List<String> = emptyList(),
    val selectedGrade: String = "All Grades",
    
    // Academic Information
    val studentId: String = "",
    val isVerified: Boolean = false,
    val schoolName: String = "",
    val className: String = "",
    val section: String = "",
    val rollNumber: String = "",
    val admissionNumber: String = "",
    val board: String = "",
    val medium: String = "",
    val academicSession: String = "",

    // Personal Information
    val gender: String = "",
    val dob: String = "",
    val age: Int = 0,
    val bloodGroup: String = "",
    val nationality: String = "",
    val category: String = "",

    // Contact Information
    val mobileNumber: String = "",
    val parentName: String = "",
    val parentMobileNumber: String = "",

    // Address
    val country: String = "",
    val state: String = "",
    val district: String = "",
    val city: String = "",
    val pinCode: String = "",

    // Learning Information
    val currentQuestionPapers: List<String> = emptyList(),
    val certificatesEarned: List<String> = emptyList(),
    val studyStreak: Int = 0,
    val totalStudyTime: Int = 0,
    val completedLessons: Int = 0,

    // Achievements
    val badges: List<String> = emptyList(),
    val rank: String = "Bronze Starter",
    val points: Int = 100,
    val level: Int = 1,
    val attendancePercentage: Double = 95.0,

    // Account Information
    @Serializable(with = TimestampSerializer::class)
    val lastLogin: Long = 0L,
    val accountStatus: String = "Active",
    val bio: String = "",
    val isPremium: Boolean = false,
    val deviceInfo: String = "",
    val appVersion: String = ""
)

@Serializable
data class Book(
    @Serializable(with = StringOrNumericSerializer::class)
    val id: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val bookName: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val className: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val subject: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val description: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val coverImage: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val pdfUrl: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class Video(
    @Serializable(with = StringOrNumericSerializer::class)
    val id: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val title: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val description: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val className: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val subject: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val thumbnail: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val videoUrl: String = "", // Used as youtubeUrl
    @Serializable(with = SafeStringSerializer::class)
    val youtubeVideoId: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val chapter: String = "",
    @Serializable(with = SafeIntSerializer::class)
    val partNumber: Int = 1,
    @Serializable(with = SafeStringSerializer::class)
    val teacher: String = "",
    @Serializable(with = SafeStringSerializer::class)
    val duration: String = "",
    @Serializable(with = SafeIntSerializer::class)
    val order: Int = 0,
    @Serializable(with = SafeStringListSerializer::class)
    val relatedBooks: List<String> = emptyList(),
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class Banner(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val link: String = "",
    val ctaText: String = "Explore Now",
    val backgroundColor: String = "#6200EE", // Hex or Gradient string
    val order: Int = 0,
    val isEnabled: Boolean = true,
    @Serializable(with = TimestampSerializer::class)
    val startDate: Long = 0L,
    @Serializable(with = TimestampSerializer::class)
    val endDate: Long = 0L,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val isEnabled: Boolean = true,
    @Serializable(with = TimestampSerializer::class)
    val scheduledAt: Long = 0L,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class HomeSectionConfig(
    val id: String = "",
    val type: String = "", // "books", "videos", "question_papers", "websites", "exams", "trending", "recommended", "announcements"
    val title: String = "",
    val icon: String = "",
    val isVisible: Boolean = true,
    val order: Int = 0
)

@Serializable
data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val associatedId: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class FlashcardDeck(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val className: String = "",
    val subject: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class Flashcard(
    val id: String = "",
    val deckId: String = "",
    val frontText: String = "",
    val backText: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class VideoProgress(
    val id: String = "",
    val userId: String = "",
    val videoId: String = "",
    val isWatched: Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    val lastWatchedAt: Long = 0L
)

@Serializable
data class BookProgress(
    val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val lastPage: Int = 0,
    @Serializable(with = TimestampSerializer::class)
    val lastReadAt: Long = 0L
)

@Serializable
data class QuestionPaper(
    @Serializable(with = StringOrNumericSerializer::class)
    val id: String = "",
    val className: String = "",
    val subject: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val section: String = "",
    val board: String = "",
    val year: String = "",
    val pdfUrl: String = "",
    val fileSize: String = "",
    val totalPages: Int = 0,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class QuestionPaperSection(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class Website(
    @Serializable(with = StringOrNumericSerializer::class)
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val logo: String = "",
    val url: String = "",
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)
