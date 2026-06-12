package com.metroreader.app.ui.screen.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroreader.app.data.local.entity.BookmarkEntity
import com.metroreader.app.data.local.entity.HighlightEntity
import com.metroreader.app.data.repository.BookRepository
import com.metroreader.app.data.repository.UserPreferencesRepository
import com.metroreader.app.domain.model.*
import com.metroreader.app.parser.ContentLoader
import com.metroreader.app.ui.theme.ReaderThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val book: Book? = null,
    val content: BookContent? = null,
    val currentChapterIndex: Int = 0,
    val currentChapter: Chapter? = null,
    val scrollOffset: Float = 0f,
    val characterOffset: Int = 0,
    val progressPercent: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showControls: Boolean = true,
    val showToc: Boolean = false,
    val showBookmarkPanel: Boolean = false,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    // TTS sync
    val ttsActiveSentenceIndex: Int = -1,
    val ttsActiveChapterIndex: Int = -1,
    val isPlaying: Boolean = false,
    // Reader settings
    val fontSize: Float = 18f,
    val lineSpacing: Float = 1.6f,
    val marginHorizontal: Float = 20f,
    val readerTheme: ReaderThemeMode = ReaderThemeMode.DARK,
    val keepScreenOn: Boolean = true,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val contentLoader: ContentLoader,
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        observePreferences()
        observeAnnotations()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                _uiState.update { it.copy(isLoading = false, error = "Book not found") }
                return@launch
            }

            // Restore progress
            val progress = bookRepository.getProgress(bookId)
            val chapterIndex = progress?.currentChapterIndex ?: 0
            val charOffset = progress?.characterOffset ?: 0
            val scrollOffset = progress?.scrollOffset ?: 0f

            _uiState.update {
                it.copy(
                    book = book,
                    currentChapterIndex = chapterIndex,
                    characterOffset = charOffset,
                    scrollOffset = scrollOffset,
                    progressPercent = progress?.progressPercent ?: 0f,
                )
            }

            // Load content asynchronously
            val content = contentLoader.loadContent(bookId, book.filePath, book.format)
            if (content != null) {
                val chapter = content.chapters.getOrNull(chapterIndex)
                _uiState.update {
                    it.copy(
                        content = content,
                        currentChapter = chapter,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load book content") }
            }

            // Mark as opened
            bookRepository.markOpened(bookId)
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.readerPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        fontSize = prefs.fontSize,
                        lineSpacing = prefs.lineSpacing,
                        marginHorizontal = prefs.marginHorizontal,
                        readerTheme = prefs.theme,
                    )
                }
            }
        }
        viewModelScope.launch {
            prefsRepository.keepScreenOn.collect { keep ->
                _uiState.update { it.copy(keepScreenOn = keep) }
            }
        }
    }

    private fun observeAnnotations() {
        viewModelScope.launch {
            bookRepository.getBookmarks(bookId).collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
        viewModelScope.launch {
            bookRepository.getHighlights(bookId).collect { highlights ->
                _uiState.update { it.copy(highlights = highlights) }
            }
        }
    }

    fun navigateToChapter(index: Int) {
        val content = _uiState.value.content ?: return
        val chapter = content.chapters.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                currentChapterIndex = index,
                currentChapter = chapter,
                scrollOffset = 0f,
                characterOffset = 0,
                showToc = false,
            )
        }
        saveProgress(index, 0, 0f, 0f)
    }

    fun navigateNextChapter() {
        val current = _uiState.value.currentChapterIndex
        val total = _uiState.value.content?.chapters?.size ?: 0
        if (current < total - 1) navigateToChapter(current + 1)
    }

    fun navigatePreviousChapter() {
        val current = _uiState.value.currentChapterIndex
        if (current > 0) navigateToChapter(current - 1)
    }

    fun onScrollOffsetChange(offset: Float, charOffset: Int) {
        val chapter = _uiState.value.currentChapterIndex
        val totalChapters = _uiState.value.content?.chapters?.size?.coerceAtLeast(1) ?: 1
        val chapterProgress = (chapter.toFloat() / totalChapters) + (offset / totalChapters)
        val percent = (chapterProgress * 100f).coerceIn(0f, 100f)

        _uiState.update {
            it.copy(
                scrollOffset = offset,
                characterOffset = charOffset,
                progressPercent = percent
            )
        }
        saveProgress(chapter, charOffset, offset, percent)
    }

    fun toggleControls() = _uiState.update { it.copy(showControls = !it.showControls) }
    fun toggleToc() = _uiState.update { it.copy(showToc = !it.showToc) }
    fun toggleBookmarkPanel() = _uiState.update { it.copy(showBookmarkPanel = !it.showBookmarkPanel) }

    fun addBookmark(label: String = "") {
        viewModelScope.launch {
            val state = _uiState.value
            bookRepository.addBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    chapterIndex = state.currentChapterIndex,
                    characterOffset = state.characterOffset,
                    label = label.ifBlank { state.currentChapter?.title ?: "Bookmark" }
                )
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { bookRepository.deleteBookmark(id) }
    }

    fun addHighlight(startOffset: Int, endOffset: Int, text: String, colorHex: String = "#FFD700") {
        viewModelScope.launch {
            bookRepository.addHighlight(
                HighlightEntity(
                    bookId = bookId,
                    chapterIndex = _uiState.value.currentChapterIndex,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    selectedText = text,
                    colorHex = colorHex,
                )
            )
        }
    }

    // Called by TTS engine to sync highlighting
    fun updateTtsPosition(chapterIndex: Int, sentenceIndex: Int) {
        _uiState.update {
            it.copy(
                ttsActiveChapterIndex = chapterIndex,
                ttsActiveSentenceIndex = sentenceIndex,
                isPlaying = true,
            )
        }
    }

    fun onTtsStopped() {
        _uiState.update {
            it.copy(
                ttsActiveSentenceIndex = -1,
                ttsActiveChapterIndex = -1,
                isPlaying = false,
            )
        }
    }

    private fun saveProgress(chapter: Int, charOffset: Int, scrollOffset: Float, percent: Float) {
        viewModelScope.launch {
            bookRepository.saveProgress(
                ReadingPosition(
                    bookId = bookId,
                    chapterIndex = chapter,
                    characterOffset = charOffset,
                    scrollOffset = scrollOffset,
                    progressPercent = percent,
                )
            )
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { prefsRepository.setFontSize(size) }
    }

    fun setLineSpacing(spacing: Float) {
        viewModelScope.launch { prefsRepository.setLineSpacing(spacing) }
    }

    fun setMargin(margin: Float) {
        viewModelScope.launch { prefsRepository.setMarginHorizontal(margin) }
    }

    fun setReaderTheme(theme: ReaderThemeMode) {
        viewModelScope.launch { prefsRepository.setReaderTheme(theme) }
    }
}
