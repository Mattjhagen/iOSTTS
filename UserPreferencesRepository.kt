package com.metroreader.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.metroreader.app.ui.theme.ReaderThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_READER_THEME       = stringPreferencesKey("reader_theme")
        val KEY_FONT_SIZE          = floatPreferencesKey("font_size")
        val KEY_LINE_SPACING       = floatPreferencesKey("line_spacing")
        val KEY_MARGIN_HORIZONTAL  = floatPreferencesKey("margin_horizontal")
        val KEY_TTS_SPEED          = floatPreferencesKey("tts_speed")
        val KEY_TTS_PITCH          = floatPreferencesKey("tts_pitch")
        val KEY_TTS_VOICE_ID       = stringPreferencesKey("tts_voice_id")
        val KEY_APP_THEME_DARK     = booleanPreferencesKey("app_theme_dark")
        val KEY_ACCENT_COLOR       = stringPreferencesKey("accent_color")
        val KEY_KEEP_SCREEN_ON     = booleanPreferencesKey("keep_screen_on")
        val KEY_AUTO_SCROLL        = booleanPreferencesKey("auto_scroll_tts")
        val KEY_HIGHLIGHT_SENTENCE = booleanPreferencesKey("highlight_sentence")
        val KEY_LAST_BOOK_ID       = longPreferencesKey("last_book_id")
    }

    data class ReaderPreferences(
        val theme: ReaderThemeMode = ReaderThemeMode.DARK,
        val fontSize: Float = 18f,
        val lineSpacing: Float = 1.6f,
        val marginHorizontal: Float = 20f,
    )

    data class TtsPreferences(
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val voiceId: String = "",
        val autoScroll: Boolean = true,
        val highlightSentence: Boolean = true,
    )

    val readerPreferences: Flow<ReaderPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            ReaderPreferences(
                theme = ReaderThemeMode.valueOf(
                    prefs[KEY_READER_THEME] ?: ReaderThemeMode.DARK.name
                ),
                fontSize = prefs[KEY_FONT_SIZE] ?: 18f,
                lineSpacing = prefs[KEY_LINE_SPACING] ?: 1.6f,
                marginHorizontal = prefs[KEY_MARGIN_HORIZONTAL] ?: 20f,
            )
        }

    val ttsPreferences: Flow<TtsPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            TtsPreferences(
                speed = prefs[KEY_TTS_SPEED] ?: 1.0f,
                pitch = prefs[KEY_TTS_PITCH] ?: 1.0f,
                voiceId = prefs[KEY_TTS_VOICE_ID] ?: "",
                autoScroll = prefs[KEY_AUTO_SCROLL] ?: true,
                highlightSentence = prefs[KEY_HIGHLIGHT_SENTENCE] ?: true,
            )
        }

    val isDarkTheme: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_APP_THEME_DARK] ?: true }

    val accentColor: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_ACCENT_COLOR] ?: "#0078D4" }

    val keepScreenOn: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_KEEP_SCREEN_ON] ?: true }

    val lastBookId: Flow<Long?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_LAST_BOOK_ID] }

    suspend fun setReaderTheme(theme: ReaderThemeMode) {
        dataStore.edit { it[KEY_READER_THEME] = theme.name }
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setLineSpacing(spacing: Float) {
        dataStore.edit { it[KEY_LINE_SPACING] = spacing }
    }

    suspend fun setMarginHorizontal(margin: Float) {
        dataStore.edit { it[KEY_MARGIN_HORIZONTAL] = margin }
    }

    suspend fun setTtsSpeed(speed: Float) {
        dataStore.edit { it[KEY_TTS_SPEED] = speed }
    }

    suspend fun setTtsPitch(pitch: Float) {
        dataStore.edit { it[KEY_TTS_PITCH] = pitch }
    }

    suspend fun setTtsVoiceId(voiceId: String) {
        dataStore.edit { it[KEY_TTS_VOICE_ID] = voiceId }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        dataStore.edit { it[KEY_APP_THEME_DARK] = dark }
    }

    suspend fun setAccentColor(hex: String) {
        dataStore.edit { it[KEY_ACCENT_COLOR] = hex }
    }

    suspend fun setKeepScreenOn(keep: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = keep }
    }

    suspend fun setAutoScroll(auto: Boolean) {
        dataStore.edit { it[KEY_AUTO_SCROLL] = auto }
    }

    suspend fun setHighlightSentence(highlight: Boolean) {
        dataStore.edit { it[KEY_HIGHLIGHT_SENTENCE] = highlight }
    }

    suspend fun setLastBookId(bookId: Long) {
        dataStore.edit { it[KEY_LAST_BOOK_ID] = bookId }
    }
}
