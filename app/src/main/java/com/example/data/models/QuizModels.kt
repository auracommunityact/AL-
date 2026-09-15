package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val className: String = "",
    val subject: String = "",
    val associatedId: String = "", // e.g., video ID or book ID
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)

@Serializable
data class QuizQuestion(
    val id: String = "",
    val quizId: String = "",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val explanation: String = "",
    val order: Int = 0
)

@Serializable
data class QuizResult(
    val id: String = "",
    val quizId: String = "",
    val userId: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = 0L
)
