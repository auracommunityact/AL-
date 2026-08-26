package com.example.utils

import com.example.ui.study.ParsedAnalysisResult
import com.example.ui.study.ParsedSubjectScore
import com.google.mlkit.vision.text.Text

object ResultParser {

    fun parse(text: Text): ParsedAnalysisResult {
        val fullText = text.text
        val blocks = text.textBlocks
        val lines = blocks.flatMap { it.lines }
        
        val studentName = extractField(fullText, listOf("Name", "Student Name", "Name of Student", "Candidate's Name"))
        val rollNumber = extractField(fullText, listOf("Roll No", "Roll Number", "Enrollment No", "Seat No", "Reg No"))
        val schoolName = extractField(fullText, listOf("School", "Institution", "School Name", "College Name"))
        val board = detectBoard(fullText)
        val className = extractField(fullText, listOf("Class", "Grade", "Standard", "Sem"))
        
        val subjects = extractSubjectsSpatially(lines)
        
        val totalMaxMarks = subjects.sumOf { it.maxMarks ?: 100.0 }
        val obtainedMarks = subjects.sumOf { it.marks ?: 0.0 }
        val percentage = if (totalMaxMarks > 0) (obtainedMarks / totalMaxMarks) * 100.0 else 0.0
        
        val passFail = if (percentage >= 33.0) "Pass" else "Fail"
        val grade = calculateGrade(percentage)
        
        val strengths = subjects.filter { (it.marks ?: 0.0) / (it.maxMarks ?: 100.0) >= 0.75 }
            .joinToString(", ") { it.name }
        val weaknesses = subjects.filter { (it.marks ?: 0.0) / (it.maxMarks ?: 100.0) < 0.45 }
            .joinToString(", ") { it.name }
            
        val summary = generateSummary(percentage, grade, subjects)
        val studyPlan = generateStudyPlan(weaknesses)
        val suggestions = generateSuggestions(weaknesses)

        return ParsedAnalysisResult(
            studentName = studentName ?: "Student Name",
            rollNumber = rollNumber,
            board = board ?: "Academic Board",
            className = className ?: "Standard Profile",
            schoolName = schoolName ?: "Educational Institution",
            subjects = subjects,
            totalMarks = totalMaxMarks,
            obtainedMarks = obtainedMarks,
            percentage = percentage,
            grade = grade,
            passFail = passFail,
            performanceSummary = summary,
            strengths = if (strengths.isNotEmpty()) "Strong performance in $strengths. These subjects show high conceptual clarity." else "Balanced distribution of scores across the curriculum.",
            weaknesses = if (weaknesses.isNotEmpty()) "Improvement required in $weaknesses. Focus on basic fundamentals and problem-solving." else "No significant weak areas identified in this scan.",
            improvementSuggestions = suggestions,
            personalizedStudyPlan = studyPlan,
            motivationalFeedback = generateMotivationalFeedback(percentage)
        )
    }

    private fun extractSubjectsSpatially(lines: List<Text.Line>): List<ParsedSubjectScore> {
        val subjects = mutableListOf<ParsedSubjectScore>()
        
        // Group lines by vertical proximity (rows)
        val rows = mutableListOf<MutableList<Text.Line>>()
        val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
        
        for (line in sortedLines) {
            val top = line.boundingBox?.top ?: 0
            val height = line.boundingBox?.height() ?: 20
            
            val matchingRow = rows.find { row ->
                val rowTop = row.first().boundingBox?.top ?: 0
                Math.abs(rowTop - top) < height / 2
            }
            
            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        val subjectRegex = Regex("(?i)^(?!TOTAL|GRADE|RESULT|NAME|ROLL|CLASS)([A-Za-z\\s&]{3,})")
        val marksRegex = Regex("(\\d{1,3})(?:\\s*/?\\s*(\\d{2,3}))?")

        for (row in rows) {
            val sortedRow = row.sortedBy { it.boundingBox?.left ?: 0 }
            val rowText = sortedRow.joinToString(" ") { it.text }
            
            val subjectMatch = subjectRegex.find(rowText)
            if (subjectMatch != null) {
                val name = subjectMatch.groupValues[1].trim()
                if (isNoise(name)) continue
                
                val marksMatch = marksRegex.findAll(rowText).lastOrNull()
                if (marksMatch != null) {
                    val marks = marksMatch.groupValues[1].toDoubleOrNull()
                    val maxMarks = marksMatch.groupValues[2].toDoubleOrNull() ?: 100.0
                    if (marks != null && marks <= maxMarks) {
                        subjects.add(ParsedSubjectScore(name, marks, maxMarks))
                    }
                }
            }
        }
        
        return if (subjects.isEmpty()) extractSubjectsFallback(lines) else subjects
    }

    private fun extractSubjectsFallback(lines: List<Text.Line>): List<ParsedSubjectScore> {
        val subjects = mutableListOf<ParsedSubjectScore>()
        val subjectRegex = Regex("^([A-Za-z\\s&]{3,})\\s+(\\d+)(?:\\s+(\\d+))?")
        
        for (line in lines) {
            val text = line.text.trim()
            val match = subjectRegex.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (isNoise(name)) continue
                val marks = match.groupValues[2].toDoubleOrNull()
                val maxMarks = match.groupValues[3].toDoubleOrNull() ?: 100.0
                if (marks != null) {
                    subjects.add(ParsedSubjectScore(name, marks, maxMarks))
                }
            }
        }
        return subjects
    }

