// LibraryView.swift
// Metro Reader — Library Screen

import SwiftUI
import SwiftData
import UniformTypeIdentifiers

struct LibraryView: View {

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var appState: AppState
    @Query(sort: \Book.addedDate, order: .reverse) private var allBooks: [Book]

    @StateObject private var viewModel: LibraryViewModel

    @State private var showGrid: Bool = true
    @State private var selectedBook: Book? = nil
    @State private var navigateToReader: Bool = false

    init(modelContext: ModelContext) {
        let repo = BookRepository(modelContext: modelContext)
        _viewModel = StateObject(wrappedValue: LibraryViewModel(repository: repo))
    }

    var filteredBooks: [Book] { viewModel.filteredBooks(from: allBooks) }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground).ignoresSafeArea()

                if allBooks.isEmpty {
                    EmptyLibraryView(onImport: { viewModel.showImportPicker = true })
                } else {
                    ScrollView {
                        // ── Title ─────────────────────────────────────────
                        VStack(alignment: .leading, spacing: 0) {
                            Text("metro")
                                .font(MetroType.display(56, weight: .ultraLight))
                                .tracking(-3)
                                .foregroundStyle(.primary)
                            Text("library")
                                .font(MetroType.display(56, weight: .ultraLight))
                                .tracking(-3)
                                .foregroundStyle(appState.accentColor)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        // ── Search ────────────────────────────────────────
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundStyle(.secondary)
                            TextField("search books…", text: $viewModel.searchText)
                                .font(MetroType.bodySans())
                        }
                        .padding(10)
                        .background(Color(.secondarySystemBackground))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)

                        // ── Sort / Filter Controls ────────────────────────
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(LibrarySortOrder.allCases, id: \.self) { order in
                                    MetroChip(
                                        label: order.rawValue,
                                        isSelected: viewModel.sortOrder == order,
                                        action: { viewModel.sortOrder = order }
                                    )
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                        .padding(.bottom, 8)

                        // ── Book Grid / List ──────────────────────────────
                        if showGrid {
                            BookGridView(
                                books: filteredBooks,
                                accentColor: appState.accentColor,
                                onBookTap: { book in selectedBook = book },
                                onBookDelete: { book in viewModel.confirmDelete(book) }
                            )
                        } else {
                            BookListView(
                                books: filteredBooks,
                                accentColor: appState.accentColor,
                                onBookTap: { book in selectedBook = book },
                                onBookDelete: { book in viewModel.confirmDelete(book) }
                            )
                        }

                        Spacer(minLength: 80)
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { viewModel.showImportPicker = true }) {
                        Image(systemName: "plus")
                            .foregroundStyle(appState.accentColor)
                    }
                    .accessibilityLabel("Import Book")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showGrid.toggle() }) {
                        Image(systemName: showGrid ? "list.bullet" : "square.grid.2x2")
                            .foregroundStyle(appState.accentColor)
                    }
                    .accessibilityLabel(showGrid ? "Switch to List" : "Switch to Grid")
                }
            }
            .fileImporter(
                isPresented: $viewModel.showImportPicker,
                allowedContentTypes: LibraryViewModel.supportedUTTypes,
                allowsMultipleSelection: true
            ) { result in
                switch result {
                case .success(let urls):
                    Task {
                        for url in urls {
                            await viewModel.importBook(from: url)
                        }
                    }
                case .failure(let error):
                    viewModel.importError = error.localizedDescription
                }
            }
            .alert("Delete Book", isPresented: $viewModel.showDeleteConfirm) {
                Button("Delete", role: .destructive) {
                    if let book = viewModel.bookToDelete {
                        viewModel.deleteBook(book)
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will permanently remove the book and all its annotations.")
            }
            .alert("Import Error", isPresented: .constant(viewModel.importError != nil)) {
                Button("OK") { viewModel.importError = nil }
            } message: {
                Text(viewModel.importError ?? "")
            }
            .overlay {
                if viewModel.isImporting {
                    ZStack {
                        Color.black.opacity(0.4).ignoresSafeArea()
                        VStack(spacing: 16) {
                            MetroLoadingDots(color: appState.accentColor)
                            Text("Importing…")
                                .font(MetroType.bodySans(16, weight: .light))
                                .foregroundStyle(.white)
                        }
                        .padding(32)
                        .background(Color(.systemBackground))
                    }
                }
            }
            .navigationDestination(item: $selectedBook) { book in
                ReaderView(book: book)
            }
        }
        .onAppear {
            // Handle pending import from app state
            if let url = appState.pendingImportURL {
                appState.pendingImportURL = nil
                Task { await viewModel.importBook(from: url) }
            }
        }
    }
}

