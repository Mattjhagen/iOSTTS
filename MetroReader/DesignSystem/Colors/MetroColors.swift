// MetroColors.swift
// Metro Reader Design System — Color Palette

import SwiftUI

// MARK: - Metro Accent Colors
enum MetroColors {
    static let accentBlue   = Color(hex: "#0078D4")!
    static let accentCyan   = Color(hex: "#00B4D8")!
    static let accentGreen  = Color(hex: "#107C10")!
    static let accentPurple = Color(hex: "#8764B8")!
    static let accentOrange = Color(hex: "#CA5010")!
    static let accentRed    = Color(hex: "#D13438")!
    static let accentTeal   = Color(hex: "#038387")!
    static let accentGold   = Color(hex: "#C19C00")!

    // MARK: - Background Scales
    static let backgroundDark    = Color(hex: "#0D0D0D")!
    static let backgroundAmoled  = Color(hex: "#000000")!
    static let backgroundLight   = Color(hex: "#FAFAFA")!
    static let backgroundSepia   = Color(hex: "#F5ECD7")!

    // MARK: - Surface
    static let surfaceDark   = Color(hex: "#1A1A1A")!
    static let surfaceLight  = Color(hex: "#F0F0F0")!
    static let surfaceSepia  = Color(hex: "#EDE0C8")!

    // MARK: - Text
    static let textPrimaryDark   = Color(hex: "#F5F5F5")!
    static let textPrimaryLight  = Color(hex: "#1A1A1A")!
    static let textPrimarySepia  = Color(hex: "#3B2A1A")!
    static let textSecondaryDark = Color(hex: "#9E9E9E")!
    static let textSecondaryLight = Color(hex: "#6B6B6B")!

    // MARK: - Highlight
    static let highlightSentenceDark  = Color(hex: "#0078D4")!.opacity(0.25)
    static let highlightSentenceLight = Color(hex: "#0078D4")!.opacity(0.15)
    static let highlightSentenceSepia = Color(hex: "#CA5010")!.opacity(0.20)

    // MARK: - Accent Color Presets (for Settings)
    static let accentPresets: [(name: String, color: Color, hex: String)] = [
        ("Blue",   accentBlue,   "#0078D4"),
        ("Cyan",   accentCyan,   "#00B4D8"),
        ("Green",  accentGreen,  "#107C10"),
        ("Purple", accentPurple, "#8764B8"),
        ("Orange", accentOrange, "#CA5010"),
        ("Red",    accentRed,    "#D13438"),
        ("Teal",   accentTeal,   "#038387"),
        ("Gold",   accentGold,   "#C19C00"),
    ]
}

// MARK: - Reader Theme Colors
struct ReaderColors {
    let background: Color
    let surface: Color
    let onBackground: Color
    let subtext: Color
    let highlightSentence: Color
    let accentColor: Color
}

enum ReaderThemeMode: String, CaseIterable, Codable {
    case dark   = "dark"
    case amoled = "amoled"
    case light  = "light"
    case sepia  = "sepia"

    var displayName: String {
        switch self {
        case .dark:   return "Dark"
        case .amoled: return "AMOLED"
        case .light:  return "Light"
        case .sepia:  return "Sepia"
        }
    }

    func colors(accent: Color = MetroColors.accentBlue) -> ReaderColors {
        switch self {
        case .dark:
            return ReaderColors(
                background: MetroColors.backgroundDark,
                surface: MetroColors.surfaceDark,
                onBackground: MetroColors.textPrimaryDark,
                subtext: MetroColors.textSecondaryDark,
                highlightSentence: MetroColors.highlightSentenceDark,
                accentColor: accent
            )
        case .amoled:
            return ReaderColors(
                background: MetroColors.backgroundAmoled,
                surface: Color(hex: "#111111")!,
                onBackground: MetroColors.textPrimaryDark,
                subtext: MetroColors.textSecondaryDark,
                highlightSentence: accent.opacity(0.2),
                accentColor: accent
            )
        case .light:
            return ReaderColors(
                background: MetroColors.backgroundLight,
                surface: MetroColors.surfaceLight,
                onBackground: MetroColors.textPrimaryLight,
                subtext: MetroColors.textSecondaryLight,
                highlightSentence: MetroColors.highlightSentenceLight,
                accentColor: accent
            )
        case .sepia:
            return ReaderColors(
                background: MetroColors.backgroundSepia,
                surface: MetroColors.surfaceSepia,
                onBackground: MetroColors.textPrimarySepia,
                subtext: Color(hex: "#7A6040")!,
                highlightSentence: MetroColors.highlightSentenceSepia,
                accentColor: MetroColors.accentOrange
            )
        }
    }
}

// MARK: - Color Hex Extension
extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.hasPrefix("#") ? String(hexSanitized.dropFirst()) : hexSanitized

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        let length = hexSanitized.count
        switch length {
        case 6:
            self.init(
                red:   Double((rgb & 0xFF0000) >> 16) / 255.0,
                green: Double((rgb & 0x00FF00) >> 8)  / 255.0,
                blue:  Double( rgb & 0x0000FF)         / 255.0
            )
        case 8:
            self.init(
                red:   Double((rgb & 0xFF000000) >> 24) / 255.0,
                green: Double((rgb & 0x00FF0000) >> 16) / 255.0,
                blue:  Double((rgb & 0x0000FF00) >> 8)  / 255.0,
                opacity: Double( rgb & 0x000000FF)       / 255.0
            )
        default:
            return nil
        }
    }

    var hexString: String {
        let components = UIColor(self).cgColor.components ?? [0, 0, 0, 1]
        let r = Int((components[0] * 255).rounded())
        let g = Int((components[1] * 255).rounded())
        let b = Int((components[2] * 255).rounded())
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}
