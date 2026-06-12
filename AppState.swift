// AppState.swift
// Global observable state for Metro Reader

import SwiftUI
import Combine

@MainActor
final class AppState: ObservableObject {

    // MARK: - Published State
    @Published var showImportPicker: Bool = false
    @Published var pendingImportURL: URL? = nil
    @Published var selectedTab: AppTab = .library

    // MARK: - User Preferences (persisted via UserDefaults)
    @AppStorage("isDarkTheme")       var isDarkTheme: Bool = true
    @AppStorage("accentColorHex")    var accentColorHex: String = "#0078D4"
    @AppStorage("fontSize")          var fontSize: Double = 18.0
    @AppStorage("lineSpacing")       var lineSpacing: Double = 1.6
    @AppStorage("marginHorizontal")  var marginHorizontal: Double = 20.0
    @AppStorage("readerTheme")       var readerThemeRaw: String = ReaderThemeMode.dark.rawValue
    @AppStorage("keepScreenOn")      var keepScreenOn: Bool = true
    @AppStorage("ttsSpeed")          var ttsSpeed: Float = 1.0
    @AppStorage("ttsPitch")          var ttsPitch: Float = 1.0
    @AppStorage("ttsVoiceIdentifier") var ttsVoiceIdentifier: String = ""
    @AppStorage("highlightSentence") var highlightSentence: Bool = true
    @AppStorage("autoScroll")        var autoScroll: Bool = true

    var colorScheme: ColorScheme? {
        isDarkTheme ? .dark : .light
    }

    var readerTheme: ReaderThemeMode {
        get { ReaderThemeMode(rawValue: readerThemeRaw) ?? .dark }
        set { readerThemeRaw = newValue.rawValue }
    }

    var accentColor: Color {
        Color(hex: accentColorHex) ?? MetroColors.accentBlue
    }

    // MARK: - URL Handling
    func handleOpenURL(_ url: URL) async {
        guard url.isFileURL else { return }
        pendingImportURL = url
        selectedTab = .library
    }
}

enum AppTab: String, CaseIterable {
    case library = "Library"
    case reader  = "Reader"
    case audio   = "Audio"
    case settings = "Settings"

    var systemImage: String {
        switch self {
        case .library:  return "books.vertical"
        case .reader:   return "book.open"
        case .audio:    return "headphones"
        case .settings: return "gearshape"
        }
    }
}
