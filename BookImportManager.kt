package com.metroreader.app.parser

import android.content.Context
import android.net.Uri
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.data.repository.BookRepository
import com.metroreader.app.domain.model.Book
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val txtParser: TxtParser,
    private val htmlParser: HtmlParser,
    private val bookRepository: BookRepository,
) {

    /**
     * Import a book from a URI (file picker, share sheet, open-with).
     * Copies the file to internal storage, parses metadata, and saves to DB.
     */
    suspend fun importFromUri(uri: Uri, mimeType: String? = null): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val format = BookFormatDetector.detect(uri, mimeType)
                if (format == BookFormat.UNKNOWN) {
                    return@withContext ImportResult.Error("Unsupported file format")
                }

                // Copy to internal storage
                val booksDir = File(context.filesDir, "books").also { it.mkdirs() }
                val fileName = getFileName(uri) ?: "book_${System.currentTimeMillis()}.${format.name.lowercase()}"
                val destFile = File(booksDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext ImportResult.Error("Cannot open file")

                // Parse
                val result = parseFile(destFile, format)
                if (result is ImportResult.Success) {
                    val bookId = bookRepository.addBook(result.book)
                    ImportResult.Success(result.book.copy(id = bookId))
                } else {
                    destFile.delete()
                    result
                }
            } catch (e: Exception) {
                ImportResult.Error("Import failed: ${e.message}", e)
            }
        }

    /**
     * Import from an already-copied local file.
     */
    suspend fun importFromFile(file: File): ImportResult = withContext(Dispatchers.IO) {
        val format = BookFormatDetector.detect(file)
        if (format == BookFormat.UNKNOWN) {
            return@withContext ImportResult.Error("Unsupported file format: ${file.extension}")
        }
        val result = parseFile(file, format)
        if (result is ImportResult.Success) {
            val bookId = bookRepository.addBook(result.book)
            ImportResult.Success(result.book.copy(id = bookId))
        } else result
    }

    private suspend fun parseFile(file: File, format: BookFormat): ImportResult {
        val parser: BookParser = when (format) {
            BookFormat.EPUB -> epubParser
            BookFormat.PDF  -> pdfParser
            BookFormat.TXT  -> txtParser
            BookFormat.HTML -> htmlParser
            else            -> return ImportResult.Error("Parser not available for $format")
        }
        return parser.parse(file, context)
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
