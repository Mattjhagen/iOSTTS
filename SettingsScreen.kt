package com.metroreader.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroreader.app.ui.components.MetroDivider
import com.metroreader.app.ui.components.MetroSectionHeader
import com.metroreader.app.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "settings",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // ── Appearance ────────────────────────────────────────────────────────
        item { MetroSectionHeader(title = "appearance") }
        item {
            SettingToggle(
                icon = Icons.Filled.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme throughout the app",
                checked = uiState.isDarkTheme,
                onCheckedChange = viewModel::setDarkTheme
            )
        }
        item {
            SettingToggle(
                icon = Icons.Filled.ScreenLockPortrait,
                title = "Keep Screen On",
                subtitle = "Prevent screen from sleeping while reading",
                checked = uiState.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn
            )
        }

        // ── Accent Color ──────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Accent Color",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val accents = listOf(
                        "#0078D4" to MetroAccentBlue,
                        "#00B4D8" to MetroAccentCyan,
                        "#107C10" to MetroAccentGreen,
                        "#8764B8" to MetroAccentPurple,
                        "#CA5010" to MetroAccentOrange,
                        "#D13438" to MetroAccentRed,
                    )
                    accents.forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, RoundedCornerShape(0.dp))
                                .clickable { viewModel.setAccentColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.accentColor == hex) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { MetroDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }

        // ── Reading ───────────────────────────────────────────────────────────
        item { MetroSectionHeader(title = "reading") }
        item {
            SettingSliderItem(
                icon = Icons.Filled.TextFields,
                title = "Font Size",
                value = uiState.fontSize,
                valueRange = 12f..32f,
                displayValue = "${uiState.fontSize.toInt()}sp",
                onValueChange = viewModel::setFontSize
            )
        }
        item {
            SettingSliderItem(
                icon = Icons.Filled.FormatLineSpacing,
                title = "Line Spacing",
                value = uiState.lineSpacing,
                valueRange = 1.0f..2.5f,
                displayValue = String.format("%.1f", uiState.lineSpacing),
                onValueChange = viewModel::setLineSpacing
            )
        }

        item { MetroDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }

        // ── Audio / TTS ───────────────────────────────────────────────────────
        item { MetroSectionHeader(title = "audio") }
        item {
            SettingSliderItem(
                icon = Icons.Filled.Speed,
                title = "TTS Speed",
                value = uiState.ttsSpeed,
                valueRange = 0.25f..4f,
                displayValue = "${uiState.ttsSpeed}x",
                onValueChange = viewModel::setTtsSpeed
            )
        }
        item {
            SettingToggle(
                icon = Icons.Filled.Highlight,
                title = "Highlight Sentences",
                subtitle = "Highlight current sentence during TTS playback",
                checked = uiState.highlightSentence,
                onCheckedChange = viewModel::setHighlightSentence
            )
        }
        item {
            SettingToggle(
                icon = Icons.Filled.AutoScroll,
                title = "Auto-Scroll",
                subtitle = "Automatically scroll text during TTS playback",
                checked = uiState.autoScroll,
                onCheckedChange = viewModel::setAutoScroll
            )
        }

        item { MetroDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }

        // ── About ─────────────────────────────────────────────────────────────
        item { MetroSectionHeader(title = "about") }
        item {
            SettingInfoItem(
                icon = Icons.Filled.Info,
                title = "Metro Reader",
                subtitle = "Version 1.0.0 · No cloud · No tracking · No ads"
            )
        }
        item {
            SettingInfoItem(
                icon = Icons.Filled.Code,
                title = "Open Source",
                subtitle = "Built with Kotlin + Jetpack Compose"
            )
        }
    }
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}

@Composable
private fun SettingSliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily), modifier = Modifier.weight(1f))
            Text(
                displayValue,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(start = 40.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}

@Composable
private fun SettingInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
