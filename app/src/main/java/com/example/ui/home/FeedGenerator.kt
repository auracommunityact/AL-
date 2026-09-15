package com.example.ui.home

import com.example.data.models.Banner
import com.example.data.models.Book
import com.example.data.models.Post
import com.example.data.models.Video
import kotlin.random.Random

object FeedGenerator {
    fun generateMixedFeed(
        banners: List<Banner>,
        posts: List<Post>,
        videos: List<Video>,
        books: List<Book>
    ): List<FeedItem> {
        val newFeed = mutableListOf<FeedItem>()
        
        // Rule 1: Banner always first
        if (banners.isNotEmpty()) {
            newFeed.add(FeedItem.BannerItem(banners))
        }

        val shuffledPosts = posts.shuffled().toMutableList()
        val shuffledVideos = videos.shuffled().toMutableList()
        val shuffledBooks = books.shuffled().toMutableList()

        var consecutiveType = ""
        var consecutiveCount = 0

        while (shuffledPosts.isNotEmpty() || shuffledVideos.isNotEmpty() || shuffledBooks.isNotEmpty()) {
            val choices = mutableListOf<String>()
            
            // Rule 2 & 3 & 6: Balance and prevent long streaks
            if (shuffledPosts.isNotEmpty() && (consecutiveType != "post" || consecutiveCount < 2)) {
                choices.addAll(List(3) { "post" }) // weight
            }
            if (shuffledVideos.isNotEmpty() && (consecutiveType != "video" || consecutiveCount < 3)) {
                choices.addAll(List(4) { "video" }) // weight
            }
            if (shuffledBooks.isNotEmpty() && (consecutiveType != "book" || consecutiveCount < 1)) {
                choices.addAll(List(2) { "book" }) // weight
            }

            // Fallback if strict limits blocked all options but items still exist
            if (choices.isEmpty()) {
                 if (shuffledPosts.isNotEmpty()) choices.add("post")
                 if (shuffledVideos.isNotEmpty()) choices.add("video")
                 if (shuffledBooks.isNotEmpty()) choices.add("book")
            }
            
            if (choices.isEmpty()) break

            val pick = choices.random()
            
            if (pick == consecutiveType) {
                consecutiveCount++
            } else {
                consecutiveType = pick
                consecutiveCount = 1
            }

            when (pick) {
                "post" -> newFeed.add(FeedItem.PostItem(shuffledPosts.removeAt(0)))
                "video" -> newFeed.add(FeedItem.VideoItem(shuffledVideos.removeAt(0)))
                "book" -> newFeed.add(FeedItem.BookItem(shuffledBooks.removeAt(0)))
            }
        }
        
        return newFeed
    }
}