    private fun extractField(fullText: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            val regex = Regex("(?i)$keyword[:\\s-]+([^\\n]+)")
            val match = regex.find(fullText)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun detectBoard(fullText: String): String? {
        val boards = listOf("CBSE", "ICSE", "ISC", "NIOS", "MSBSHSE", "UP BOARD", "BIHAR BOARD", "WBBSE", "UPMSP", "BSEB")
        for (board in boards) {
            if (fullText.contains(board, ignoreCase = true)) return board
        }
        return if (fullText.contains("Board", ignoreCase = true)) "State Board" else null
    }

    private fun isNoise(text: String): Boolean {
        val noise = listOf("NAME", "ROLL", "SCHOOL", "TOTAL", "PERCENTAGE", "GRADE", "RESULT", "MARK", "SUBJECT", "MARKS", "OBTAINED")
        return noise.any { text.equals(it, ignoreCase = true) }
    }

    private fun calculateGrade(percentage: Double): String {
        return when {
            percentage >= 90 -> "A+"
            percentage >= 80 -> "A"
            percentage >= 70 -> "B+"
            percentage >= 60 -> "B"
            percentage >= 50 -> "C"
            percentage >= 40 -> "D"
            else -> "E"
        }
    }

    private fun generateSummary(percentage: Double, grade: String, subjects: List<ParsedSubjectScore>): String {
        val total = subjects.size
        return "You have analyzed a report card with $total subjects. Your overall percentage is ${String.format("%.2f", percentage)}%, which corresponds to a '$grade' grade. " +
               if (percentage >= 75) "Great job maintaining a high standard!" else "There's significant room for improvement to reach the next tier."
    }

    private fun generateStudyPlan(weaknesses: String): String {
        if (weaknesses.isEmpty()) return "Continue with your current routine. Dedicate 1 hour daily to revision of all subjects to maintain your grades."
        
        val weakList = weaknesses.split(", ")
        val plan = StringBuilder("Focus Study Plan:\n")
        weakList.forEach { subject ->
            plan.append("- ${subject.uppercase()}: 2 hours daily focusing on fundamental concepts and practice questions.\n")
        }
        plan.append("- OTHERS: 1 hour daily for regular assignments.")
        return plan.toString()
    }

    private fun generateSuggestions(weaknesses: String): String {
        if (weaknesses.isEmpty()) return "Review advanced topics in your strongest subjects to stay ahead. Participate in Olympiads or competitions."
        return "1. Take chapter-wise tests for $weaknesses.\n2. Use Aura's Video Lectures to clarify doubts in these areas.\n3. Practice previous year question papers."
    }

    private fun generateMotivationalFeedback(percentage: Double): String {
        return when {
            percentage >= 90 -> "Outstanding! You are among the top performers. Keep the momentum going! ✨"
            percentage >= 75 -> "Well done! You have a solid foundation. With a bit more effort, you can hit the 90s! 🚀"
            percentage >= 50 -> "Good effort. You're on the right track, but consistency is key to reaching the top. 📈"
            else -> "Don't be discouraged. Every expert was once a beginner. Start small, stay consistent, and you will see progress! 💪"
        }
    }
}
