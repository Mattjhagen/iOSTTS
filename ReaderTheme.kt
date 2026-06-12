package com.metroreader.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Themes available in the reader screen.
 */
enum class ReaderThemeMode {
    DARK, AMOLED, LIGHT, SEPIA
}

data class ReaderColors(
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val subtext: Color,
    val highlightSentence: Color = TtsHighlightSentence,
    val highlightParagraph: Color = TtsHighlightParagraph,
)

fun readerColorsFor(mode: ReaderThemeMode): ReaderColors = when (mode) {
    ReaderThemeMode.DARK -> ReaderColors(
        background = MetroDarkBackground,
        surface = MetroDarkSurface,
        onBackground = MetroDarkOnBackground,
        onSurface = MetroDarkOnSurface,
        subtext = MetroDarkSubtext,
    )
    ReaderThemeMode.AMOLED -> ReaderColors(
        background = MetroAmoledBackground,
        surface = MetroAmoledSurface,
        onBackground = MetroDarkOnBackground,
        onSurface = MetroDarkOnSurface,
        subtext = MetroDarkSubtext,
    )
    ReaderThemeMode.LIGHT -> ReaderColors(
        background = MetroLightBackground,
        surface = MetroLightSurface,
        onBackground = MetroLightOnBackground,
        onSurface = MetroLightOnSurface,
        subtext = MetroLightSubtext,
    )
    ReaderThemeMode.SEPIA -> ReaderColors(
        background = MetroSepiaBackground,
        surface = MetroSepiaSurface,
        onBackground = MetroSepiaOnBackground,
        onSurface = MetroSepiaOnSurface,
        subtext = MetroSepiaOnSurface.copy(alpha = 0.7f),
    )
}
