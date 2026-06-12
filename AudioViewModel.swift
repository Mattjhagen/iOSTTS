// AudioViewModel.swift
// Metro Reader — Audio Feature

import SwiftUI
import AVFoundation
import Combine

@MainActor
final class AudioViewModel: ObservableObject {

    @Published var book: Book? = nil
    @Published var content: BookContent? = nil
    @Published var isLoadingContent: Bool = false

    let engine: MetroTTSEngine

    private let repository: BookRepository
    private var cancellables = Set<AnyCancellable>()

    init(repository: BookRepository) {
        self.repository = repository
        self.engine = MetroTTSEngine()

        engine.onSentenceHighlight = { [weak self] idx in
            Task { @MainActor in
                self?.objectWillChange.send()
            }
        }
    }

    // MARK: - Load Book for Playback
    func loadBook(_ book: Book) async {
        guard self.book?.id != book.id else {
            // Already loaded — just resume
            return
        }
        isLoadingContent = true
        self.book = book

        do {
            let docsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fileURL = docsURL.appendingPathComponent("Books").appendingPathComponent(book.filePath)
            let parsers: [any BookParser] = [EPUBParser(), PDFParser(), TxtParser(), HTMLParser(), MOBIParser()]
            guard let parser = parsers.first(where: { $0.supports(format: book.format) }) else { return }

            let parsed = try await parser.parseContent(from: fileURL, bookId: book.id)
            content = parsed

            let startChapter = book.ttsProgress?.chapterIndex ?? 0
            let startSentence = book.ttsProgress?.sentenceIndex ?? 0
            engine.load(book: book, content: parsed, startChapter: startChapter, startSentence: startSentence)

            if let progress = book.ttsProgress {
                engine.setSpeed(progress.speedMultiplier)
                engine.setPitch(progress.pitchMultiplier)
                if let voice = AVSpeechSynthesisVoice(identifier: progress.voiceIdentifier) {
                    engine.setVoice(voice)
                }
            }
        } catch {
            print("AudioViewModel: Failed to load content: \(error)")
        }

        isLoadingContent = false
    }

    func saveProgress() {
        guard let book = book else { return }
        repository.saveTtsProgress(
            for: book,
            chapterIndex: engine.currentChapterIndex,
            sentenceIndex: max(0, engine.currentSentenceIndex),
            speed: engine.speed,
            pitch: engine.pitch,
            voiceIdentifier: engine.selectedVoice?.identifier ?? ""
        )
    }

    var currentChapterTitle: String {
        guard let content = content,
              engine.currentChapterIndex < content.chapters.count else { return "" }
        return content.chapters[engine.currentChapterIndex].title
    }

    var totalChapters: Int { content?.chapters.count ?? 0 }

    var sentenceProgress: Double {
        guard let content = content,
              engine.currentChapterIndex < content.chapters.count else { return 0 }
        let total = content.chapters[engine.currentChapterIndex].sentences.count
        guard total > 0 else { return 0 }
        return Double(max(0, engine.currentSentenceIndex)) / Double(total)
    }
}
