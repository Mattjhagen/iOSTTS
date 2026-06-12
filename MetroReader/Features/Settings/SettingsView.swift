// SettingsView.swift
// Metro Reader — Settings Screen

import SwiftUI

struct SettingsView: View {

    @EnvironmentObject private var appState: AppState
    @State private var showAbout: Bool = false

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {

                    // ── Title ─────────────────────────────────────────────
                    VStack(alignment: .leading, spacing: 0) {
                        Text("metro")
                            .font(MetroType.display(48, weight: .ultraLight))
                            .tracking(-3)
                            .foregroundStyle(.primary)
                        Text("settings")
                            .font(MetroType.display(48, weight: .ultraLight))
                            .tracking(-3)
                            .foregroundStyle(appState.accentColor)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 24)

                    // ── Appearance ────────────────────────────────────────
                    MetroSectionHeader(title: "appearance")

                    MetroSettingToggle(
                        icon: "moon.fill",
                        title: "Dark Mode",
                        subtitle: "Use dark background throughout the app",
                        isOn: $appState.isDarkTheme
                    )

                    // Accent Color
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "paintpalette")
                                .foregroundStyle(appState.accentColor)
                                .frame(width: 24)
                            Text("Accent Color")
                                .font(MetroType.bodySans(16, weight: .medium))
                        }
                        .padding(.horizontal, 20)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 10) {
                                ForEach(MetroColors.accentPresets, id: \.hex) { preset in
                                    Button(action: { appState.accentColorHex = preset.hex }) {
                                        ZStack {
                                            Circle()
                                                .fill(preset.color)
                                                .frame(width: 36, height: 36)
                                            if appState.accentColorHex == preset.hex {
                                                Circle()
                                                    .strokeBorder(.white, lineWidth: 2)
                                                    .frame(width: 36, height: 36)
                                                Image(systemName: "checkmark")
                                                    .font(.caption2)
                                                    .fontWeight(.bold)
                                                    .foregroundStyle(.white)
                                            }
                                        }
                                    }
                                    .accessibilityLabel("\(preset.name) accent color")
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                    }
                    .padding(.vertical, 10)

                    MetroDivider().padding(.horizontal, 20)

                    // ── Reader ────────────────────────────────────────────
                    MetroSectionHeader(title: "reader")

                    // Reader Theme
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "textformat")
                                .foregroundStyle(appState.accentColor)
                                .frame(width: 24)
                            Text("Reading Theme")
                                .font(MetroType.bodySans(16, weight: .medium))
                        }
                        .padding(.horizontal, 20)

                        HStack(spacing: 8) {
                            ForEach(ReaderThemeMode.allCases, id: \.self) { theme in
                                Button(action: { appState.readerTheme = theme }) {
                                    VStack(spacing: 4) {
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(theme.colors().background)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 4)
                                                    .strokeBorder(
                                                        appState.readerTheme == theme ? appState.accentColor : Color.clear,
                                                        lineWidth: 2
                                                    )
                                            )
                                            .frame(width: 48, height: 64)
                                            .overlay {
                                                VStack(spacing: 2) {
                                                    ForEach(0..<3) { _ in
                                                        RoundedRectangle(cornerRadius: 1)
                                                            .fill(theme.colors().onBackground.opacity(0.6))
                                                            .frame(width: 28, height: 2)
                                                    }
                                                }
                                            }
                                        Text(theme.displayName)
                                            .font(MetroType.caption())
                                            .foregroundStyle(appState.readerTheme == theme ? appState.accentColor : .secondary)
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.vertical, 10)

                    MetroSettingSlider(
                        icon: "textformat.size",
                        title: "Font Size",
                        displayValue: "\(Int(appState.fontSize))pt",
                        value: $appState.fontSize,
                        range: 12...32,
                        step: 1
                    )

                    MetroSettingSlider(
                        icon: "line.3.horizontal",
                        title: "Line Spacing",
                        displayValue: String(format: "%.1f", appState.lineSpacing),
                        value: $appState.lineSpacing,
                        range: 1.2...2.4,
                        step: 0.1
                    )

                    MetroSettingSlider(
                        icon: "arrow.left.and.right",
                        title: "Margins",
                        displayValue: "\(Int(appState.marginHorizontal))pt",
                        value: $appState.marginHorizontal,
                        range: 8...48,
                        step: 4
                    )

                    MetroSettingToggle(
                        icon: "sun.max",
                        title: "Keep Screen On",
                        subtitle: "Prevent screen from sleeping while reading",
                        isOn: $appState.keepScreenOn
                    )

                    MetroDivider().padding(.horizontal, 20)

                    // ── Audio / TTS ───────────────────────────────────────
                    MetroSectionHeader(title: "audio")

                    MetroSettingSlider(
                        icon: "speedometer",
                        title: "Narration Speed",
                        displayValue: String(format: "%.2fx", appState.ttsSpeed),
                        value: $appState.ttsSpeed,
                        range: 0.1...1.0,
                        step: 0.05
                    )

                    MetroSettingSlider(
                        icon: "waveform",
                        title: "Voice Pitch",
                        displayValue: String(format: "%.1f", appState.ttsPitch),
                        value: $appState.ttsPitch,
                        range: 0.5...2.0,
                        step: 0.1
                    )

                    MetroSettingToggle(
                        icon: "text.word.spacing",
                        title: "Highlight Sentence",
                        subtitle: "Highlight the sentence currently being read",
                        isOn: $appState.highlightSentence
                    )

                    MetroSettingToggle(
                        icon: "arrow.up.and.down.text.horizontal",
                        title: "Auto-Scroll",
                        subtitle: "Automatically scroll to the spoken sentence",
                        isOn: $appState.autoScroll
                    )

                    MetroDivider().padding(.horizontal, 20)

                    // ── About ─────────────────────────────────────────────
                    MetroSectionHeader(title: "about")

                    Button(action: { showAbout = true }) {
                        HStack {
                            Image(systemName: "info.circle")
                                .foregroundStyle(appState.accentColor)
                                .frame(width: 24)
                            Text("About Metro Reader")
                                .font(MetroType.bodySans(16, weight: .medium))
                                .foregroundStyle(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)

                    Spacer(minLength: 80)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showAbout) {
                AboutSheet()
            }
        }
    }
}

// MARK: - About Sheet
struct AboutSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var appState: AppState

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("metro")
                        .font(MetroType.display(48, weight: .ultraLight))
                        .tracking(-3)
                    Text("reader")
                        .font(MetroType.display(48, weight: .ultraLight))
                        .tracking(-3)
                        .foregroundStyle(appState.accentColor)
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Version 1.0")
                        .font(MetroType.bodySans(16, weight: .medium))
                    Text("A premium ebook and audiobook reader inspired by Windows Phone Metro design. No accounts. No analytics. No tracking. Everything stays on your device.")
                        .font(MetroType.bodySans())
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 24)

                Spacer()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
