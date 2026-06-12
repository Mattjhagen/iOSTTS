// BookImportService.swift
// Metro Reader — Book Import Orchestrator

import Foundation
import SwiftData
import UIKit

@MainActor
final class BookImportService: ObservableObject {

    @Published var isImporting: Bool = false
    @Published var importError: String? = nil
    @Published var lastImportedBook: Book? = nil

    private let repository: BookRepository
    private let parsers: [any BookParser]

    init(repository: BookRepository) {
        self.repository = repository
        self.parsers = [
            EPUBParser(),
            PDFParser(),
            TxtParser(),
            HTMLParser(),
            MOBIParser(),
        ]
    }

    // MARK: - Import from URL (Files app, AirDrop, Share Sheet, Open In)
    func importBook(from url: URL) async {
        isImporting = true
        importError = nil

        do {
            // Gain security-scoped access
            let didStartAccess = url.startAccessingSecurityScopedResource()
            defer {
                if didStartAccess { url.stopAccessingSecurityScopedResource() }
            }

            // Determine format
            let ext = url.pathExtension.lowercased()
            guard let format = BookFormat.from(extension: ext) else {
                throw ImportError.unsupportedFormat(ext)
            }

            // Copy file to app's Documents directory
            let destURL = try copyToDocuments(url: url)
            let relativePath = destURL.lastPathComponent

            // Find appropriate parser
            guard let parser = parsers.first(where: { $0.supports(format: format) }) else {
                throw ImportError.noParserAvailable(format)
            }

            // Parse metadata
            let metadata = try await parser.parseMetadata(from: destURL, format: format)

            // Check for duplicate (same file path or same title+author)
            if let existing = try? repository.fetchAllBooks().first(where: {
                $0.filePath == relativePath || ($0.title == metadata.title && $0.author == metadata.author)
            }) {
                lastImportedBook = existing
                isImporting = false
                return
            }

            // Create Book record
            let fileSize = (try? FileManager.default.attributesOfItem(atPath: destURL.path)[.size] as? Int64) ?? 0
            let book = Book(
                title: metadata.title,
                author: metadata.author,
                publisher: metadata.publisher,
                language: metadata.language,
                isbn: metadata.isbn,
                description: metadata.description,
                format: format,
                filePath: relativePath,
                coverData: metadata.coverData,
                accentColorHex: metadata.coverData.flatMap { extractDominantColor($0) } ?? "#0078D4",
                fileSize: fileSize,
                pageCount: metadata.pageCount
            )

            repository.insert(book)
            lastImportedBook = book

        } catch {
            importError = error.localizedDescription
        }

        isImporting = false
    }

    // MARK: - Helpers

    private func copyToDocuments(url: URL) throws -> URL {
        let docsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let booksDir = docsURL.appendingPathComponent("Books", isDirectory: true)
        try FileManager.default.createDirectory(at: booksDir, withIntermediateDirectories: true)

        var destURL = booksDir.appendingPathComponent(url.lastPathComponent)
        // Handle name collision
        var counter = 1
        while FileManager.default.fileExists(atPath: destURL.path) {
            let name = url.deletingPathExtension().lastPathComponent
            let ext  = url.pathExtension
            destURL = booksDir.appendingPathComponent("\(name)_\(counter).\(ext)")
            counter += 1
        }

        try FileManager.default.copyItem(at: url, to: destURL)
        return destURL
    }

    private func extractDominantColor(_ data: Data) -> String? {
        guard let image = UIImage(data: data),
              let cgImage = image.cgImage else { return nil }

        // Sample a small region of the image for dominant color
        let size = CGSize(width: 10, height: 10)
        UIGraphicsBeginImageContext(size)
        UIImage(cgImage: cgImage).draw(in: CGRect(origin: .zero, size: size))
        let resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        guard let pixelData = resized?.cgImage?.dataProvider?.data,
              let data = CFDataGetBytePtr(pixelData) else { return nil }

        let r = Int(data[0])
        let g = Int(data[1])
        let b = Int(data[2])
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}

// MARK: - Import Errors
enum ImportError: LocalizedError {
    case unsupportedFormat(String)
    case noParserAvailable(BookFormat)
    case fileAccessDenied
    case parseFailure(String)

    var errorDescription: String? {
        switch self {
        case .unsupportedFormat(let ext):
            return "The file format '.\(ext)' is not supported."
        case .noParserAvailable(let format):
            return "No parser available for \(format.rawValue) files."
        case .fileAccessDenied:
            return "Cannot access the selected file."
        case .parseFailure(let reason):
            return "Failed to parse the book: \(reason)"
        }
    }
}
