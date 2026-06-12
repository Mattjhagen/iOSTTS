package com.metroreader.app.ui.screen.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroreader.app.data.repository.BookRepository
import com.metroreader.app.domain.model.Book
import com.metroreader.app.parser.BookImportManager
import com.metroreader.app.parser.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val importError: String? = null,
    val importSuccess: Book? = null,
    val sortOrder: SortOrder = SortOrder.RECENTLY_ADDED,
)

enum class SortOrder { RECENTLY_ADDED, RECENTLY_READ, TITLE, AUTHOR }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val importManager: BookImportManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.RECENTLY_ADDED)

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _sortOrder
            ) { query, sort -> Pair(query, sort) }
                .flatMapLatest { (query, sort) ->
                    if (query.isBlank()) {
                        bookRepository.getAllBooks().map { books ->
                            when (sort) {
                                SortOrder.RECENTLY_ADDED -> books.sortedByDescending { it.addedAt }
                                SortOrder.RECENTLY_READ  -> books.sortedByDescending { it.lastOpenedAt ?: 0L }
                                SortOrder.TITLE          -> books.sortedBy { it.title.lowercase() }
                                SortOrder.AUTHOR         -> books.sortedBy { it.author.lowercase() }
                            }
                        }
                    } else {
                        bookRepository.searchBooks(query)
                    }
                }
                .collect { books ->
                    _uiState.update { it.copy(books = books, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun importBook(uri: Uri, mimeType: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, importError = null) }
            when (val result = importManager.importFromUri(uri, mimeType)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, importSuccess = result.book)
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, importError = result.message)
                    }
                }
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            bookRepository.deleteBook(book.id)
        }
    }

    fun clearImportError() = _uiState.update { it.copy(importError = null) }
    fun clearImportSuccess() = _uiState.update { it.copy(importSuccess = null) }
}
