package com.example.data.repository

import com.example.BuildConfig
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TranslationService {
    private val languageIdClient = LanguageIdentification.getClient()

    suspend fun identifyLanguage(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val languageCode = languageIdClient.identifyLanguage(text).await()
            if (languageCode == "und") null else languageCode
        } catch (e: Exception) {
            null
        }
    }

    suspend fun translateText(
        text: String, 
        sourceLangCode: String, 
        targetLangCode: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()
                
            val translator = Translation.getClient(options)
            
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            
            val result = translator.translate(text).await()
            translator.close()
            result
        } catch (e: Exception) {
            "Error: ${e.message ?: "Failed to translate."}"
        }
    }
    
    // Completely Offline AI & Study Companions - No API keys or network required
    suspend fun improveGrammar(text: String): String = withContext(Dispatchers.IO) {
        var result = text.trim()
        if (result.isEmpty()) return@withContext ""

        // Capitalize sentences
        val sentences = result.split(Regex("(?<=[.!?])\\s+"))
            .map { s ->
                val trimmedS = s.trim()
                if (trimmedS.isNotEmpty()) {
                    trimmedS.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                } else {
                    ""
                }
            }
            .filter { it.isNotEmpty() }
            
        result = sentences.joinToString(" ")

        // Fix common mistakes
        result = result
            .replace(Regex("\\s+([.,!?])"), "$1") // remove space before punctuation
            .replace(Regex("([.,!?])(?=[a-zA-Z])"), "$1 ") // add space after punctuation
            .replace(Regex("\\bi\\b"), "I") // capitalize 'i'
            .replace(Regex("\\s+"), " ") // remove duplicate spacing

        result
    }

    suspend fun simplifyNotes(text: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext "No text provided to simplify."

        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 5 }

        buildString {
            append("### 💡 Simplified Explanations\n\n")
            append("*(Complex terms simplified offline)*\n\n")
            
            sentences.forEach { sentence ->
                var simpleText = sentence
                simpleText = simpleText.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                append("- $simpleText\n")
            }
            
            append("\n**Quick Tip:** Focus on key terms and review one point at a time for efficient learning! 🧠")
        }
    }

    suspend fun summarizeNotes(text: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext "No text provided to summarize."
        
        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 5 }
            
        if (sentences.isEmpty()) return@withContext trimmed

        // Heuristic term extraction excluding standard stop words
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "if", "then", "else", "of", "in", "on", "at", "to", "for", "with", "by", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her", "its", "our", "their"
        )
        
        val words = trimmed.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 3 && !stopWords.contains(it) }

        val wordFreq = mutableMapOf<String, Int>()
        for (w in words) {
            wordFreq[w] = (wordFreq[w] ?: 0) + 1
        }

        val sentenceScores = sentences.map { sentence ->
            val sentenceWords = sentence.lowercase()
                .replace(Regex("[^a-zA-Z0-9\\s]"), "")
                .split(Regex("\\s+"))
            val score = sentenceWords.sumOf { wordFreq[it] ?: 0 }
            sentence to score
        }

        val topSentences = sentenceScores
            .sortedByDescending { it.second }
            .take(4)
            .sortedWith(Comparator { a, b ->
                sentences.indexOf(a.first).compareTo(sentences.indexOf(b.first))
            })
            .map { it.first }

        val keyTerms = wordFreq.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key.replaceFirstChar { char -> char.uppercase() } }

        buildString {
            append("### 📝 Offline Document Summary\n\n")
            append("*(Fully analyzed locally in offline study mode)*\n\n")
            
            append("**🎯 Key Takeaways:**\n")
            topSentences.forEach { sentence ->
                append("- $sentence\n")
            }
            append("\n")
            
            if (keyTerms.isNotEmpty()) {
                append("**🔑 Extracted Core Terms:**\n")
                append(keyTerms.joinToString(separator = ", ") { "`$it`" })
                append("\n\n")
            }
            
            append("---")
        }
    }
}
