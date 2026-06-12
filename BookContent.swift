// BookContent.swift
// Domain models for parsed book content (in-memory, not persisted)

import Foundation

// MARK: - Chapter
struct Chapter: Identifiable, Sendable {
    let id: UUID
    let index: Int
    let title: String
    let content: String          // Plain text content
    let htmlContent: String?     // Original HTML (for rich rendering)
    let sentences: [String]      // Pre-split sentences for TTS sync
    let wordCount: Int
    let images: [ChapterImage]

    init(
        id: UUID = UUID(),
        index: Int,
        title: String,
        content: String,
        htmlContent: String? = nil,
        sentences: [String] = [],
        images: [ChapterImage] = []
    ) {
        self.id = id
        self.index = index
        self.title = title
        self.content = content
        self.htmlContent = htmlContent
        self.sentences = sentences.isEmpty ? Self.splitSentences(content) : sentences
        self.wordCount = content.split(separator: " ").count
        self.images = images
    }

    static func splitSentences(_ text: String) -> [String] {
        var sentences: [String] = []
        let range = text.startIndex..<text.endIndex
        text.enumerateSubstrings(in: range, options: .bySentences) { substring, _, _, _ in
            if let s = substring?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty {
                sentences.append(s)
            }
        }
        return sentences.isEmpty ? [text] : sentences
    }
}

// MARK: - Chapter Image
struct ChapterImage: Identifiable, Sendable {
    let id: UUID
    let data: Data
    let mimeType: String
    let altText: String

    init(id: UUID = UUID(), data: Data, mimeType: String = "image/jpeg", altText: String = "") {
        self.id = id
        self.data = data
        self.mimeType = mimeType
        self.altText = altText
    }
}

// MARK: - TOC Entry
struct TocEntry: Identifiable, Sendable {
    let id: UUID
    let title: String
    let chapterIndex: Int
    let level: Int               // 0 = top level, 1 = sub-chapter, etc.
    let children: [TocEntry]

    init(
        id: UUID = UUID(),
        title: String,
        chapterIndex: Int,
        level: Int = 0,
        children: [TocEntry] = []
    ) {
        self.id = id
        self.title = title
        self.chapterIndex = chapterIndex
        self.level = level
        self.children = children
    }
}

// MARK: - BookContent
struct BookContent: Sendable {
    let bookId: UUID
    let chapters: [Chapter]
    let tableOfContents: [TocEntry]
    let totalWordCount: Int
    let estimatedReadingMinutes: Int

    init(bookId: UUID, chapters: [Chapter], tableOfContents: [TocEntry] = []) {
        self.bookId = bookId
        self.chapters = chapters
        self.tableOfContents = tableOfContents
        self.totalWordCount = chapters.reduce(0) { $0 + $1.wordCount }
        // Average reading speed: 238 wpm
        self.estimatedReadingMinutes = max(1, totalWordCount / 238)
    }
}

// MARK: - Book Metadata (from parsing)
struct BookMetadata: Sendable {
    var title: String
    var author: String
    var publisher: String
    var language: String
    var isbn: String
    var description: String
    var coverData: Data?
    var pageCount: Int
    var format: BookFormat

    init(
        title: String = "Unknown Title",
        author: String = "Unknown Author",
        publisher: String = "",
        language: String = "en",
        isbn: String = "",
        description: String = "",
        coverData: Data? = nil,
        pageCount: Int = 0,
        format: BookFormat
    ) {
        self.title = title
        self.author = author
        self.publisher = publisher
        self.language = language
        self.isbn = isbn
        self.description = description
        self.coverData = coverData
        self.pageCount = pageCount
        self.format = format
    }
}
