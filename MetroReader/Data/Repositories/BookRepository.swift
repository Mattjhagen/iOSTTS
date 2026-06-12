// BookRepository.swift
// Metro Reader — Data Access Layer

import SwiftData
import Foundation

@MainActor
final class BookRepository {

    let modelContext: ModelContext

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Books

    func fetchAllBooks() throws -> [Book] {
        let descriptor = FetchDescriptor<Book>(
            sortBy: [SortDescriptor(\.lastOpenedDate, order: .reverse),
                     SortDescriptor(\.addedDate, order: .reverse)]
        )
        return try modelContext.fetch(descriptor)
    }

    func fetchRecentlyRead(limit: Int = 8) throws -> [Book] {
        var descriptor = FetchDescriptor<Book>(
            predicate: #Predicate { $0.lastOpenedDate != nil },
            sortBy: [SortDescriptor(\.lastOpenedDate, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return try modelContext.fetch(descriptor)
    }

    func fetchBook(id: UUID) throws -> Book? {
        let descriptor = FetchDescriptor<Book>(
            predicate: #Predicate { $0.id == id }
        )
        return try modelContext.fetch(descriptor).first
    }

    func insert(_ book: Book) {
        modelContext.insert(book)
        try? modelContext.save()
    }

    func delete(_ book: Book) {
        // Also delete the file from disk
        let fileURL = documentsURL.appendingPathComponent(book.filePath)
        try? FileManager.default.removeItem(at: fileURL)
        modelContext.delete(book)
        try? modelContext.save()
    }

    func markOpened(_ book: Book) {
        book.lastOpenedDate = Date()
        try? modelContext.save()
    }

    func updateProgress(_ book: Book, percent: Double) {
        book.progressPercent = percent
        book.isFinished = percent >= 99.0
        try? modelContext.save()
    }

    // MARK: - Reading Progress

    func saveReadingProgress(
        for book: Book,
        chapterIndex: Int,
        characterOffset: Int,
        scrollOffsetY: Double,
        percent: Double
    ) {
        if let existing = book.readingProgress {
            existing.currentChapterIndex = chapterIndex
            existing.characterOffset = characterOffset
            existing.scrollOffsetY = scrollOffsetY
            existing.progressPercent = percent
            existing.lastReadDate = Date()
        } else {
            let progress = ReadingProgress(
                bookId: book.id,
                chapterIndex: chapterIndex,
                characterOffset: characterOffset,
                scrollOffsetY: scrollOffsetY,
                progressPercent: percent
            )
            modelContext.insert(progress)
            book.readingProgress = progress
        }
        book.progressPercent = percent
        try? modelContext.save()
    }

    // MARK: - Bookmarks

    func addBookmark(to book: Book, chapterIndex: Int, characterOffset: Int, label: String) {
        let bookmark = Bookmark(
            bookId: book.id,
            chapterIndex: chapterIndex,
            characterOffset: characterOffset,
            label: label
        )
        modelContext.insert(bookmark)
        book.bookmarks.append(bookmark)
        try? modelContext.save()
    }

    func deleteBookmark(_ bookmark: Bookmark) {
        modelContext.delete(bookmark)
        try? modelContext.save()
    }

    // MARK: - Highlights

    func addHighlight(
        to book: Book,
        chapterIndex: Int,
        startOffset: Int,
        endOffset: Int,
        text: String,
        colorHex: String = "#FFD700"
    ) {
        let highlight = Highlight(
            bookId: book.id,
            chapterIndex: chapterIndex,
            startOffset: startOffset,
            endOffset: endOffset,
            selectedText: text,
            colorHex: colorHex
        )
        modelContext.insert(highlight)
        book.highlights.append(highlight)
        try? modelContext.save()
    }

    func deleteHighlight(_ highlight: Highlight) {
        modelContext.delete(highlight)
        try? modelContext.save()
    }

    // MARK: - Notes

    func addNote(to book: Book, chapterIndex: Int, characterOffset: Int, content: String) {
        let note = Note(
            bookId: book.id,
            chapterIndex: chapterIndex,
            characterOffset: characterOffset,
            content: content
        )
        modelContext.insert(note)
        book.notes.append(note)
        try? modelContext.save()
    }

    func updateNote(_ note: Note, content: String) {
        note.content = content
        note.modifiedDate = Date()
        try? modelContext.save()
    }

    func deleteNote(_ note: Note) {
        modelContext.delete(note)
        try? modelContext.save()
    }

    // MARK: - TTS Progress

    func saveTtsProgress(
        for book: Book,
        chapterIndex: Int,
        sentenceIndex: Int,
        speed: Float,
        pitch: Float,
        voiceIdentifier: String
    ) {
        if let existing = book.ttsProgress {
            existing.chapterIndex = chapterIndex
            existing.sentenceIndex = sentenceIndex
            existing.speedMultiplier = speed
            existing.pitchMultiplier = pitch
            existing.voiceIdentifier = voiceIdentifier
            existing.lastPlayedDate = Date()
        } else {
            let progress = TtsProgress(
                bookId: book.id,
                chapterIndex: chapterIndex,
                sentenceIndex: sentenceIndex,
                speed: speed,
                pitch: pitch,
                voiceIdentifier: voiceIdentifier
            )
            modelContext.insert(progress)
            book.ttsProgress = progress
        }
        try? modelContext.save()
    }

    // MARK: - Helpers

    var documentsURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }
}
