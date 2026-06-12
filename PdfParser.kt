package com.metroreader.app.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.domain.model.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfParser @Inject constructor() : BookParser {

    override fun supportsFormat(format: BookFormat) = format == BookFormat.PDF

    override suspend fun parse(file: File, context: Context): ImportResult {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            // Extract first page as cover thumbnail
            val coverPath = extractCover(renderer, context, file.nameWithoutExtension)

            renderer.close()
            pfd.close()

            val estimatedMinutes = (totalPages * 2).coerceAtLeast(1) // ~2 min/page

            val book = Book(
                title = file.nameWithoutExtension,
                author = "",
                filePath = file.absolutePath,
                coverPath = coverPath,
                format = BookFormat.PDF,
                totalPages = totalPages,
                totalChapters = (totalPages / 10).coerceAtLeast(1),
                fileSize = file.length(),
                estimatedReadingMinutes = estimatedMinutes,
            )
            ImportResult.Success(book)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse PDF: ${e.message}", e)
        }
    }

    override suspend fun extractContent(file: File, context: Context): BookContent {
        // PDF content is rendered page-by-page; chapters are approximated by page groups
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount
        val pagesPerChapter = 10

        val chapters = (0 until totalPages step pagesPerChapter).mapIndexed { chapterIdx, startPage ->
            val endPage = minOf(startPage + pagesPerChapter - 1, totalPages - 1)
            Chapter(
                index = chapterIdx,
                title = "Pages ${startPage + 1}–${endPage + 1}",
                content = "PDF page group ${startPage + 1} to ${endPage + 1}",
                htmlContent = "",
                sentences = listOf("Pages ${startPage + 1} to ${endPage + 1}")
            )
        }

        renderer.close()
        pfd.close()

        return BookContent(bookId = 0L, chapters = chapters)
    }

    /**
     * Renders the first PDF page as a cover thumbnail.
     */
    fun renderPage(file: File, pageIndex: Int, width: Int = 800): Bitmap? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close(); pfd.close(); return null
            }
            val page = renderer.openPage(pageIndex)
            val scale = width.toFloat() / page.width
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCover(renderer: PdfRenderer, context: Context, bookName: String): String? {
        return try {
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            val width = 400
            val scale = width.toFloat() / page.width
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
            val coverFile = File(coversDir, "${bookName.replace("[^a-zA-Z0-9]".toRegex(), "_")}.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
