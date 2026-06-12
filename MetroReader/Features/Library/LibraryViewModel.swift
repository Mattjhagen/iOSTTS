// LibraryViewModel.swift
// Metro Reader — Library Feature

import SwiftUI
import SwiftData
import Combine
import UniformTypeIdentifiers

@MainActor
final class LibraryViewModel: ObservableObject {

    @Published var searchText: String = ""
    @Published var sortOrder: LibrarySortOrder = .recentlyOpened
    @Published var filterFormat: BookFormat? = nil
    @Published var showImportPicker: Bool = false
    @Published var isImporting: Bool = false
    @Published var importError: String? = nil
    @Published var selectedBook: Book? = nil
    @Published var showDeleteConfirm: Bool = false
    @Published var bookToDelete: Book? = nil

    private let repository: BookRepository
    private let importService: BookImportService
    private var cancellables = Set<AnyCancellable>()

    init(repository: BookRepository) {
        self.repository = repository
        self.importService = BookImportService(repository: repository)

        importService.$isImporting
            .assign(to: &$isImporting)
        importService.$importError
            .assign(to: &$importError)
    }

    // MARK: - Filtered Books
    func filteredBooks(from books: [Book]) -> [Book] {
        var result = books

        // Search filter
        if !searchText.isEmpty {
            result = result.filter {
                $0.title.localizedCaseInsensitiveContains(searchText) ||
                $0.author.localizedCaseInsensitiveContains(searchText)
            }
        }

        // Format filter
        if let format = filterFormat {
            result = result.filter { $0.format == format }
        }

        // Sort
        switch sortOrder {
        case .recentlyOpened:
            result.sort {
                ($0.lastOpenedDate ?? $0.addedDate) > ($1.lastOpenedDate ?? $1.addedDate)
            }
        case .title:
            result.sort { $0.title.localizedCompare($1.title) == .orderedAscending }
        case .author:
            result.sort { $0.author.localizedCompare($1.author) == .orderedAscending }
        case .dateAdded:
            result.sort { $0.addedDate > $1.addedDate }
        case .progress:
            result.sort { $0.progressPercent > $1.progressPercent }
        }

        return result
    }

    // MARK: - Import
    func importBook(from url: URL) async {
        await importService.importBook(from: url)
    }

    // MARK: - Delete
    func confirmDelete(_ book: Book) {
        bookToDelete = book
        showDeleteConfirm = true
    }

    func deleteBook(_ book: Book) {
        repository.delete(book)
        if selectedBook?.id == book.id { selectedBook = nil }
    }

    // MARK: - Supported UTTypes for document picker
    static var supportedUTTypes: [UTType] {
        [
            UTType(filenameExtension: "epub") ?? .data,
            .pdf,
            .plainText,
            .html,
            UTType(filenameExtension: "mobi") ?? .data,
            UTType(filenameExtension: "azw3") ?? .data,
        ]
    }
}

enum LibrarySortOrder: String, CaseIterable {
    case recentlyOpened = "Recently Opened"
    case title          = "Title"
    case author         = "Author"
    case dateAdded      = "Date Added"
    case progress       = "Progress"
}
