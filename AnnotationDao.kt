package com.metroreader.app.data.local.dao

import androidx.room.*
import com.metroreader.app.data.local.entity.BookmarkEntity
import com.metroreader.app.data.local.entity.HighlightEntity
import com.metroreader.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {

    // ── Bookmarks ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    // ── Highlights ────────────────────────────────────────────────────────────
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY chapterIndex, startOffset")
    fun getHighlights(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND chapterIndex = :chapter ORDER BY startOffset")
    suspend fun getHighlightsForChapter(bookId: Long, chapter: Int): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Update
    suspend fun updateHighlight(highlight: HighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    // ── Notes ─────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getNotes(bookId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
