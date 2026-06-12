package com.metroreader.app.domain.model

/**
 * Represents a single chapter or section of a book.
 */
data class Chapter(
    val index: Int,
    val title: String,
    val content: String,          // Plain text content for TTS
    val htmlContent: String = "", // HTML content for rich rendering
    val startPage: Int = 0,
    val endPage: Int = 0,
    val sentences: List<String> = emptyList(), // Pre-split sentences for TTS sync
)

/**
 * Full parsed book content.
 */
data class BookContent(
    val bookId: Long,
    val chapters: List<Chapter>,
    val tableOfContents: List<TocEntry> = emptyList(),
)

data class TocEntry(
    val title: String,
    val chapterIndex: Int,
    val level: Int = 0,
    val href: String = "",
)

/**
 * Reading position within a book.
 */
data class ReadingPosition(
    val bookId: Long,
    val chapterIndex: Int = 0,
    val characterOffset: Int = 0,
    val scrollOffset: Float = 0f,
    val progressPercent: Float = 0f,
)

/**
 * A text range used for highlighting/TTS sync.
 */
data class TextRange(
    val start: Int,
    val end: Int,
    val chapterIndex: Int = 0,
)
