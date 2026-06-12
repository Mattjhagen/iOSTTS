// MetroTypography.swift
// Metro Reader Design System — Typography
// Uses SF Pro (system font) with Inter-inspired weight/tracking strategy

import SwiftUI

// MARK: - Metro Type Scale
enum MetroType {

    // MARK: Display — large hero text (like Metro tile titles)
    static func display(_ size: CGFloat = 64, weight: Font.Weight = .ultraLight) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Headline
    static func headline(_ size: CGFloat = 34, weight: Font.Weight = .thin) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Title
    static func title(_ size: CGFloat = 22, weight: Font.Weight = .light) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Body — reading text
    static func body(_ size: CGFloat = 18, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .serif)
    }

    // MARK: Body Sans — UI text
    static func bodySans(_ size: CGFloat = 16, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Label
    static func label(_ size: CGFloat = 12, weight: Font.Weight = .medium) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Caption
    static func caption(_ size: CGFloat = 11, weight: Font.Weight = .regular) -> Font {
        .system(size: size, weight: weight, design: .default)
    }

    // MARK: Reader Body — scaled for Dynamic Type
    static func readerBody(size: Double) -> Font {
        .system(size: size, weight: .regular, design: .serif)
    }

    // MARK: Reader Chapter Title
    static func readerChapterTitle(size: Double) -> Font {
        .system(size: size * 1.4, weight: .thin, design: .default)
    }
}

// MARK: - Tracking Modifiers
extension View {
    /// Apply Metro-style tight tracking to display text
    func metroDisplayTracking() -> some View {
        self.tracking(-2)
    }

    /// Apply Metro-style standard tracking to headline text
    func metroHeadlineTracking() -> some View {
        self.tracking(-1)
    }

    /// Apply Metro-style label tracking (uppercase labels)
    func metroLabelTracking() -> some View {
        self.tracking(1.5)
    }
}

// MARK: - Text Style Modifiers
struct MetroDisplayStyle: ViewModifier {
    let color: Color
    func body(content: Content) -> some View {
        content
            .font(MetroType.display())
            .tracking(-3)
            .foregroundStyle(color)
    }
}

struct MetroHeadlineStyle: ViewModifier {
    let color: Color
    func body(content: Content) -> some View {
        content
            .font(MetroType.headline())
            .tracking(-1)
            .foregroundStyle(color)
    }
}

struct MetroSectionLabelStyle: ViewModifier {
    let color: Color
    func body(content: Content) -> some View {
        content
            .font(MetroType.label(12, weight: .semibold))
            .tracking(2)
            .textCase(.uppercase)
            .foregroundStyle(color)
    }
}

extension View {
    func metroDisplayStyle(color: Color = .primary) -> some View {
        modifier(MetroDisplayStyle(color: color))
    }
    func metroHeadlineStyle(color: Color = .primary) -> some View {
        modifier(MetroHeadlineStyle(color: color))
    }
    func metroSectionLabel(color: Color = .secondary) -> some View {
        modifier(MetroSectionLabelStyle(color: color))
    }
}
