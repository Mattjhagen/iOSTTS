// ReaderView.swift
// Metro Reader — Reader Screen

import SwiftUI
import SwiftData

struct ReaderView: View {

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var appState: AppState

    let book: Book

    @StateObject private var viewModel: ReaderViewModel

    init(book: Book) {
        self.book = book
        // modelContext injected via onAppear
        _viewModel = StateObject(wrappedValue: ReaderViewModel(
            book: book,
            repository: BookRepository(modelContext: ModelContext(MetroReaderSchema.container))
        ))
    }

    private var themeColors: ReaderColors {
        appState.readerTheme.colors(accent: appState.accentColor)
    }

    var body: some View {
        ZStack {
            // ── Background ────────────────────────────────────────────────
            themeColors.background.ignoresSafeArea()

            if viewModel.isLoading {
                loadingView
            } else if let error = viewModel.loadError {
                errorView(error)
            } else if let chapter = viewModel.currentChapter {
                // ── Main Reading Area ─────────────────────────────────────
                ReaderContentView(
                    chapter: chapter,
                    themeColors: themeColors,
                    fontSize: appState.fontSize,
                    lineSpacing: appState.lineSpacing,
                    marginHorizontal: appState.marginHorizontal,
                    highlightedSentenceIndex: viewModel.highlightedSentenceIndex,
                    highlights: book.highlights.filter { $0.chapterIndex == viewModel.currentChapterIndex },
                    onTap: { viewModel.toggleControls() },
                    onScrollChange: { offset, viewH, contentH in
                        viewModel.updateScrollProgress(offsetY: offset, viewHeight: viewH, contentHeight: contentH)
                    },
                    onTextSelected: { text, range in
                        viewModel.selectedText = text
                        viewModel.selectedTextRange = range
                        viewModel.showHighlightMenu = true
                    }
                )
                .ignoresSafeArea(edges: .bottom)
            }

            // ── Controls Overlay ──────────────────────────────────────────
            if viewModel.showControls {
                ReaderControlsOverlay(viewModel: viewModel, themeColors: themeColors, onDismiss: { dismiss() })
                    .transition(.opacity)
            }
        }
        .navigationBarHidden(true)
        .statusBarHidden(!viewModel.showControls)
        .animation(.easeInOut(duration: 0.2), value: viewModel.showControls)
        .sheet(isPresented: $viewModel.showTOC) {
            TOCSheet(
                toc: viewModel.content?.tableOfContents ?? [],
                chapters: viewModel.content?.chapters ?? [],
                currentIndex: viewModel.currentChapterIndex,
                accentColor: appState.accentColor,
                onSelect: { index in
                    viewModel.goToChapter(index)
                    viewModel.showTOC = false
                }
            )
        }
        .sheet(isPresented: $viewModel.showHighlightMenu) {
            HighlightMenuSheet(
                selectedText: viewModel.selectedText,
                accentColor: appState.accentColor,
                onHighlight: { colorHex in
                    if let range = viewModel.selectedTextRange {
                        viewModel.addHighlight(text: viewModel.selectedText, range: range, colorHex: colorHex)
                    }
                    viewModel.showHighlightMenu = false
                },
                onNote: {
                    viewModel.showHighlightMenu = false
                    viewModel.showNoteEditor = true
                },
                onDismiss: { viewModel.showHighlightMenu = false }
            )
            .presentationDetents([.height(200)])
        }
        .onAppear {
            Task { await viewModel.loadContent() }
            if appState.keepScreenOn { UIApplication.shared.isIdleTimerDisabled = true }
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }

    private var loadingView: some View {
        VStack(spacing: 20) {
            MetroLoadingDots(color: appState.accentColor)
            Text("loading \(book.title)…")
                .font(MetroType.bodySans(16, weight: .light))
                .foregroundStyle(themeColors.subtext)
        }
    }

    private func errorView(_ error: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("cannot open book")
                .font(MetroType.headline(28, weight: .thin))
                .foregroundStyle(themeColors.onBackground)
            Text(error)
                .font(MetroType.bodySans())
                .foregroundStyle(themeColors.subtext)
            Button("Go Back") { dismiss() }
                .foregroundStyle(themeColors.accentColor)
        }
        .padding(32)
    }
}

