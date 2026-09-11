package com.example.ui.home

import com.example.data.models.Banner
import com.example.data.models.Book
import com.example.data.models.Post
import com.example.data.models.Video

sealed class FeedItem {
    data class BannerItem(val banners: List<Banner>) : FeedItem()
    data class PostItem(val post: Post) : FeedItem()
    data class VideoItem(val video: Video) : FeedItem()
    data class BookItem(val book: Book) : FeedItem()
}
