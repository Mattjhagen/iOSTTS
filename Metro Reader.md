# Metro Reader

Metro Reader is a production-quality native Android application built with Kotlin and Jetpack Compose. It combines the functionality of a modern ebook reader and an audiobook player, allowing users to import their own books and listen to them using high-quality Text-to-Speech (TTS).

The design philosophy is heavily inspired by the classic Windows Phone Metro UI, featuring large typography, clean layouts, flat design, smooth horizontal transitions, and a strong emphasis on content over chrome.

## Core Features

### 📚 Library & Import
- Import books from **EPUB, PDF, TXT, and HTML** formats.
- Supports importing via the in-app file picker, Android Share Sheet, or "Open With" integration.
- Automatic metadata extraction (title, author, cover thumbnail, estimated reading time).
- Local-only storage (Room Database) ensuring complete privacy.

### 📖 Distraction-Free Reader
- Beautiful, immersive reading experience with minimal controls.
- Adjustable font size, line spacing, and margins.
- Multiple themes: **Dark Mode, AMOLED Black, Light, and Sepia**.
- Supports bookmarks, highlights, and table of contents navigation.
- Remembers your exact reading position (scroll offset and character offset).

### 🎧 Audiobook Mode (TTS)
- Converts any imported ebook into an audiobook experience.
- Uses Android's native Text-to-Speech engine.
- Dedicated full-screen Metro audio player with cover art and progress tracking.
- Controls for Play, Pause, Stop, Seek, Next/Previous Chapter.
- Adjustable Voice, Speed, and Pitch settings.
- **Smart Sync:** Highlights the current sentence and auto-scrolls the text while audio is playing (similar to Kindle VoiceView).

### 🎨 Metro UI Design System
- Large, thin typography using the Inter font family (resembling Segoe UI Variable).
- Edge-to-edge layouts with no excessive shadows or skeuomorphism.
- Animated tiles and smooth transitions.
- Fully responsive Jetpack Compose architecture.

## Architecture & Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Dependency Injection:** Hilt
- **Local Database:** Room Database
- **Preferences:** DataStore
- **Asynchronous Programming:** Coroutines & Flow
- **Image Loading:** Coil
- **Media Playback:** Media3 (ExoPlayer) + Foreground MediaSessionService
- **Parsers:**
  - EPUB: `epublib` + `Jsoup`
  - PDF: `android.graphics.pdf.PdfRenderer`
  - HTML/TXT: Custom chunking and sentence splitting algorithms

## Privacy & Security

Metro Reader is designed with a strict offline-first, privacy-focused approach:
- **No cloud account required.**
- **All data remains on-device.**
- **No analytics, no tracking, no advertisements, no telemetry.**

## Build Instructions

1. Open the project in **Android Studio** (Ladybug or later recommended).
2. Sync the project with Gradle files.
3. Build and run the app on an emulator or physical device running Android 8.0 (API 26) or higher.

To build a release APK from the command line:
```bash
./gradlew assembleRelease
```

## License

This project is open-source and provided under the MIT License.
