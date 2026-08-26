package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.Video
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Entity(tableName = "cached_videos")
data class CachedVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val className: String,
    val subject: String,
    val thumbnail: String,
    val videoUrl: String,
    val youtubeVideoId: String,
    val chapter: String,
    val partNumber: Int,
    val teacher: String,
    val duration: String,
    val videoOrder: Int,
    val relatedBooksJson: String,
    val cachedAt: Long
) {
    fun toVideo() = Video(
        id = id,
        title = title,
        description = description,
        className = className,
        subject = subject,
        thumbnail = thumbnail,
        videoUrl = videoUrl,
        youtubeVideoId = youtubeVideoId,
        chapter = chapter,
        partNumber = partNumber,
        teacher = teacher,
        duration = duration,
        order = videoOrder,
        relatedBooks = try { Json.decodeFromString(relatedBooksJson) } catch (e: Exception) { emptyList() },
        createdAt = cachedAt
    )

    companion object {
        fun fromVideo(video: Video) = CachedVideoEntity(
            id = video.id,
            title = video.title,
            description = video.description,
            className = video.className,
            subject = video.subject,
            thumbnail = video.thumbnail,
            videoUrl = video.videoUrl,
            youtubeVideoId = video.youtubeVideoId,
            chapter = video.chapter,
            partNumber = video.partNumber,
            teacher = video.teacher,
            duration = video.duration,
            videoOrder = video.order,
            relatedBooksJson = try { Json.encodeToString(video.relatedBooks) } catch (e: Exception) { "[]" },
            cachedAt = if (video.createdAt > 0L) video.createdAt else System.currentTimeMillis()
        )
    }
}
