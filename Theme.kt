package com.metroreader.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MetroDarkColorScheme = darkColorScheme(
    primary             = MetroAccentBlue,
    onPrimary           = MetroDarkOnBackground,
    primaryContainer    = MetroAccentBlue.copy(alpha = 0.2f),
    onPrimaryContainer  = MetroDarkOnBackground,
    secondary           = MetroAccentCyan,
    onSecondary         = MetroDarkOnBackground,
    secondaryContainer  = MetroAccentCyan.copy(alpha = 0.2f),
    onSecondaryContainer= MetroDarkOnBackground,
    tertiary            = MetroAccentPurple,
    onTertiary          = MetroDarkOnBackground,
    background          = MetroDarkBackground,
    onBackground        = MetroDarkOnBackground,
    surface             = MetroDarkSurface,
    onSurface           = MetroDarkOnSurface,
    surfaceVariant      = MetroDarkSurfaceVariant,
    onSurfaceVariant    = MetroDarkSubtext,
    outline             = MetroDarkSubtext.copy(alpha = 0.5f),
    error               = MetroError,
    onError             = MetroDarkOnBackground,
)

private val MetroLightColorScheme = lightColorScheme(
    primary             = MetroAccentBlue,
    onPrimary           = MetroLightBackground,
    primaryContainer    = MetroAccentBlue.copy(alpha = 0.1f),
    onPrimaryContainer  = MetroLightOnBackground,
    secondary           = MetroAccentCyan,
    onSecondary         = MetroLightBackground,
    secondaryContainer  = MetroAccentCyan.copy(alpha = 0.1f),
    onSecondaryContainer= MetroLightOnBackground,
    tertiary            = MetroAccentPurple,
    onTertiary          = MetroLightBackground,
    background          = MetroLightBackground,
    onBackground        = MetroLightOnBackground,
    surface             = MetroLightSurface,
    onSurface           = MetroLightOnSurface,
    surfaceVariant      = MetroLightSurfaceVariant,
    onSurfaceVariant    = MetroLightSubtext,
    outline             = MetroLightSubtext.copy(alpha = 0.5f),
    error               = MetroError,
    onError             = MetroLightBackground,
)

@Composable
fun MetroReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MetroDarkColorScheme else MetroLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MetroTypography,
        content     = content
    )
}
