package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {
    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE relatedUrl = :url ORDER BY timestamp DESC")
    suspend fun getNotesForUrl(url: String): List<NoteEntity>
}
