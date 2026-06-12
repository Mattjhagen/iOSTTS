package com.metroreader.app.ui.screen.audio

import android.content.Context
import android.content.Intent
import android.speech.tts.Voice
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroreader.app.data.local.entity.TtsProgressEntity
import com.metroreader.app.data.repository.BookRepository
import com.metroreader.app.data.repository.UserPreferencesRepository
import com.metroreader.app.domain.model.Book
import com.metroreader.app.domain.model.BookContent
import com.metroreader.app.parser.ContentLoader
import com.metroreader.app.service.TtsPlaybackService
import com.metroreader.app.tts.MetroTtsEngine
import com.metroreader.app.tts.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioUiState(
    val book: Book? = null,
    val content: BookContent? = null,
    val ttsState: TtsState = TtsState(),
    val currentChapterIndex: Int = 0,
    val currentSentenceIndex: Int = 0,
    val isLoading: Boolean = true,
    val showVoiceSelector: Boolean = false,
    val showSpeedPicker: Boolean = false,
)

@HiltViewModel
class AudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val contentLoader: ContentLoader,
    private val ttsEngine: MetroTtsEngine,
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        observeTtsState()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId) ?: return@launch
            val content = contentLoader.loadContent(bookId, book.filePath, book.format)
            val ttsProgress = bookRepository.getTtsProgress(bookId)
            val prefs = prefsRepository.ttsPreferences.first()

            val startChapter = ttsProgress?.chapterIndex ?: 0
            val startSentence = ttsProgress?.sentenceIndex ?: 0

            // Apply saved settings
            ttsEngine.setSpeed(ttsProgress?.speedMultiplier ?: prefs.speed)
            ttsEngine.setPitch(ttsProgress?.pitchMultiplier ?: prefs.pitch)

            // Load chapter into engine
            content?.chapters?.getOrNull(startChapter)?.let { chapter ->
                ttsEngine.loadChapter(startChapter, chapter.sentences, startSentence)
            }

            // Set up callbacks
            ttsEngine.onSentenceStart = { ch, si ->
                _uiState.update { it.copy(currentChapterIndex = ch, currentSentenceIndex = si) }
            }
            ttsEngine.onChapterDone = { ch ->
                val nextChapter = ch + 1
                val nextChapterContent = content?.chapters?.getOrNull(nextChapter)
                if (nextChapterContent != null) {
                    ttsEngine.loadChapter(nextChapter, nextChapterContent.sentences, 0)
                    ttsEngine.play()
                }
            }

            _uiState.update {
                it.copy(
                    book = book,
                    content = content,
                    currentChapterIndex = startChapter,
                    currentSentenceIndex = startSentence,
                    isLoading = false,
                )
            }
        }
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsEngine.state.collect { ttsState ->
                _uiState.update { it.copy(ttsState = ttsState) }
            }
        }
    }

    fun play() {
        startService()
        ttsEngine.play()
    }

    fun pause() {
        ttsEngine.pause()
        saveProgress()
    }

    fun stop() {
        ttsEngine.stop()
        saveProgress()
        context.stopService(Intent(context, TtsPlaybackService::class.java))
    }

    fun seekToSentence(index: Int) {
        ttsEngine.seekToSentence(index)
    }

    fun skipToChapter(chapterIndex: Int) {
        val content = _uiState.value.content ?: return
        val chapter = content.chapters.getOrNull(chapterIndex) ?: return
        ttsEngine.loadChapter(chapterIndex, chapter.sentences, 0)
        _uiState.update { it.copy(currentChapterIndex = chapterIndex, currentSentenceIndex = 0) }
        if (_uiState.value.ttsState.isPlaying) ttsEngine.play()
    }

    fun nextChapter() {
        val current = _uiState.value.currentChapterIndex
        val total = _uiState.value.content?.chapters?.size ?: 0
        if (current < total - 1) skipToChapter(current + 1)
    }

    fun previousChapter() {
        val current = _uiState.value.currentChapterIndex
        if (current > 0) skipToChapter(current - 1)
    }

    fun setSpeed(speed: Float) {
        ttsEngine.setSpeed(speed)
        viewModelScope.launch { prefsRepository.setTtsSpeed(speed) }
    }

    fun setPitch(pitch: Float) {
        ttsEngine.setPitch(pitch)
        viewModelScope.launch { prefsRepository.setTtsPitch(pitch) }
    }

    fun setVoice(voice: Voice) {
        ttsEngine.setVoice(voice)
        viewModelScope.launch { prefsRepository.setTtsVoiceId(voice.name) }
    }

    fun toggleVoiceSelector() = _uiState.update { it.copy(showVoiceSelector = !it.showVoiceSelector) }
    fun toggleSpeedPicker() = _uiState.update { it.copy(showSpeedPicker = !it.showSpeedPicker) }

    private fun saveProgress() {
        viewModelScope.launch {
            val state = _uiState.value
            bookRepository.saveTtsProgress(
                TtsProgressEntity(
                    bookId = bookId,
                    chapterIndex = state.currentChapterIndex,
                    sentenceIndex = state.currentSentenceIndex,
                    speedMultiplier = state.ttsState.speed,
                    pitchMultiplier = state.ttsState.pitch,
                    voiceId = state.ttsState.selectedVoice?.name ?: "",
                )
            )
        }
    }

    private fun startService() {
        val intent = Intent(context, TtsPlaybackService::class.java).apply {
            action = TtsPlaybackService.ACTION_PLAY
        }
        context.startForegroundService(intent)
    }

    override fun onCleared() {
        saveProgress()
        super.onCleared()
    }
}
