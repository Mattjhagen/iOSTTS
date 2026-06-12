// RootNavigationView.swift
// Metro Reader — Root Navigation
// TabView on iPhone, NavigationSplitView on iPad

import SwiftUI
import SwiftData

struct RootNavigationView: View {

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var appState: AppState
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        if horizontalSizeClass == .regular {
            // ── iPad: NavigationSplitView ─────────────────────────────────
            iPadLayout
        } else {
            // ── iPhone: TabView ───────────────────────────────────────────
            iPhoneLayout
        }
    }

    // MARK: - iPhone Tab Bar
    private var iPhoneLayout: some View {
        TabView(selection: $appState.selectedTab) {
            HomeView()
                .tabItem {
                    Label("Home", systemImage: "house")
                }
                .tag(AppTab.library)

            LibraryView(modelContext: modelContext)
                .tabItem {
                    Label("Library", systemImage: "books.vertical")
                }
                .tag(AppTab.reader)

            AudioView(modelContext: modelContext)
                .tabItem {
                    Label("Audio", systemImage: "headphones")
                }
                .tag(AppTab.audio)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(AppTab.settings)
        }
        .tint(appState.accentColor)
        .preferredColorScheme(appState.colorScheme)
    }

    // MARK: - iPad Split View
    private var iPadLayout: some View {
        NavigationSplitView {
            // Sidebar
            List(selection: Binding(
                get: { appState.selectedTab },
                set: { if let t = $0 { appState.selectedTab = t } }
            )) {
                Section {
                    ForEach(AppTab.allCases, id: \.self) { tab in
                        NavigationLink(value: tab) {
                            Label(tab.rawValue, systemImage: tab.systemImage)
                        }
                    }
                }
            }
            .navigationTitle("Metro Reader")
            .listStyle(.sidebar)
        } detail: {
            switch appState.selectedTab {
            case .library:
                HomeView()
            case .reader:
                LibraryView(modelContext: modelContext)
            case .audio:
                AudioView(modelContext: modelContext)
            case .settings:
                SettingsView()
            }
        }
        .tint(appState.accentColor)
        .preferredColorScheme(appState.colorScheme)
    }
}
