// AudioView.swift
// Metro Reader — Audio Player Screen

import SwiftUI
import AVFoundation

struct AudioView: View {

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel: AudioViewModel

    @State private var showVoicePicker: Bool = false

    init(modelContext: ModelContext) {
        let repo = BookRepository(modelContext: modelContext)
        _viewModel = StateObject(wrappedValue: AudioViewModel(repository: repo))
    }

    var engine: MetroTTSEngine { viewModel.engine }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground).ignoresSafeArea()

                if let book = viewModel.book {
                    playerContent(book: book)
                } else {
                    noBookContent
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showVoicePicker) {
                VoicePickerSheet(
                    voices: engine.availableVoices,
                    selectedVoice: engine.selectedVoice,
                    accentColor: appState.accentColor,
                    onSelect: { voice in
                        engine.setVoice(voice)
                        showVoicePicker = false
                    }
                )
            }
            .onDisappear { viewModel.saveProgress() }
        }
    }

    // MARK: - Player Content
    @ViewBuilder
    private func playerContent(book: Book) -> some View {
        VStack(spacing: 0) {
            Spacer()

            // ── Cover Art ─────────────────────────────────────────────────
            BookCoverView(
                coverData: book.coverData,
                title: book.title,
                accentColor: appState.accentColor.opacity(0.8)
            )
            .frame(width: 220, height: 310)
            .shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)
            .scaleEffect(engine.state == .playing ? 1.0 : 0.92)
            .animation(.spring(duration: 0.4), value: engine.state)

            Spacer().frame(height: 32)

            // ── Book Info ─────────────────────────────────────────────────
            VStack(spacing: 4) {
                Text(book.title)
                    .font(MetroType.headline(24, weight: .thin))
                    .lineLimit(1)
                    .foregroundStyle(.primary)
                if !book.author.isEmpty {
                    Text(book.author)
                        .font(MetroType.bodySans(15, weight: .regular))
                        .foregroundStyle(.secondary)
                }
                Text(viewModel.currentChapterTitle)
                    .font(MetroType.label(12, weight: .semibold))
                    .tracking(1.5)
                    .textCase(.uppercase)
                    .foregroundStyle(appState.accentColor)
                    .padding(.top, 2)
            }
            .padding(.horizontal, 32)

            Spacer().frame(height: 24)

            // ── Progress Bar ──────────────────────────────────────────────
            VStack(spacing: 6) {
                MetroProgressBar(
                    progress: viewModel.sentenceProgress,
                    color: appState.accentColor,
                    height: 3
                )
                .padding(.horizontal, 32)

                HStack {
                    Text("Chapter \(engine.currentChapterIndex + 1)")
                        .font(MetroType.caption())
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("of \(viewModel.totalChapters)")
                        .font(MetroType.caption())
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 32)
            }

            Spacer().frame(height: 32)

            // ── Playback Controls ─────────────────────────────────────────
            HStack(spacing: 40) {
                Button(action: { engine.previousChapter() }) {
                    Image(systemName: "backward.end.fill")
                        .font(.title2)
                        .foregroundStyle(engine.currentChapterIndex > 0 ? .primary : .tertiary)
                }
                .disabled(engine.currentChapterIndex == 0)
                .accessibilityLabel("Previous Chapter")

                // Main Play/Pause
                Button(action: {
                    switch engine.state {
                    case .playing: engine.pause()
                    case .paused, .idle, .finished: engine.play()
                    case .loading: break
                    }
                }) {
                    ZStack {
                        Circle()
                            .fill(appState.accentColor)
                            .frame(width: 72, height: 72)
                        Image(systemName: engine.state == .playing ? "pause.fill" : "play.fill")
                            .font(.title)
                            .foregroundStyle(.white)
                    }
                }
                .accessibilityLabel(engine.state == .playing ? "Pause" : "Play")

                Button(action: { engine.nextChapter() }) {
                    Image(systemName: "forward.end.fill")
                        .font(.title2)
                        .foregroundStyle(engine.currentChapterIndex + 1 < viewModel.totalChapters ? .primary : .tertiary)
                }
                .disabled(engine.currentChapterIndex + 1 >= viewModel.totalChapters)
                .accessibilityLabel("Next Chapter")
            }

            Spacer().frame(height: 32)

            // ── Speed & Pitch Controls ────────────────────────────────────
            VStack(spacing: 12) {
                MetroSettingSlider(
                    icon: "speedometer",
                    title: "Speed",
                    displayValue: String(format: "%.2fx", engine.speed / AVSpeechUtteranceDefaultSpeechRate),
                    value: Binding(
                        get: { Double(engine.speed) },
                        set: { engine.setSpeed(Float($0)) }
                    ),
                    range: Double(AVSpeechUtteranceMinimumSpeechRate)...Double(AVSpeechUtteranceMaximumSpeechRate),
                    step: 0.05
                )

                MetroSettingSlider(
                    icon: "waveform",
                    title: "Pitch",
                    displayValue: String(format: "%.1f", engine.pitch),
                    value: Binding(
                        get: { Double(engine.pitch) },
                        set: { engine.setPitch(Float($0)) }
                    ),
                    range: 0.5...2.0,
                    step: 0.1
                )
            }
            .padding(.horizontal, 8)

            Spacer().frame(height: 16)

            // ── Voice Selector ────────────────────────────────────────────
            Button(action: { showVoicePicker = true }) {
                HStack {
                    Image(systemName: "person.wave.2")
                        .foregroundStyle(appState.accentColor)
                    Text(engine.selectedVoice?.name ?? "Select Voice")
                        .font(MetroType.bodySans(15, weight: .regular))
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color(.secondarySystemBackground))
                .padding(.horizontal, 20)
            }
            .buttonStyle(.plain)

            Spacer(minLength: 32)
        }
        .overlay(alignment: .top) {
            if viewModel.isLoadingContent {
                HStack(spacing: 8) {
                    MetroLoadingDots(color: appState.accentColor)
                    Text("Loading audio…")
                        .font(MetroType.caption())
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 8)
            }
        }
    }

    // MARK: - No Book State
    private var noBookContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("listening now")
                .font(MetroType.headline(32, weight: .thin))
                .foregroundStyle(.primary)
            Text("Open a book from the Library and tap the audio button to start listening.")
                .font(MetroType.bodySans())
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 32)
    }
}

// MARK: - Voice Picker Sheet
struct VoicePickerSheet: View {
    let voices: [AVSpeechSynthesisVoice]
    let selectedVoice: AVSpeechSynthesisVoice?
    let accentColor: Color
    let onSelect: (AVSpeechSynthesisVoice) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(voices, id: \.identifier) { voice in
                Button(action: { onSelect(voice) }) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(voice.name)
                                .font(MetroType.bodySans(16, weight: voice.identifier == selectedVoice?.identifier ? .semibold : .regular))
                                .foregroundStyle(.primary)
                            Text(voice.language)
                                .font(MetroType.caption())
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if voice.quality == .premium {
                            Text("NEURAL")
                                .font(MetroType.label(10, weight: .semibold))
                                .tracking(1)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(accentColor)
                        } else if voice.quality == .enhanced {
                            Text("ENHANCED")
                                .font(MetroType.label(10, weight: .semibold))
                                .tracking(1)
                                .foregroundStyle(accentColor)
                        }
                        if voice.identifier == selectedVoice?.identifier {
                            Image(systemName: "checkmark")
                                .foregroundStyle(accentColor)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .listStyle(.plain)
            .navigationTitle("Select Voice")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
