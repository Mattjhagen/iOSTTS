// ReaderViewModel.swift
// Metro Reader — Reader Feature

import SwiftUI
import SwiftData
import Combine
import NaturalLanguage

@MainActor
final class ReaderViewModel: ObservableObject {

    // MARK: - Book & Content
    let book: Book
    @Published var content: BookContent? = nil
    @Published var isLoading: Bool = true
    @Published var loadError: String? = nil

    // MARK: - Navigation
    @Published var currentChapterIndex: Int = 0
    @Published var scrollOffsetY: Double = 0
    @Published var characterOffset: Int = 0

    // MARK: - UI State
    @Published var showControls: Bool = true
    @Published var showTOC: Bool = false
    @Published var showBookmarkSheet: Bool = false
    @Published var showHighlightMenu: Bool = false
    @Published var showNoteEditor: Bool = false
    @Published var selectedTextRange: NSRange? = nil
    @Published var selectedText: String = ""

    // MARK: - TTS Sync
    @Published var highlightedSentenceIndex: Int = -1
    @Published var isTTSActive: Bool = false

    // MARK: - Reading Settings (from AppState)
    @Published var fontSize: Double = 18
    @Published var lineSpacing: Double = 1.6
    @Published var marginHorizontal: Double = 20
    @Published var readerTheme: ReaderThemeMode = .dark

    private let repository: BookRepository
    private var cancellables = Set<AnyCancellable>()
    private var controlsHideTask: Task<Void, Never>? = nil
    private var progressSaveTask: Task<Void, Never>? = nil

    init(book: Book, repository: BookRepository) {
        self.book = book
        self.repository = repository

        // Restore last reading position
        if let progress = book.readingProgress {
            currentChapterIndex = progress.currentChapterIndex
            characterOffset = progress.characterOffset
            scrollOffsetY = progress.scrollOffsetY
        }
    }

    // MARK: - Load Content
    func loadContent() async {
        isLoading = true
        loadError = nil

        do {
            let docsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fileURL = docsURL.appendingPathComponent("Books").appendingPathComponent(book.filePath)

            let parsers: [any BookParser] = [EPUBParser(), PDFParser(), TxtParser(), HTMLParser(), MOBIParser()]
            guard let parser = parsers.first(where: { $0.supports(format: book.format) }) else {
                throw ImportError.noParserAvailable(book.format)
            }

            let parsed = try await parser.parseContent(from: fileURL, bookId: book.id)
            content = parsed

            // Update estimated reading time
            if book.estimatedReadingMinutes == 0 {
                book.estimatedReadingMinutes = parsed.estimatedReadingMinutes
                try? repository.modelContext.save()
            }
        } catch {
            loadError = error.localizedDescription
        }

        isLoading = false
    }

    // MARK: - Navigation
    var currentChapter: Chapter? {
        guard let content = content,
              currentChapterIndex < content.chapters.count else { return nil }
        return content.chapters[currentChapterIndex]
    }

    func goToChapter(_ index: Int) {
        guard let content = content, index >= 0, index < content.chapters.count else { return }
        currentChapterIndex = index
        scrollOffsetY = 0
        characterOffset = 0
        highlightedSentenceIndex = -1
        scheduleProgressSave()
    }

    func nextChapter() {
        guard let content = content, currentChapterIndex + 1 < content.chapters.count else { return }
        goToChapter(currentChapterIndex + 1)
    }

    func previousChapter() {
        guard currentChapterIndex > 0 else { return }
        goToChapter(currentChapterIndex - 1)
    }

    // MARK: - Progress
    func updateScrollProgress(offsetY: Double, viewHeight: Double, contentHeight: Double) {
        scrollOffsetY = offsetY
        let progress = contentHeight > 0 ? offsetY / contentHeight : 0
        let chapterFraction = 1.0 / Double(content?.chapters.count ?? 1)
        let globalProgress = (Double(currentChapterIndex) * chapterFraction + progress * chapterFraction) * 100
        characterOffset = Int(progress * Double(currentChapter?.content.count ?? 0))

        if abs(globalProgress - book.progressPercent) > 0.5 {
            scheduleProgressSave(percent: globalProgress)
        }
    }

    private func scheduleProgressSave(percent: Double? = nil) {
        progressSaveTask?.cancel()
        progressSaveTask = Task { [weak self] in
            guard let self = self else { return }
            try? await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds debounce
            guard !Task.isCancelled else { return }
            let p = percent ?? self.book.progressPercent
            self.repository.saveReadingProgress(
                for: self.book,
                chapterIndex: self.currentChapterIndex,
                characterOffset: self.characterOffset,
                scrollOffsetY: self.scrollOffsetY,
                percent: p
            )
        }
    }

    // MARK: - Controls Visibility
    func toggleControls() {
        withAnimation(.easeInOut(duration: 0.2)) {
            showControls.toggle()
        }
        if showControls { scheduleControlsHide() }
    }

    func showControlsTemporarily() {
        withAnimation(.easeInOut(duration: 0.2)) { showControls = true }
        scheduleControlsHide()
    }

    private func scheduleControlsHide() {
        controlsHideTask?.cancel()
        controlsHideTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 4_000_000_000)
            guard !Task.isCancelled else { return }
            withAnimation(.easeInOut(duration: 0.3)) {
                self?.showControls = false
            }
        }
    }

    // MARK: - Bookmarks
    func addBookmark(label: String = "") {
        repository.addBookmark(
            to: book,
            chapterIndex: currentChapterIndex,
            characterOffset: characterOffset,
            label: label.isEmpty ? (currentChapter?.title ?? "Bookmark") : label
        )
    }

    var isCurrentPositionBookmarked: Bool {
        book.bookmarks.contains {
            $0.chapterIndex == currentChapterIndex &&
            abs($0.characterOffset - characterOffset) < 200
        }
    }

    // MARK: - Highlights
    func addHighlight(text: String, range: NSRange, colorHex: String = "#FFD700") {
        repository.addHighlight(
            to: book,
            chapterIndex: currentChapterIndex,
            startOffset: range.location,
            endOffset: range.location + range.length,
            text: text,
            colorHex: colorHex
        )
    }

    // MARK: - TTS Sync
    func setTTSSentenceHighlight(index: Int) {
        highlightedSentenceIndex = index
    }

    func clearTTSHighlight() {
        highlightedSentenceIndex = -1
    }

    // MARK: - Computed Properties
    var progressPercent: Double {
        guard let content = content, !content.chapters.isEmpty else { return 0 }
        let chapterFraction = 1.0 / Double(content.chapters.count)
        return (Double(currentChapterIndex) * chapterFraction) * 100
    }

    var chapterProgressText: String {
        guard let content = content else { return "" }
        return "Chapter \(currentChapterIndex + 1) of \(content.chapters.count)"
    }
}
