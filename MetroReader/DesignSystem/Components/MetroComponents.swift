// MetroComponents.swift
// Metro Reader Design System — Reusable SwiftUI Components

import SwiftUI

// MARK: - Metro Section Header
struct MetroSectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(MetroType.headline(28, weight: .thin))
            .tracking(-1)
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 4)
    }
}

// MARK: - Metro Stat Tile
struct MetroStatTile: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(MetroType.headline(32, weight: .thin))
                .foregroundStyle(.white)
            Text(label)
                .font(MetroType.label(11, weight: .semibold))
                .tracking(2)
                .textCase(.uppercase)
                .foregroundStyle(.white.opacity(0.8))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color)
    }
}

// MARK: - Metro Chip (selection pill)
struct MetroChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(MetroType.label(11, weight: .semibold))
                .tracking(1.5)
                .foregroundStyle(isSelected ? .white : .primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? MetroColors.accentBlue : Color(.systemFill))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Metro Loading Indicator
struct MetroLoadingDots: View {
    @State private var phase: Int = 0
    let color: Color

    init(color: Color = MetroColors.accentBlue) {
        self.color = color
    }

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<3) { i in
                Circle()
                    .fill(color)
                    .frame(width: 8, height: 8)
                    .scaleEffect(phase == i ? 1.4 : 0.8)
                    .opacity(phase == i ? 1.0 : 0.4)
                    .animation(.easeInOut(duration: 0.4).delay(Double(i) * 0.15), value: phase)
            }
        }
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 450_000_000)
                phase = (phase + 1) % 3
            }
        }
    }
}

// MARK: - Metro Divider
struct MetroDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color(.separator).opacity(0.3))
            .frame(height: 1)
    }
}

// MARK: - Book Cover Image
struct BookCoverView: View {
    let coverData: Data?
    let title: String
    let accentColor: Color
    var cornerRadius: CGFloat = 0

    var body: some View {
        Group {
            if let data = coverData, let uiImage = UIImage(data: data) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else {
                GeneratedCoverView(title: title, accentColor: accentColor)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
    }
}

struct GeneratedCoverView: View {
    let title: String
    let accentColor: Color

    var body: some View {
        ZStack {
            accentColor
            VStack(alignment: .leading, spacing: 4) {
                Spacer()
                Text(title)
                    .font(MetroType.title(16, weight: .light))
                    .foregroundStyle(.white)
                    .lineLimit(4)
                    .padding(12)
            }
        }
    }
}

// MARK: - Progress Bar
struct MetroProgressBar: View {
    let progress: Double
    let color: Color
    let height: CGFloat

    init(progress: Double, color: Color = MetroColors.accentBlue, height: CGFloat = 2) {
        self.progress = progress
        self.color = color
        self.height = height
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Color(.systemFill))
                    .frame(height: height)
                Rectangle()
                    .fill(color)
                    .frame(width: geo.size.width * max(0, min(1, progress)), height: height)
                    .animation(.easeInOut(duration: 0.3), value: progress)
            }
        }
        .frame(height: height)
    }
}

// MARK: - Metro Setting Row
struct MetroSettingToggle: View {
    let icon: String
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .foregroundStyle(MetroColors.accentBlue)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(MetroType.bodySans(16, weight: .medium))
                Text(subtitle)
                    .font(MetroType.caption())
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(MetroColors.accentBlue)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
    }
}

// MARK: - Metro Setting Slider
struct MetroSettingSlider: View {
    let icon: String
    let title: String
    let displayValue: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(MetroColors.accentBlue)
                    .frame(width: 24)
                Text(title)
                    .font(MetroType.bodySans(16, weight: .medium))
                Spacer()
                Text(displayValue)
                    .font(MetroType.label(13, weight: .semibold))
                    .foregroundStyle(MetroColors.accentBlue)
            }
            .padding(.horizontal, 20)
            Slider(value: $value, in: range, step: step)
                .tint(MetroColors.accentBlue)
                .padding(.horizontal, 20)
        }
        .padding(.vertical, 6)
    }
}
