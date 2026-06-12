// BookModels.swift
// Metro Reader — SwiftData Schema
// All models use @Model for SwiftData persistence

import SwiftData
import Foundation

// MARK: - Book Format
enum BookFormat: String, Codable, CaseIterable {
    case epub  = "EPUB"
    case pdf   = "PDF"
    case txt   = "TXT"
    case html  = "HTML"
    case mobi  = "MOBI"
    case azw3  = "AZW3"

    var fileExtensions: [String] {
        switch self {
        case .epub: return ["epub"]
        case .pdf:  return ["pdf"]
        case .txt:  return ["txt"]
        case .html: return ["html", "htm", "xhtml"]
        case .mobi: return ["mobi", "azw"]
        case .azw3: return ["azw3"]
        }
    }

    static func from(extension ext: String) -> BookFormat? {
        allCases.first { $0.fileExtensions.contains(ext.lowercased()) }
    }
}

// MARK: - Book @Model
@Model
final class Book {
    @Attribute(.unique) var id: UUID
    var title: String
    var author: String
    var publisher: String
    var language: String
    var isbn: String
    var description: String
    var formatRaw: String
    var filePath: String          // Relative path in app's Documents directory
    var coverData: Data?          // JPEG thumbnail cached locally
    var accentColorHex: String    // Dominant cover color
    var fileSize: Int64
    var pageCount: Int
    var estimatedReadingMinutes: Int
    var addedDate: Date
    var lastOpenedDate: Date?
    var isFinished: Bool
    var progressPercent: Double   // 0–100

    // Relationships
    @Relationship(deleteRule: .cascade) var readingProgress: ReadingProgress?
    @Relationship(deleteRule: .cascade) var bookmarks: [Bookmark] = []
    @Relationship(deleteRule: .cascade) var highlights: [Highlight] = []
    @Relationship(deleteRule: .cascade) var notes: [Note] = []
    @Relationship(deleteRule: .cascade) var ttsProgress: TtsProgress?

    var format: BookFormat {
        get { BookFormat(rawValue: formatRaw) ?? .epub }
        set { formatRaw = newValue.rawValue }
    }

    init(
        id: UUID = UUID(),
        title: String,
        author: String = "",
        publisher: String = "",
        language: String = "en",
        isbn: String = "",
        description: String = "",
        format: BookFormat,
        filePath: String,
        coverData: Data? = nil,
        accentColorHex: String = "#0078D4",
        fileSize: Int64 = 0,
        pageCount: Int = 0,
        estimatedReadingMinutes: Int = 0
    ) {
        self.id = id
        self.title = title
        self.author = author
        self.publisher = publisher
        self.language = language
        self.isbn = isbn
        self.description = description
        self.formatRaw = format.rawValue
        self.filePath = filePath
        self.coverData = coverData
        self.accentColorHex = accentColorHex
        self.fileSize = fileSize
        self.pageCount = pageCount
        self.estimatedReadingMinutes = estimatedReadingMinutes
        self.addedDate = Date()
        self.isFinished = false
        self.progressPercent = 0
    }
}

// MARK: - ReadingProgress @Model
@Model
final class ReadingProgress {
    @Attribute(.unique) var id: UUID
    var bookId: UUID
    var currentChapterIndex: Int
    var characterOffset: Int
    var scrollOffsetY: Double
    var progressPercent: Double
    var lastReadDate: Date

    init(bookId: UUID, chapterIndex: Int = 0, characterOffset: Int = 0,
         scrollOffsetY: Double = 0, progressPercent: Double = 0) {
        self.id = UUID()
        self.bookId = bookId
        self.currentChapterIndex = chapterIndex
        self.characterOffset = characterOffset
        self.scrollOffsetY = scrollOffsetY
        self.progressPercent = progressPercent
        self.lastReadDate = Date()
    }
}

// MARK: - Bookmark @Model
@Model
final class Bookmark {
    @Attribute(.unique) var id: UUID
    var bookId: UUID
    var chapterIndex: Int
    var characterOffset: Int
    var label: String
    var createdDate: Date

    init(bookId: UUID, chapterIndex: Int, characterOffset: Int, label: String = "") {
        self.id = UUID()
        self.bookId = bookId
        self.chapterIndex = chapterIndex
        self.characterOffset = characterOffset
        self.label = label
        self.createdDate = Date()
    }
}

// MARK: - Highlight @Model
@Model
final class Highlight {
    @Attribute(.unique) var id: UUID
    var bookId: UUID
    var chapterIndex: Int
    var startOffset: Int
    var endOffset: Int
    var selectedText: String
    var colorHex: String
    var note: String
    var createdDate: Date

    init(bookId: UUID, chapterIndex: Int, startOffset: Int, endOffset: Int,
         selectedText: String, colorHex: String = "#FFD700", note: String = "") {
        self.id = UUID()
        self.bookId = bookId
        self.chapterIndex = chapterIndex
        self.startOffset = startOffset
        self.endOffset = endOffset
        self.selectedText = selectedText
        self.colorHex = colorHex
        self.note = note
        self.createdDate = Date()
    }
}

// MARK: - Note @Model
@Model
final class Note {
    @Attribute(.unique) var id: UUID
    var bookId: UUID
    var chapterIndex: Int
    var characterOffset: Int
    var content: String
    var createdDate: Date
    var modifiedDate: Date

    init(bookId: UUID, chapterIndex: Int, characterOffset: Int, content: String) {
        self.id = UUID()
        self.bookId = bookId
        self.chapterIndex = chapterIndex
        self.characterOffset = characterOffset
        self.content = content
        self.createdDate = Date()
        self.modifiedDate = Date()
    }
}

// MARK: - TtsProgress @Model
@Model
final class TtsProgress {
    @Attribute(.unique) var id: UUID
    var bookId: UUID
    var chapterIndex: Int
    var sentenceIndex: Int
    var speedMultiplier: Float
    var pitchMultiplier: Float
    var voiceIdentifier: String
    var lastPlayedDate: Date

    init(bookId: UUID, chapterIndex: Int = 0, sentenceIndex: Int = 0,
         speed: Float = 1.0, pitch: Float = 1.0, voiceIdentifier: String = "") {
        self.id = UUID()
        self.bookId = bookId
        self.chapterIndex = chapterIndex
        self.sentenceIndex = sentenceIndex
        self.speedMultiplier = speed
        self.pitchMultiplier = pitch
        self.voiceIdentifier = voiceIdentifier
        self.lastPlayedDate = Date()
    }
}
