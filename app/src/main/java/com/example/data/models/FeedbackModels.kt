package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Feedback(
    val id: String = "",
    val userId: String,
    val userName: String,
    val userEmail: String,
    val category: String,
    val subject: String,
    val description: String,
    val requestedFeatureName: String? = null,
    val screenshotUrl: String? = null,
    val deviceInfo: String,
    val appVersion: String,
    val status: String = "New",
    val upvotes: Int = 0,
    val upvotedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class AppFeature(
    val id: String,
    val name: String,
    val description: String,
    val route: String,
    val category: String = "General"
)

object FeedbackConstants {
    val CATEGORIES = listOf(
        "Bug Report",
        "App Feedback",
        "Improvement Suggestion",
        "Feature Request",
        "UI/UX Feedback",
        "Other"
    )

    val STATUS_OPTIONS = listOf(
        "New",
        "Under Review",
        "Planned",
        "In Progress",
        "Completed",
        "Rejected"
    )
}

object AppFeatures {
    val FEATURES = listOf(
        AppFeature("home", "Home", "Main dashboard with recent content", "home"),
        AppFeature("papers", "Question Papers", "Previous year papers and mock tests", "questionPapers"),
        AppFeature("videos", "Videos", "Video lessons and educational content", "videos"),
        AppFeature("books", "Books", "Digital library of books and notes", "books"),
        AppFeature("chat", "Chat", "Connect with teachers and peers", "chat_list"),
        AppFeature("planner", "Study Planner", "Organize your study schedule", "study_planner"),
        AppFeature("countdown", "Exam Countdown", "Track exam dates", "exam_countdown"),
        AppFeature("pdf", "PDF Tool", "Read and build PDF documents", "pdf_tool"),
        AppFeature("translate", "Notes Translate", "Translate study material", "notes_translate"),
        AppFeature("calculator", "Scientific Calculator", "Complex math calculations", "calculator"),
        AppFeature("analysis", "Result Analysis", "Analyze test results", "result_analysis"),
        AppFeature("tracker", "Progress Tracker", "Track syllabus progress", "progress"),
        AppFeature("report", "Weekly Report", "Performance insights", "weekly_report"),
        AppFeature("map", "Map Agent", "Learn geography and history", "map_agent"),
        AppFeature("ai_homework", "AI Homework Helper", "Step-by-step help", "ai_chat?prompt=Homework+Help"),
        AppFeature("ai_doubt", "AI Doubt Solver", "Clear your syllabus doubts", "ai_chat?prompt=Doubt+Solving"),
        AppFeature("leaderboard", "Leaderboard", "Compete with other students", "leaderboard")
    )
}
