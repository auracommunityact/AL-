package com.example.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.Book

@Composable
fun PlayStoreBookItem(book: Book, onBookClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onBookClick),
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = book.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" },
            contentDescription = book.bookName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = book.bookName, style = MaterialTheme.typography.bodyMedium, maxLines = 2, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlayStoreBookListItem(book: Book, onBookClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onBookClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" },
            contentDescription = book.bookName,
            modifier = Modifier
                .size(60.dp)
                .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = book.bookName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = book.subject, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ShelfRow(
    books: List<Book>,
    savedBookIds: List<String>,
    offlineBookIds: List<String>,
    downloadProgress: Map<String, Int>,
    onBookClick: (Book) -> Unit,
    onToggleSave: (String) -> Unit,
    onDownloadBook: (Book) -> Unit,
    onDeleteOfflineBook: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Fill empty slots so the shelf always has 3 spaces
            for (i in 0 until 3) {
                if (i < books.size) {
                    val book = books[i]
                    BookItem(
                        book = book,
                        isSaved = savedBookIds.contains(book.id),
                        isDownloaded = offlineBookIds.contains(book.id),
                        downloadProgress = downloadProgress[book.id],
                        onClick = { onBookClick(book) },
                        onToggleSave = { onToggleSave(book.id) },
                        onDownloadBook = { onDownloadBook(book) },
                        onDeleteOfflineBook = { onDeleteOfflineBook(book.id) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                }
            }
        }
        
        // Wooden shelf visual representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 16.dp)
                .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.extraSmall)
                .background(Color(0xFF8B5A2B)) // Wood color
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(horizontal = 20.dp)
                .background(Color(0xFF5C3A21)) // Darker shade for depth
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BookItem(
    book: Book,
    isSaved: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onDownloadBook: () -> Unit,
    onDeleteOfflineBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.small)
        ) {
            AsyncImage(
                model = book.coverImage.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=300&q=80" },
                contentDescription = book.bookName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Save/Bookmark Button
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Book",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Download Button / Progress / Delete
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    .padding(4.dp)
            ) {
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                } else if (isDownloaded) {
                    IconButton(
                        onClick = onDeleteOfflineBook,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DownloadDone,
                            contentDescription = "Downloaded - Click to Delete",
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDownloadBook,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download Book",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