// MARK: - Book Grid
struct BookGridView: View {
    let books: [Book]
    let accentColor: Color
    let onBookTap: (Book) -> Void
    let onBookDelete: (Book) -> Void

    private let columns = [
        GridItem(.adaptive(minimum: 110, maximum: 140), spacing: 12)
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 16) {
            ForEach(books) { book in
                BookGridCell(book: book, accentColor: accentColor)
                    .onTapGesture { onBookTap(book) }
                    .contextMenu {
                        Button(role: .destructive) { onBookDelete(book) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
            }
        }
        .padding(.horizontal, 20)
    }
}

struct BookGridCell: View {
    let book: Book
    let accentColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            BookCoverView(
                coverData: book.coverData,
                title: book.title,
                accentColor: accentColor.opacity(0.8)
            )
            .aspectRatio(0.65, contentMode: .fit)
            .overlay(alignment: .bottom) {
                if book.progressPercent > 0 {
                    MetroProgressBar(progress: book.progressPercent / 100, color: accentColor, height: 3)
                }
            }

            Text(book.title)
                .font(MetroType.label(12, weight: .medium))
                .lineLimit(2)
                .foregroundStyle(.primary)

            if !book.author.isEmpty {
                Text(book.author)
                    .font(MetroType.caption())
                    .lineLimit(1)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Book List
struct BookListView: View {
    let books: [Book]
    let accentColor: Color
    let onBookTap: (Book) -> Void
    let onBookDelete: (Book) -> Void

    var body: some View {
        LazyVStack(spacing: 0) {
            ForEach(books) { book in
                BookListCell(book: book, accentColor: accentColor)
                    .onTapGesture { onBookTap(book) }
                    .contextMenu {
                        Button(role: .destructive) { onBookDelete(book) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                MetroDivider().padding(.leading, 76)
            }
        }
    }
}

struct BookListCell: View {
    let book: Book
    let accentColor: Color

    var body: some View {
        HStack(spacing: 12) {
            BookCoverView(
                coverData: book.coverData,
                title: book.title,
                accentColor: accentColor.opacity(0.8)
            )
            .frame(width: 48, height: 68)

            VStack(alignment: .leading, spacing: 4) {
                Text(book.title)
                    .font(MetroType.bodySans(16, weight: .medium))
                    .lineLimit(1)
                if !book.author.isEmpty {
                    Text(book.author)
                        .font(MetroType.caption())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                if book.progressPercent > 0 {
                    MetroProgressBar(progress: book.progressPercent / 100, color: accentColor, height: 2)
                        .frame(maxWidth: 120)
                }
            }

            Spacer()

            Text(book.format.rawValue)
                .font(MetroType.caption())
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
    }
}

// MARK: - Empty State
struct EmptyLibraryView: View {
    let onImport: () -> Void
    @EnvironmentObject private var appState: AppState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Spacer()
            Text("no books yet")
                .font(MetroType.headline(32, weight: .thin))
                .foregroundStyle(.primary)
            Text("Import EPUB, PDF, TXT, HTML, or MOBI files to start reading.")
                .font(MetroType.bodySans())
                .foregroundStyle(.secondary)
            Spacer().frame(height: 24)
            Button(action: onImport) {
                HStack {
                    Image(systemName: "plus")
                    Text("IMPORT BOOK")
                        .font(MetroType.label(14, weight: .semibold))
                        .tracking(1.5)
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 14)
                .background(appState.accentColor)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 32)
    }
}
