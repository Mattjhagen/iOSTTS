// MetroReaderSchema.swift
// SwiftData container configuration

import SwiftData
import Foundation

enum MetroReaderSchema {

    static let schema = Schema([
        Book.self,
        ReadingProgress.self,
        Bookmark.self,
        Highlight.self,
        Note.self,
        TtsProgress.self,
    ])

    static let modelConfiguration = ModelConfiguration(
        schema: schema,
        isStoredInMemoryOnly: false,
        allowsSave: true,
        cloudKitDatabase: .none  // Offline-first, no CloudKit
    )

    @MainActor
    static var container: ModelContainer = {
        do {
            return try ModelContainer(
                for: schema,
                configurations: [modelConfiguration]
            )
        } catch {
            fatalError("MetroReader: Failed to create SwiftData container: \(error)")
        }
    }()
}
