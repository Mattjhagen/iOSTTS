package com.metroreader.app.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class TtsState(
    val isInitialized: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentChapterIndex: Int = 0,
    val currentSentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val selectedVoice: Voice? = null,
    val availableVoices: List<Voice> = emptyList(),
    val error: String? = null,
)

/**
 * Core TTS engine wrapping Android TextToSpeech API.
 * Provides sentence-level playback with progress callbacks.
 */
@Singleton
class MetroTtsEngine @Inject constructor() {

    private var tts: TextToSpeech? = null
    private var sentences: List<String> = emptyList()
    private var chapterIndex: Int = 0
    private var sentenceIndex: Int = 0

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    var onSentenceStart: ((chapterIndex: Int, sentenceIndex: Int) -> Unit)? = null
    var onSentenceDone: ((chapterIndex: Int, sentenceIndex: Int) -> Unit)? = null
    var onChapterDone: ((chapterIndex: Int) -> Unit)? = null
    var onPlaybackDone: (() -> Unit)? = null

    fun initialize(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ttsInstance = tts ?: return@TextToSpeech
                ttsInstance.language = Locale.getDefault()

                val voices = ttsInstance.voices?.toList() ?: emptyList()
                _state.value = _state.value.copy(
                    isInitialized = true,
                    availableVoices = voices,
                )

                ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        val parts = utteranceId.split("_")
                        if (parts.size == 3 && parts[0] == "sentence") {
                            val ch = parts[1].toIntOrNull() ?: return
                            val si = parts[2].toIntOrNull() ?: return
                            sentenceIndex = si
                            _state.value = _state.value.copy(
                                currentChapterIndex = ch,
                                currentSentenceIndex = si,
                                isPlaying = true,
                            )
                            onSentenceStart?.invoke(ch, si)
                        }
                    }

                    override fun onDone(utteranceId: String) {
                        val parts = utteranceId.split("_")
                        if (parts.size == 3 && parts[0] == "sentence") {
                            val ch = parts[1].toIntOrNull() ?: return
                            val si = parts[2].toIntOrNull() ?: return
                            onSentenceDone?.invoke(ch, si)

                            // Queue next sentence
                            val nextIndex = si + 1
                            if (nextIndex < sentences.size) {
                                speakSentence(ch, nextIndex)
                            } else {
                                // Chapter done
                                _state.value = _state.value.copy(isPlaying = false)
                                onChapterDone?.invoke(ch)
                                onPlaybackDone?.invoke()
                            }
                        }
                    }

                    override fun onError(utteranceId: String) {
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            error = "TTS error for utterance: $utteranceId"
                        )
                    }
                })
            } else {
                _state.value = _state.value.copy(error = "TTS initialization failed")
            }
        }
    }

    fun loadChapter(chapterIdx: Int, sentenceList: List<String>, startSentence: Int = 0) {
        stop()
        chapterIndex = chapterIdx
        sentences = sentenceList
        sentenceIndex = startSentence
        _state.value = _state.value.copy(
            currentChapterIndex = chapterIdx,
            currentSentenceIndex = startSentence,
            totalSentences = sentenceList.size,
        )
    }

    fun play() {
        if (!_state.value.isInitialized) return
        if (_state.value.isPaused) {
            // Resume from current position
            speakSentence(chapterIndex, sentenceIndex)
            _state.value = _state.value.copy(isPlaying = true, isPaused = false)
        } else {
            speakSentence(chapterIndex, sentenceIndex)
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false, isPaused = true)
    }

    fun resume() = play()

    fun stop() {
        tts?.stop()
        sentenceIndex = 0
        _state.value = _state.value.copy(
            isPlaying = false,
            isPaused = false,
            currentSentenceIndex = 0,
        )
    }

    fun seekToSentence(index: Int) {
        tts?.stop()
        sentenceIndex = index.coerceIn(0, sentences.size - 1)
        _state.value = _state.value.copy(currentSentenceIndex = sentenceIndex)
        if (_state.value.isPlaying || _state.value.isPaused) {
            speakSentence(chapterIndex, sentenceIndex)
            _state.value = _state.value.copy(isPlaying = true, isPaused = false)
        }
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
        _state.value = _state.value.copy(pitch = pitch)
    }

    fun setVoice(voice: Voice) {
        tts?.voice = voice
        _state.value = _state.value.copy(selectedVoice = voice)
    }

    fun getAvailableVoices(): List<Voice> = tts?.voices?.toList() ?: emptyList()

    private fun speakSentence(chapter: Int, index: Int) {
        val text = sentences.getOrNull(index) ?: return
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_${chapter}_$index")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "sentence_${chapter}_$index")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.value = TtsState()
    }
}
