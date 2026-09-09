package com.example.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object AuraLocalAITutor {

    fun generateStudySchedule(grade: String, subjects: List<String>): Flow<String> = flow {
        val header = "## \uD83D\uDCC5 Personalized Weekly Study Schedule\n**Grade:** $grade\n**Subjects:** ${subjects.joinToString(", ")}\n\n"
        emit(header)
        delay(400) // Simulate model loading
        
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val timeSlots = listOf("4:00 PM - 5:00 PM", "5:15 PM - 6:15 PM", "6:30 PM - 7:30 PM")
        
        val allContent = buildString {
            append("Here is an optimized study plan tailored for your syllabus. I have balanced the subjects to ensure steady progress.\n\n")
            days.forEachIndexed { dayIndex, day ->
                append("### $day\n")
                timeSlots.forEachIndexed { index, time ->
                    val subject = if (subjects.isNotEmpty()) {
                        subjects[(index + dayIndex) % subjects.size]
                    } else "General Study"
                    
                    val task = when (index) {
                        0 -> "Review textbook chapter and core concepts."
                        1 -> "Practice MCQs and standard questions."
                        else -> "Revision and homework."
                    }
                    append("- **$time**: $subject - $task\n")
                }
                append("\n")
            }
            append("### Sunday\n- **10:00 AM - 12:00 PM**: Weekly Revision & Mock Test.\n- **Evening**: Rest and prepare for the upcoming week.\n\n")
            append("---\n*Generated locally by Aura AI. (Offline Mode)*")
        }
        
        val chunkSize = 12
        var currentIndex = 0
        while (currentIndex < allContent.length) {
            val chunk = allContent.substring(currentIndex, minOf(currentIndex + chunkSize, allContent.length))
            emit(chunk)
            currentIndex += chunkSize
            delay(40) 
        }
    }

    fun generateChatResponse(prompt: String, useRag: Boolean): Flow<String> = flow {
        delay(600) 
        
        val retrievedContext = if (useRag && (prompt.contains("science", ignoreCase = true) || prompt.contains("chapter", ignoreCase = true))) {
            "RETRIEVED LOCAL CONTENT: [Class 10 Science, Chapter 3 - Metals and Non-metals. Metals are electropositive in nature. Non-metals are electronegative.]\n"
        } else ""

        val responseContent = buildString {
            if (retrievedContext.isNotEmpty()) {
                append("Based on the Aura Learning syllabus:\n\n")
            }
            if (prompt.contains("science", ignoreCase = true) && prompt.contains("chapter 3", ignoreCase = true)) {
                append("Chapter 3 of Class 10 Science covers **Metals and Non-metals**.\n\n- **Metals** are materials that are generally hard, malleable, ductile, and good conductors of heat and electricity.\n- **Non-metals** lack these properties and are generally electronegative.")
            } else if (prompt.contains("math", ignoreCase = true)) {
                append("Here is the step-by-step mathematical solution:\n\n1. Identify the given values.\n2. Apply the relevant formula.\n3. Substitute the values to find the answer.")
            } else {
                append("I am Aura AI, running locally on your device. To provide the best answer, please specify your subject or chapter so I can search your local books and videos!")
            }
        }
        
        val chunkSize = 4
        var currentIndex = 0
        while (currentIndex < responseContent.length) {
            val chunk = responseContent.substring(currentIndex, minOf(currentIndex + chunkSize, responseContent.length))
            emit(chunk)
            currentIndex += chunkSize
            delay(30)
        }
    }
}
