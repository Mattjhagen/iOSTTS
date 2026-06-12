package com.metroreader.app.parser

import android.content.Context
import android.net.Uri
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.domain.model.Book
import com.metroreader.app.domain.model.BookContent
import java.io.File

/**
 * Result of a book import operation.
 */
sealed class ImportResult {
    data class Success(val book: Book) : ImportResult()
    data class Error(val message: String, val cause: Throwable? = null) : ImportResult()
}

/**
 * Common interface for all book parsers.
 */
interface BookParser {
    suspend fun parse(file: File, context: Context): ImportResult
    suspend fun extractContent(file: File, context: Context): BookContent
    fun supportsFormat(format: BookFormat): Boolean
}

/**
 * Detects book format from file extension or MIME type.
 */
object BookFormatDetector {
    fun detect(uri: Uri, mimeType: String? = null): BookFormat {
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith(".epub") || mimeType == "application/epub+zip" -> BookFormat.EPUB
            path.endsWith(".pdf")  || mimeType == "application/pdf"       -> BookFormat.PDF
            path.endsWith(".mobi")                                         -> BookFormat.MOBI
            path.endsWith(".azw3") || path.endsWith(".azw")               -> BookFormat.AZW3
            path.endsWith(".txt")  || mimeType == "text/plain"            -> BookFormat.TXT
            path.endsWith(".html") || path.endsWith(".htm")
                || mimeType == "text/html"                                 -> BookFormat.HTML
            else                                                           -> BookFormat.UNKNOWN
        }
    }

    fun detect(file: File): BookFormat {
        return when (file.extension.lowercase()) {
            "epub"        -> BookFormat.EPUB
            "pdf"         -> BookFormat.PDF
            "mobi"        -> BookFormat.MOBI
            "azw3", "azw" -> BookFormat.AZW3
            "txt"         -> BookFormat.TXT
            "html", "htm" -> BookFormat.HTML
            else          -> BookFormat.UNKNOWN
        }
    }
}

/**
 * Utility to split text into sentences for TTS synchronization.
 */
object SentenceSplitter {
    private val sentencePattern = Regex("""(?<=[.!?])\s+(?=[A-Z"'])""")

    fun split(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val sentences = mutableListOf<String>()
        var remaining = text.trim()
        val parts = sentencePattern.split(remaining)
        parts.forEach { part ->
            val trimmed = part.trim()
            if (trimmed.isNotEmpty()) sentences.add(trimmed)
        }
        return sentences.ifEmpty { listOf(text.trim()) }
    }
}
