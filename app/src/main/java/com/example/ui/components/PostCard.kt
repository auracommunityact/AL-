package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.models.Post

@Composable
fun PostCard(post: Post, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (!post.image_url.isNullOrBlank()) {
                AsyncImage(
                    model = post.image_url,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (!post.youtube_video_id.isNullOrBlank() && onClick != null) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    YouTubePlayerComponent(videoId = post.youtube_video_id, videoUrl = post.youtube_url)
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (post.status.isNotBlank()) {
                     Spacer(modifier = Modifier.height(4.dp))
                     Text(
                         text = post.status.uppercase(),
                         style = MaterialTheme.typography.labelSmall,
                         color = if (post.status == "published") Color(0xFF10B981) else Color(0xFFF59E0B)
                     )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!post.youtube_video_id.isNullOrBlank() && onClick == null && post.image_url.isNullOrBlank()) {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        YouTubePlayerComponent(videoId = post.youtube_video_id, videoUrl = post.youtube_url)
                    }
                     Spacer(modifier = Modifier.height(8.dp))
                }
                
                PostDescriptionWithYouTube(description = post.description, isSummary = onClick != null)
            }
        }
    }
}

@Composable
fun PostDescriptionWithYouTube(description: String, isSummary: Boolean = false) {
    val youtubeRegex = Regex("""(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/(?:[^\/\n\s]+\/\S+\/|(?:v|e(?:mbed)?)\/|\S*?[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})""")
    
    val matches = youtubeRegex.findAll(description).toList()
    
    if (matches.isEmpty()) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCBD5E1),
            maxLines = if (isSummary) 4 else Int.MAX_VALUE,
            overflow = if (isSummary) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    } else {
        var lastIndex = 0
        Column {
            for (match in matches) {
                val textBefore = description.substring(lastIndex, match.range.first)
                if (textBefore.isNotBlank()) {
                    Text(
                        text = textBefore,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        maxLines = if (isSummary) 2 else Int.MAX_VALUE,
                        overflow = if (isSummary) TextOverflow.Ellipsis else TextOverflow.Clip
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val videoId = match.groupValues[1]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    YouTubePlayerComponent(videoId = videoId, videoUrl = null)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                lastIndex = match.range.last + 1
                
                if (isSummary) break // Only show the first video in summary view
            }
            
            if (lastIndex < description.length && !isSummary) {
                val textAfter = description.substring(lastIndex)
                if (textAfter.isNotBlank()) {
                    Text(
                        text = textAfter,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}
