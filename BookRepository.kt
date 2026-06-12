package com.metroreader.app.data.repository

import com.metroreader.app.data.local.dao.AnnotationDao
import com.metroreader.app.data.local.dao.BookDao
import com.metroreader.app.data.local.dao.ReadingProgressDao
import com.metroreader.app.data.local.dao.TtsDao
import com.metroreader.app.data.local.entity.*
import com.metroreader.app.domain.model.Book
import com.metroreader.app.domain.model.ReadingPosition
import com.metroreader.app.domain.model.toDomain
import com.metroreader.app.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val annotationDao: AnnotationDao,
    private val ttsDao: TtsDao,
) {

    // ── Books ─────────────────────────────────────────────────────────────────
    fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<Book>> =
        bookDao.getRecentlyReadBooks(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchBooks(query: String): Flow<List<Book>> =
        bookDao.searchBooks(query).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getBookById(id: Long): Book? =
        bookDao.getBookById(id)?.toDomain()

    fun observeBook(id: Long): Flow<Book?> =
        bookDao.observeBookById(id).map { it?.toDomain() }

    suspend fun addBook(book: Book): Long =
        bookDao.insertBook(book.toEntity())

    suspend fun updateBook(book: Book) =
        bookDao.updateBook(book.toEntity())

    suspend fun deleteBook(id: Long) {
        bookDao.deleteBookById(id)
    }

    suspend fun markOpened(id: Long) =
        bookDao.updateLastOpened(id)

    // ── Reading Progress ──────────────────────────────────────────────────────
    suspend fun getProgress(bookId: Long): ReadingProgressEntity? =
        progressDao.getProgress(bookId)

    fun observeProgress(bookId: Long): Flow<ReadingProgressEntity?> =
        progressDao.observeProgress(bookId)

    suspend fun saveProgress(position: ReadingPosition) {
        progressDao.upsertProgress(
            ReadingProgressEntity(
                bookId = position.bookId,
                currentChapterIndex = position.chapterIndex,
                characterOffset = position.characterOffset,
                scrollOffset = position.scrollOffset,
                progressPercent = position.progressPercent,
            )
        )
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────
    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> =
        annotationDao.getBookmarks(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity): Long =
        annotationDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(id: Long) =
        annotationDao.deleteBookmarkById(id)

    // ── Highlights ────────────────────────────────────────────────────────────
    fun getHighlights(bookId: Long): Flow<List<HighlightEntity>> =
        annotationDao.getHighlights(bookId)

    suspend fun getHighlightsForChapter(bookId: Long, chapter: Int): List<HighlightEntity> =
        annotationDao.getHighlightsForChapter(bookId, chapter)

    suspend fun addHighlight(highlight: HighlightEntity): Long =
        annotationDao.insertHighlight(highlight)

    suspend fun deleteHighlight(highlight: HighlightEntity) =
        annotationDao.deleteHighlight(highlight)

    // ── Notes ─────────────────────────────────────────────────────────────────
    fun getNotes(bookId: Long): Flow<List<NoteEntity>> =
        annotationDao.getNotes(bookId)

    suspend fun addNote(note: NoteEntity): Long =
        annotationDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) =
        annotationDao.updateNote(note)

    suspend fun deleteNote(note: NoteEntity) =
        annotationDao.deleteNote(note)

    // ── TTS ───────────────────────────────────────────────────────────────────
    suspend fun getTtsProgress(bookId: Long): TtsProgressEntity? =
        ttsDao.getTtsProgress(bookId)

    suspend fun saveTtsProgress(progress: TtsProgressEntity) =
        ttsDao.upsertTtsProgress(progress)

    fun getAudioCache(bookId: Long): Flow<List<AudioCacheEntity>> =
        ttsDao.getAudioCache(bookId)

    suspend fun getChapterAudio(bookId: Long, chapter: Int): AudioCacheEntity? =
        ttsDao.getChapterAudio(bookId, chapter)

    suspend fun saveAudioCache(entry: AudioCacheEntity): Long =
        ttsDao.insertAudioCache(entry)

    suspend fun clearAudioCache(bookId: Long) =
        ttsDao.clearAudioCacheForBook(bookId)

    fun getTotalCacheSize(): Flow<Long?> =
        ttsDao.getTotalCacheSize()
}
