// MetroTTSEngine.swift
// Metro Reader — TTS Engine
// AVSpeechSynthesizer + Apple Neural Voices + sentence-level synchronization

import AVFoundation
import MediaPlayer
import Combine

// MARK: - TTS Engine State
enum TTSState: Equatable {
    case idle
    case loading
    case playing
    case paused
    case finished
}

// MARK: - MetroTTSEngine
@MainActor
final class MetroTTSEngine: NSObject, ObservableObject {

    // MARK: - Published State
    @Published var state: TTSState = .idle
    @Published var currentSentenceIndex: Int = -1
    @Published var currentChapterIndex: Int = 0
    @Published var speed: Float = 1.0
    @Published var pitch: Float = 1.0
    @Published var selectedVoice: AVSpeechSynthesisVoice? = nil
    @Published var availableVoices: [AVSpeechSynthesisVoice] = []

    // MARK: - Private
    private let synthesizer = AVSpeechSynthesizer()
    private var sentences: [String] = []
    private var chapters: [Chapter] = []
    private var pendingSentenceIndex: Int = 0
    private var bookTitle: String = ""
    private var bookAuthor: String = ""
    private var coverData: Data? = nil

    // MARK: - Callbacks
    var onSentenceHighlight: ((Int) -> Void)?
    var onChapterChange: ((Int) -> Void)?
    var onFinished: (() -> Void)?

    override init() {
        super.init()
        synthesizer.delegate = self
        loadAvailableVoices()
        setupRemoteCommandCenter()
    }

    // MARK: - Setup
    private func loadAvailableVoices() {
        availableVoices = AVSpeechSynthesisVoice.speechVoices()
            .filter { $0.language.hasPrefix("en") }
            .sorted { $0.name < $1.name }
        // Prefer enhanced/neural voices
        selectedVoice = availableVoices.first(where: { $0.quality == .enhanced || $0.quality == .premium })
            ?? availableVoices.first
    }

    // MARK: - Public API

    func load(book: Book, content: BookContent, startChapter: Int = 0, startSentence: Int = 0) {
        stop()
        chapters = content.chapters
        bookTitle = book.title
        bookAuthor = book.author
        coverData = book.coverData
        currentChapterIndex = startChapter
        pendingSentenceIndex = startSentence
        loadChapter(currentChapterIndex)
        updateNowPlaying()
    }

    func play() {
        guard state != .playing else { return }
        if state == .paused {
            synthesizer.continueSpeaking()
            state = .playing
            return
        }
        speakNextSentence()
    }

    func pause() {
        guard state == .playing else { return }
        synthesizer.pauseSpeaking(at: .word)
        state = .paused
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        state = .idle
        currentSentenceIndex = -1
        onSentenceHighlight?(-1)
    }

    func seekToSentence(_ index: Int) {
        synthesizer.stopSpeaking(at: .immediate)
        pendingSentenceIndex = max(0, min(index, sentences.count - 1))
        if state == .playing || state == .paused {
            state = .playing
            speakNextSentence()
        }
    }

    func nextChapter() {
        guard currentChapterIndex + 1 < chapters.count else { return }
        currentChapterIndex += 1
        loadChapter(currentChapterIndex)
        onChapterChange?(currentChapterIndex)
        if state == .playing { speakNextSentence() }
    }

    func previousChapter() {
        guard currentChapterIndex > 0 else { return }
        currentChapterIndex -= 1
        loadChapter(currentChapterIndex)
        onChapterChange?(currentChapterIndex)
        if state == .playing { speakNextSentence() }
    }

    func setVoice(_ voice: AVSpeechSynthesisVoice) {
        selectedVoice = voice
    }

    func setSpeed(_ newSpeed: Float) {
        speed = max(AVSpeechUtteranceMinimumSpeechRate, min(AVSpeechUtteranceMaximumSpeechRate, newSpeed))
    }

    func setPitch(_ newPitch: Float) {
        pitch = max(0.5, min(2.0, newPitch))
    }

    // MARK: - Private

    private func loadChapter(_ index: Int) {
        guard index < chapters.count else { return }
        sentences = chapters[index].sentences
        pendingSentenceIndex = 0
        currentSentenceIndex = -1
    }

    private func speakNextSentence() {
        guard pendingSentenceIndex < sentences.count else {
            // Move to next chapter
            if currentChapterIndex + 1 < chapters.count {
                nextChapter()
            } else {
                state = .finished
                onFinished?()
                onSentenceHighlight?(-1)
            }
            return
        }

        let sentence = sentences[pendingSentenceIndex]
        let utterance = AVSpeechUtterance(string: sentence)
        utterance.rate = speed * AVSpeechUtteranceDefaultSpeechRate
        utterance.pitchMultiplier = pitch
        utterance.voice = selectedVoice
        utterance.preUtteranceDelay = 0.05
        utterance.postUtteranceDelay = 0.05

        currentSentenceIndex = pendingSentenceIndex
        onSentenceHighlight?(pendingSentenceIndex)
        pendingSentenceIndex += 1
        state = .playing

        synthesizer.speak(utterance)
    }

    // MARK: - Now Playing / Lock Screen
    private func updateNowPlaying() {
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: bookTitle,
            MPMediaItemPropertyArtist: bookAuthor,
            MPNowPlayingInfoPropertyPlaybackRate: state == .playing ? Double(speed) : 0.0,
            MPMediaItemPropertyPlaybackDuration: Double(sentences.count * 5), // estimate
            MPNowPlayingInfoPropertyElapsedPlaybackTime: Double(currentSentenceIndex * 5),
        ]
        if let data = coverData, let image = UIImage(data: data) {
            info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: CGSize(width: 300, height: 300)) { _ in image }
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func setupRemoteCommandCenter() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        center.stopCommand.addTarget { [weak self] _ in
            self?.stop()
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            self?.nextChapter()
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            self?.previousChapter()
            return .success
        }
        center.changePlaybackRateCommand.supportedPlaybackRates = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]
        center.changePlaybackRateCommand.addTarget { [weak self] event in
            if let e = event as? MPChangePlaybackRateCommandEvent {
                self?.setSpeed(Float(e.playbackRate))
            }
            return .success
        }
    }
}

// MARK: - AVSpeechSynthesizerDelegate
extension MetroTTSEngine: AVSpeechSynthesizerDelegate {

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            guard self.state == .playing else { return }
            self.speakNextSentence()
            self.updateNowPlaying()
        }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didPause utterance: AVSpeechUtterance) {
        Task { @MainActor in self.state = .paused }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didContinue utterance: AVSpeechUtterance) {
        Task { @MainActor in self.state = .playing }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer,
                                       willSpeakRangeOfSpeechString characterRange: NSRange,
                                       utterance: AVSpeechUtterance) {
        // Word-level highlighting (optional enhancement)
    }
}
