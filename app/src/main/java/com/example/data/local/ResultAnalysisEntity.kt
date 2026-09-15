package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.study.ParsedAnalysisResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "result_analysis_cache")
data class ResultAnalysisEntity(
    @PrimaryKey
    val imageUri: String,
    val resultJson: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toParsedResult(): ParsedAnalysisResult {
        return Json.decodeFromString(resultJson)
    }

    companion object {
        fun fromParsedResult(uri: String, result: ParsedAnalysisResult): ResultAnalysisEntity {
            return ResultAnalysisEntity(
                imageUri = uri,
                resultJson = Json.encodeToString(result)
            )
        }
    }
}