// MARK: - Reader Content View
struct ReaderContentView: View {
    let chapter: Chapter
    let themeColors: ReaderColors
    let fontSize: Double
    let lineSpacing: Double
    let marginHorizontal: Double
    let highlightedSentenceIndex: Int
    let highlights: [Highlight]
    let onTap: () -> Void
    let onScrollChange: (Double, Double, Double) -> Void
    let onTextSelected: (String, NSRange) -> Void

    @State private var contentHeight: CGFloat = 0
    @State private var scrollOffset: CGFloat = 0

    var body: some View {
        GeometryReader { geo in
            ScrollViewReader { proxy in
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        // Chapter Title
                        Text(chapter.title)
                            .font(MetroType.readerChapterTitle(size: fontSize))
                            .foregroundStyle(themeColors.onBackground)
                            .padding(.horizontal, marginHorizontal)
                            .padding(.top, 48)
                            .padding(.bottom, 32)

                        // Chapter Body — sentence-by-sentence for TTS sync
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(chapter.sentences.enumerated()), id: \.offset) { idx, sentence in
                                Text(sentence + " ")
                                    .font(MetroType.readerBody(size: fontSize))
                                    .lineSpacing(fontSize * (lineSpacing - 1))
                                    .foregroundStyle(themeColors.onBackground)
                                    .background(
                                        idx == highlightedSentenceIndex
                                            ? themeColors.highlightSentence
                                            : Color.clear
                                    )
                                    .id("sentence_\(idx)")
                                    .animation(.easeInOut(duration: 0.2), value: highlightedSentenceIndex)
                            }
                        }
                        .padding(.horizontal, marginHorizontal)
                        .background(
                            GeometryReader { contentGeo in
                                Color.clear.onAppear {
                                    contentHeight = contentGeo.size.height
                                }
                                .onChange(of: contentGeo.size.height) { _, h in
                                    contentHeight = h
                                }
                            }
                        )

                        Spacer(minLength: 80)
                    }
                    .background(
                        GeometryReader { scrollGeo in
                            Color.clear
                                .preference(key: ScrollOffsetKey.self, value: -scrollGeo.frame(in: .named("scroll")).origin.y)
                        }
                    )
                }
                .coordinateSpace(name: "scroll")
                .onPreferenceChange(ScrollOffsetKey.self) { offset in
                    scrollOffset = offset
                    onScrollChange(Double(offset), Double(geo.size.height), Double(contentHeight))
                }
                .onChange(of: highlightedSentenceIndex) { _, idx in
                    if idx >= 0 {
                        withAnimation(.easeInOut(duration: 0.4)) {
                            proxy.scrollTo("sentence_\(idx)", anchor: .center)
                        }
                    }
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
    }
}

struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Reader Controls Overlay
struct ReaderControlsOverlay: View {
    @ObservedObject var viewModel: ReaderViewModel
    let themeColors: ReaderColors
    let onDismiss: () -> Void
    @EnvironmentObject private var appState: AppState

    var body: some View {
        VStack {
            // ── Top Bar ───────────────────────────────────────────────────
            HStack {
                Button(action: onDismiss) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .light))
                        .foregroundStyle(themeColors.onBackground)
                }
                .accessibilityLabel("Back")

                Spacer()

                VStack(spacing: 2) {
                    Text(viewModel.book.title)
                        .font(MetroType.label(13, weight: .medium))
                        .lineLimit(1)
                        .foregroundStyle(themeColors.onBackground)
                    Text(viewModel.chapterProgressText)
                        .font(MetroType.caption())
                        .foregroundStyle(themeColors.subtext)
                }

                Spacer()

