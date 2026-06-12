package com.metroreader.app.parser

import android.content.Context
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.domain.model.BookContent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads book content (chapters, TOC) on demand.
 * Uses a simple LRU-style in-memory cache for the currently open book.
 */
@Singleton
class ContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val txtParser: TxtParser,
    private val htmlParser: HtmlParser,
) {
    private var cachedBookId: Long = -1L
    private var cachedContent: BookContent? = null

    suspend fun loadContent(bookId: Long, filePath: String, format: BookFormat): BookContent? =
        withContext(Dispatchers.IO) {
            if (cachedBookId == bookId && cachedContent != null) {
                return@withContext cachedContent
            }

            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val content = when (format) {
                BookFormat.EPUB -> epubParser.extractContent(file, context)
                BookFormat.PDF  -> pdfParser.extractContent(file, context)
                BookFormat.TXT  -> txtParser.extractContent(file, context)
                BookFormat.HTML -> htmlParser.extractContent(file, context)
                else            -> null
            }?.copy(bookId = bookId)

            if (content != null) {
                cachedBookId = bookId
                cachedContent = content
            }
            content
        }

    fun clearCache() {
        cachedBookId = -1L
        cachedContent = null
    }
}
