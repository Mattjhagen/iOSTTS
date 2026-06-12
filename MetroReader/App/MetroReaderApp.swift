// MetroReaderApp.swift
// Metro Reader — Native iOS Application
// Swift 6 · SwiftUI · SwiftData

import SwiftUI
import SwiftData
import AVFoundation

@main
struct MetroReaderApp: App {

    @StateObject private var appState = AppState()

    init() {
        configureAudioSession()
        configureAppearance()
    }

    var body: some Scene {
        WindowGroup {
            RootNavigationView()
                .environmentObject(appState)
                .modelContainer(MetroReaderSchema.container)
                .preferredColorScheme(appState.colorScheme)
                .onOpenURL { url in
                    Task { await appState.handleOpenURL(url) }
                }
        }
        .commands {
            // iPad keyboard shortcut commands
            CommandGroup(replacing: .newItem) {
                Button("Import Book…") {
                    appState.showImportPicker = true
                }
                .keyboardShortcut("o", modifiers: .command)
            }
        }
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, options: [.allowBluetooth, .allowAirPlay])
            try session.setActive(true)
        } catch {
            print("MetroReader: Failed to configure audio session: \(error)")
        }
    }

    private func configureAppearance() {
        // Remove UITabBar separator line for clean Metro look
        UITabBar.appearance().shadowImage = UIImage()
        UITabBar.appearance().backgroundImage = UIImage()
        // Remove UINavigationBar separator
        UINavigationBar.appearance().shadowImage = UIImage()
    }
}
