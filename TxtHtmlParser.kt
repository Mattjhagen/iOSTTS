package com.metroreader.app.parser

import android.content.Context
import com.metroreader.app.data.local.entity.BookFormat
import com.metroreader.app.domain.model.*
import org.jsoup.Jsoup
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ─── TXT Parser ───────────────────────────────────────────────────────────────
@Singleton
class TxtParser @Inject constructor() : BookParser {

    override fun supportsFormat(format: BookFormat) = format == BookFormat.TXT

    override suspend fun parse(file: File, context: Context): ImportResult {
        return try {
            val text = file.readText(Charsets.UTF_8)
            val wordCount = text.split("\\s+".toRegex()).size
            val estimatedMinutes = (wordCount / 200).coerceAtLeast(1)
            val chapters = splitIntoChapters(text)

            val book = Book(
                title = file.nameWithoutExtension,
                filePath = file.absolutePath,
                format = BookFormat.TXT,
                totalChapters = chapters.size,
                fileSize = file.length(),
                estimatedReadingMinutes = estimatedMinutes,
            )
            ImportResult.Success(book)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse TXT: ${e.message}", e)
        }
    }

    override suspend fun extractContent(file: File, context: Context): BookContent {
        val text = file.readText(Charsets.UTF_8)
        val chapters = splitIntoChapters(text)
        return BookContent(bookId = 0L, chapters = chapters)
    }

    private fun splitIntoChapters(text: String): List<Chapter> {
        // Split on common chapter markers
        val chapterPattern = Regex(
            """(?m)^(chapter\s+\d+|chapter\s+[ivxlcdm]+|part\s+\d+|\*\s*\*\s*\*|---+)""",
            RegexOption.IGNORE_CASE
        )
        val splits = chapterPattern.findAll(text).toList()

        if (splits.isEmpty()) {
            // No chapters found — split into ~3000-word chunks
            return splitByWordCount(text, 3000)
        }

        val chapters = mutableListOf<Chapter>()
        var prevEnd = 0
        splits.forEachIndexed { i, match ->
            if (i == 0 && match.range.first > 100) {
                // Prologue/intro before first chapter
                val intro = text.substring(0, match.range.first).trim()
                if (intro.isNotBlank()) {
                    chapters.add(Chapter(
                        index = 0,
                        title = "Introduction",
                        content = intro,
                        sentences = SentenceSplitter.split(intro)
                    ))
                }
            }
            val start = match.range.first
            val end = splits.getOrNull(i + 1)?.range?.first ?: text.length
            val content = text.substring(start, end).trim()
            chapters.add(Chapter(
                index = chapters.size,
                title = match.value.trim(),
                content = content,
                sentences = SentenceSplitter.split(content)
            ))
        }
        return chapters.ifEmpty { splitByWordCount(text, 3000) }
    }

    private fun splitByWordCount(text: String, wordsPerChapter: Int): List<Chapter> {
        val words = text.split("\\s+".toRegex())
        return words.chunked(wordsPerChapter).mapIndexed { i, chunk ->
            val content = chunk.joinToString(" ")
            Chapter(
                index = i,
                title = "Section ${i + 1}",
                content = content,
                sentences = SentenceSplitter.split(content)
            )
        }
    }
}

// ─── HTML Parser ──────────────────────────────────────────────────────────────
@Singleton
class HtmlParser @Inject constructor() : BookParser {

    override fun supportsFormat(format: BookFormat) = format == BookFormat.HTML

    override suspend fun parse(file: File, context: Context): ImportResult {
        return try {
            val doc = Jsoup.parse(file, "UTF-8")
            val title = doc.title().takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
            val text = doc.text()
            val wordCount = text.split("\\s+".toRegex()).size
            val estimatedMinutes = (wordCount / 200).coerceAtLeast(1)

            val book = Book(
                title = title,
                filePath = file.absolutePath,
                format = BookFormat.HTML,
                totalChapters = 1,
                fileSize = file.length(),
                estimatedReadingMinutes = estimatedMinutes,
            )
            ImportResult.Success(book)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse HTML: ${e.message}", e)
        }
    }

    override suspend fun extractContent(file: File, context: Context): BookContent {
        val doc = Jsoup.parse(file, "UTF-8")
        val sections = doc.select("h1, h2, h3")

        val chapters = if (sections.isEmpty()) {
            val text = doc.text()
            listOf(Chapter(
                index = 0,
                title = doc.title().takeIf { it.isNotBlank() } ?: "Content",
                content = text,
                htmlContent = doc.body().html(),
                sentences = SentenceSplitter.split(text)
            ))
        } else {
            sections.mapIndexed { i, heading ->
                val nextHeading = sections.getOrNull(i + 1)
                val contentElements = heading.nextElementSiblings()
                    .takeWhile { el -> nextHeading == null || el != nextHeading }
                val htmlContent = contentElements.joinToString("") { it.outerHtml() }
                val text = contentElements.joinToString(" ") { it.text() }
                Chapter(
                    index = i,
                    title = heading.text(),
                    content = text,
                    htmlContent = htmlContent,
                    sentences = SentenceSplitter.split(text)
                )
            }
        }

        return BookContent(bookId = 0L, chapters = chapters)
    }
}
