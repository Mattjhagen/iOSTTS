package com.metroreader.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroreader.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val accentColor: String = "#0078D4",
    val fontSize: Float = 18f,
    val lineSpacing: Float = 1.6f,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val keepScreenOn: Boolean = true,
    val highlightSentence: Boolean = true,
    val autoScroll: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefsRepository.isDarkTheme,
                prefsRepository.accentColor,
                prefsRepository.readerPreferences,
                prefsRepository.ttsPreferences,
                prefsRepository.keepScreenOn,
            ) { dark, accent, reader, tts, keepOn ->
                SettingsUiState(
                    isDarkTheme = dark,
                    accentColor = accent,
                    fontSize = reader.fontSize,
                    lineSpacing = reader.lineSpacing,
                    ttsSpeed = tts.speed,
                    ttsPitch = tts.pitch,
                    keepScreenOn = keepOn,
                    highlightSentence = tts.highlightSentence,
                    autoScroll = tts.autoScroll,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { prefsRepository.setDarkTheme(dark) }
    fun setAccentColor(hex: String) = viewModelScope.launch { prefsRepository.setAccentColor(hex) }
    fun setFontSize(size: Float) = viewModelScope.launch { prefsRepository.setFontSize(size) }
    fun setLineSpacing(spacing: Float) = viewModelScope.launch { prefsRepository.setLineSpacing(spacing) }
    fun setTtsSpeed(speed: Float) = viewModelScope.launch { prefsRepository.setTtsSpeed(speed) }
    fun setTtsPitch(pitch: Float) = viewModelScope.launch { prefsRepository.setTtsPitch(pitch) }
    fun setKeepScreenOn(keep: Boolean) = viewModelScope.launch { prefsRepository.setKeepScreenOn(keep) }
    fun setHighlightSentence(h: Boolean) = viewModelScope.launch { prefsRepository.setHighlightSentence(h) }
    fun setAutoScroll(auto: Boolean) = viewModelScope.launch { prefsRepository.setAutoScroll(auto) }
}
