# Metro Reader (iOS)

Metro Reader is a production-quality, native iOS ebook and audiobook reader built with Swift 6, SwiftUI, and SwiftData. It brings a clean, typography-focused Metro design language to the iOS reading experience.

This is the exact feature-equivalent of the Android version, rewritten entirely from scratch using Apple's modern native frameworks.

## Features

- **Native SwiftUI Architecture**: Built with Swift 6 concurrency, modern SwiftUI navigation (`TabView` for iPhone, `NavigationSplitView` for iPad), and the MVVM pattern.
- **Offline-First Persistence**: Uses SwiftData to securely store your library, reading progress, bookmarks, highlights, and notes entirely on-device. No accounts, no cloud tracking.
- **Universal Format Support**: Import EPUB, PDF, TXT, HTML, MOBI, and AZW3 files directly from the iOS Files app, AirDrop, or Share Sheet.
- **Premium Reader**: Customizable typography (SF Pro and system serif), adjustable margins, line spacing, and 4 Metro themes (Dark, AMOLED, Light, Sepia).
- **TTS Audiobook Engine**: Converts any text-based book into an audiobook using `AVSpeechSynthesizer` and Apple Neural Voices. Features sentence-level highlighting, auto-scrolling, and lock-screen/Control Center integration via `MPRemoteCommandCenter`.
- **Metro Design System**: Large lowercase headers, strict grid alignments, animated flat tiles, and 8 customizable accent colors.

## Requirements

- **iOS / iPadOS**: 18.0+
- **Xcode**: 16.0+
- **Language**: Swift 6

## Project Structure

```text
MetroReader/
├── App/                # App entry point, AppState, Info.plist
├── DesignSystem/       # Metro colors, typography, and reusable UI components
├── Data/               # SwiftData @Model definitions and Repositories
├── Parsers/            # EPUB, PDF, TXT, HTML, and MOBI parsing engines
├── Features/           # UI Screens (Library, Reader, Audio, Settings)
├── Services/           # Import orchestrator and TTS engine
├── Navigation/         # Responsive root navigation
└── Resources/          # Asset catalog
```

## Build Instructions

1. Open `MetroReader.xcodeproj` in Xcode 16 or later.
2. Swift Package Manager will automatically resolve dependencies:
   - `swift-toolkit` (Readium for advanced EPUB support)
   - `ZIPFoundation` (for local archive extraction)
3. Select an iOS 18 simulator or device.
4. Build and Run (`Cmd + R`).

## Privacy & Telemetry

Metro Reader is designed to respect user privacy:
- `NSPrivacyAccessedAPITypes` is strictly limited.
- No analytics SDKs are included.
- No network requests are made except for local file handling.
- All TTS processing is done locally via the on-device Neural Engine.

---
*Built as a native iOS equivalent to the Android Metro Reader project.*