                HStack(spacing: 16) {
                    Button(action: { viewModel.showTOC = true }) {
                        Image(systemName: "list.bullet")
                            .foregroundStyle(themeColors.onBackground)
                    }
                    .accessibilityLabel("Table of Contents")

                    Button(action: { viewModel.addBookmark() }) {
                        Image(systemName: viewModel.isCurrentPositionBookmarked ? "bookmark.fill" : "bookmark")
                            .foregroundStyle(viewModel.isCurrentPositionBookmarked ? themeColors.accentColor : themeColors.onBackground)
                    }
                    .accessibilityLabel("Bookmark")
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(themeColors.background.opacity(0.95))

            Spacer()

            // ── Bottom Bar ────────────────────────────────────────────────
            VStack(spacing: 0) {
                MetroProgressBar(
                    progress: viewModel.book.progressPercent / 100,
                    color: themeColors.accentColor,
                    height: 2
                )

                HStack(spacing: 24) {
                    Button(action: { viewModel.previousChapter() }) {
                        Image(systemName: "chevron.left.2")
                            .foregroundStyle(viewModel.currentChapterIndex > 0 ? themeColors.onBackground : themeColors.subtext)
                    }
                    .disabled(viewModel.currentChapterIndex == 0)
                    .accessibilityLabel("Previous Chapter")

                    Spacer()

                    Text("\(Int(viewModel.book.progressPercent))%")
                        .font(MetroType.label(12, weight: .semibold))
                        .foregroundStyle(themeColors.subtext)

                    Spacer()

                    Button(action: { viewModel.nextChapter() }) {
                        Image(systemName: "chevron.right.2")
                            .foregroundStyle(
                                (viewModel.content.map { viewModel.currentChapterIndex + 1 < $0.chapters.count } ?? false)
                                    ? themeColors.onBackground : themeColors.subtext
                            )
                    }
                    .accessibilityLabel("Next Chapter")
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 14)
            }
            .background(themeColors.background.opacity(0.95))
        }
    }
}

// MARK: - TOC Sheet
struct TOCSheet: View {
    let toc: [TocEntry]
    let chapters: [Chapter]
    let currentIndex: Int
    let accentColor: Color
    let onSelect: (Int) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if !toc.isEmpty {
                    ForEach(toc) { entry in
                        TOCRow(entry: entry, currentIndex: currentIndex, accentColor: accentColor, onSelect: onSelect)
                    }
                } else {
                    ForEach(chapters) { chapter in
                        Button(action: { onSelect(chapter.index) }) {
                            HStack {
                                Text(chapter.title)
                                    .font(MetroType.bodySans(16, weight: chapter.index == currentIndex ? .semibold : .regular))
                                    .foregroundStyle(chapter.index == currentIndex ? accentColor : .primary)
                                Spacer()
                                if chapter.index == currentIndex {
                                    Image(systemName: "chevron.right")
                                        .foregroundStyle(accentColor)
                                        .font(.caption)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .listStyle(.plain)
            .navigationTitle("Contents")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct TOCRow: View {
    let entry: TocEntry
    let currentIndex: Int
    let accentColor: Color
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: { onSelect(entry.chapterIndex) }) {
                HStack {
                    Text(entry.title)
                        .font(MetroType.bodySans(entry.level == 0 ? 16 : 14,
                                                weight: entry.chapterIndex == currentIndex ? .semibold : .regular))
                        .foregroundStyle(entry.chapterIndex == currentIndex ? accentColor : .primary)
                        .padding(.leading, CGFloat(entry.level) * 16)
                    Spacer()
                }
            }
            .buttonStyle(.plain)
            .padding(.vertical, 6)

            ForEach(entry.children) { child in
                TOCRow(entry: child, currentIndex: currentIndex, accentColor: accentColor, onSelect: onSelect)
            }
        }
    }
}

// MARK: - Highlight Menu Sheet
struct HighlightMenuSheet: View {
    let selectedText: String
    let accentColor: Color
    let onHighlight: (String) -> Void
    let onNote: () -> Void
    let onDismiss: () -> Void

    private let highlightColors: [(hex: String, color: Color)] = [
        ("#FFD700", .yellow),
        ("#90EE90", .green),
        ("#ADD8E6", .blue),
        ("#FFB6C1", .pink),
        ("#FFA500", .orange),
    ]

    var body: some View {
        VStack(spacing: 16) {
            Text(selectedText)
                .font(MetroType.bodySans(14, weight: .regular))
                .lineLimit(2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
                .padding(.top, 16)

            HStack(spacing: 16) {
                ForEach(highlightColors, id: \.hex) { item in
                    Button(action: { onHighlight(item.hex) }) {
                        Circle()
                            .fill(item.color)
                            .frame(width: 32, height: 32)
                    }
                }
                Spacer()
                Button(action: onNote) {
                    Image(systemName: "note.text.badge.plus")
                        .font(.title2)
                        .foregroundStyle(accentColor)
                }
                .accessibilityLabel("Add Note")
            }
            .padding(.horizontal, 20)
        }
    }
}
