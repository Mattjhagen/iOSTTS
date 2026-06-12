package com.metroreader.app.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.domain.model.*
import nl.siegmann.epublib.domain.Book as EpubBook
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubParser @Inject constructor() : BookParser {

    override fun supportsFormat(format: BookFormat) = format == BookFormat.EPUB

    override suspend fun parse(file: File, context: Context): ImportResult {
        return try {
            val epubBook = EpubReader().readEpub(FileInputStream(file))
            val metadata = epubBook.metadata

            val title = metadata.titles.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension
            val author = metadata.authors.firstOrNull()?.let {
                "${it.firstname} ${it.lastname}".trim()
            } ?: ""
            val description = metadata.descriptions.firstOrNull() ?: ""
            val publisher = metadata.publishers.firstOrNull() ?: ""
            val language = metadata.language ?: ""
            val isbn = metadata.identifiers.firstOrNull()?.value ?: ""

            // Extract cover
            val coverPath = extractCover(epubBook, context, file.nameWithoutExtension)

            // Count chapters
            val chapters = epubBook.spine.spineReferences
            val totalChapters = chapters.size

            // Estimate reading time (avg 200 wpm)
            val totalWords = chapters.sumOf { ref ->
                val html = String(ref.resource.data)
                Jsoup.parse(html).text().split("\\s+".toRegex()).size
            }
            val estimatedMinutes = (totalWords / 200).coerceAtLeast(1)

            val book = Book(
                title = title,
                author = author,
                filePath = file.absolutePath,
                coverPath = coverPath,
                format = BookFormat.EPUB,
                totalChapters = totalChapters,
                fileSize = file.length(),
                estimatedReadingMinutes = estimatedMinutes,
                description = description,
                publisher = publisher,
                language = language,
                isbn = isbn,
            )
            ImportResult.Success(book)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse EPUB: ${e.message}", e)
        }
    }

    override suspend fun extractContent(file: File, context: Context): BookContent {
        val epubBook = EpubReader().readEpub(FileInputStream(file))
        val spineRefs = epubBook.spine.spineReferences
        val tocItems = epubBook.tableOfContents.tocReferences

        val chapters = spineRefs.mapIndexed { index, ref ->
            val html = String(ref.resource.data)
            val doc = Jsoup.parse(html)
            val plainText = doc.text()
            val chapterTitle = doc.title().takeIf { it.isNotBlank() }
                ?: doc.select("h1,h2,h3").firstOrNull()?.text()
                ?: "Chapter ${index + 1}"

            Chapter(
                index = index,
                title = chapterTitle,
                content = plainText,
                htmlContent = doc.body().html(),
                sentences = SentenceSplitter.split(plainText)
            )
        }

        val toc = tocItems.mapIndexed { i, ref ->
            TocEntry(
                title = ref.title ?: "Chapter ${i + 1}",
                chapterIndex = i,
                level = ref.level,
                href = ref.resource?.href ?: ""
            )
        }

        return BookContent(
            bookId = 0L, // will be set by caller
            chapters = chapters,
            tableOfContents = toc
        )
    }

    private fun extractCover(epubBook: EpubBook, context: Context, bookName: String): String? {
        return try {
            val coverImage = epubBook.coverImage ?: return null
            val bitmap = BitmapFactory.decodeByteArray(coverImage.data, 0, coverImage.data.size)
                ?: return null

            val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
            val coverFile = File(coversDir, "${bookName.replace("[^a-zA-Z0-9]".toRegex(), "_")}.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
