package com.example.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val photoUrl: String,
    val points: Int,
    val rank: Int = 0,
    val level: Int = 1
)

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val category: String // "Learning", "Consistency", "Social", "Expertise"
)

object AchievementSystem {
    val BADGES = listOf(
        Badge("first_lesson", "Fast Learner", "Completed your first video lesson", "ic_badge_lesson", "Learning"),
        Badge("quiz_master", "Quiz Master", "Scored 100% on a quiz", "ic_badge_quiz", "Expertise"),
        Badge("consistent_learner", "Consistent Learner", "Maintained a 7-day study streak", "ic_badge_streak", "Consistency"),
        Badge("book_worm", "Book Worm", "Read 5 different books", "ic_badge_book", "Learning"),
        Badge("points_1000", "Points Millionaire", "Earned over 1000 points", "ic_badge_points", "Social")
    )

    fun getBadgeByName(name: String) = BADGES.find { it.name == name }
}
