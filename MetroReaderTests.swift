// MetroReaderTests.swift
// Metro Reader — Unit Tests

import XTesting
import SwiftData
@testable import MetroReader

final class MetroReaderTests: XCTestCase {

    var modelContainer: ModelContainer!
    var repository: BookRepository!

    @MainActor
    override func setUpWithError() throws {
        // Use an in-memory SwiftData container for testing
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try ModelContainer(for: Book.self, configurations: config)
        repository = BookRepository(modelContext: modelContainer.mainContext)
    }

    override func tearDownWithError() throws {
        modelContainer = nil
        repository = nil
    }

    // MARK: - SwiftData / Repository Tests

    @MainActor
    func testBookInsertionAndFetch() throws {
        let book = Book(
            title: "Test Book",
            author: "Test Author",
            format: .epub,
            filePath: "test.epub"
        )

        repository.insert(book)

        let fetched = try repository.fetchAllBooks()
        XCTAssertEqual(fetched.count, 1)
        XCTAssertEqual(fetched.first?.title, "Test Book")
    }

    @MainActor
    func testReadingProgressUpdate() throws {
        let book = Book(title: "Progress Book", format: .epub, filePath: "progress.epub")
        repository.insert(book)

        repository.saveReadingProgress(
            for: book,
            chapterIndex: 2,
            characterOffset: 1500,
            scrollOffsetY: 400.0,
            percent: 25.5
        )

        XCTAssertNotNil(book.readingProgress)
        XCTAssertEqual(book.readingProgress?.currentChapterIndex, 2)
        XCTAssertEqual(book.progressPercent, 25.5)
        XCTAssertFalse(book.isFinished)

        // Test finish condition
        repository.saveReadingProgress(
            for: book,
            chapterIndex: 10,
            characterOffset: 5000,
            scrollOffsetY: 1000.0,
            percent: 100.0
        )
        XCTAssertTrue(book.isFinished)
    }

    @MainActor
    func testBookmarksAndHighlights() throws {
        let book = Book(title: "Annotation Book", format: .pdf, filePath: "annot.pdf")
        repository.insert(book)

        repository.addBookmark(to: book, chapterIndex: 1, characterOffset: 100, label: "Important")
        repository.addHighlight(to: book, chapterIndex: 1, startOffset: 200, endOffset: 250, text: "Highlight text")

        XCTAssertEqual(book.bookmarks.count, 1)
        XCTAssertEqual(book.bookmarks.first?.label, "Important")

        XCTAssertEqual(book.highlights.count, 1)
        XCTAssertEqual(book.highlights.first?.selectedText, "Highlight text")
    }

    // MARK: - Parser Tests

    func testHtmlToPlainText() {
        let html = """
        <div>
            <h1>Chapter 1</h1>
            <p>This is a <b>bold</b> statement.</p>
            <script>alert('hidden');</script>
            <p>Second paragraph.</p>
        </div>
        """

        let plain = EPUBParser.htmlToPlainText(html)

        XCTAssertTrue(plain.contains("Chapter 1"))
        XCTAssertTrue(plain.contains("This is a bold statement."))
        XCTAssertTrue(plain.contains("Second paragraph."))
        XCTAssertFalse(plain.contains("alert"))
        XCTAssertFalse(plain.contains("<h1>"))
    }

    func testSentenceSplitting() {
        let text = "Hello world. How are you? I am fine! Dr. Smith is here. \"Quote!\" he said."
        let sentences = Chapter.splitSentences(text)

        XCTAssertEqual(sentences.count, 5)
        XCTAssertEqual(sentences[0], "Hello world.")
        XCTAssertEqual(sentences[1], "How are you?")
        XCTAssertEqual(sentences[2], "I am fine!")
        XCTAssertEqual(sentences[3], "Dr. Smith is here.")
        XCTAssertEqual(sentences[4], "\"Quote!\" he said.")
    }

    func testBookFormatDetection() {
        XCTAssertEqual(BookFormat.from(extension: "epub"), .epub)
        XCTAssertEqual(BookFormat.from(extension: "EPUB"), .epub)
        XCTAssertEqual(BookFormat.from(extension: "pdf"), .pdf)
        XCTAssertEqual(BookFormat.from(extension: "txt"), .txt)
        XCTAssertEqual(BookFormat.from(extension: "html"), .html)
        XCTAssertEqual(BookFormat.from(extension: "mobi"), .mobi)
        XCTAssertEqual(BookFormat.from(extension: "azw3"), .azw3)
        XCTAssertNil(BookFormat.from(extension: "docx"))
    }
}
