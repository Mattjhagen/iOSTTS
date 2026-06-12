package com.metroreader.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metroreader.app.data.repository.BookRepository
import com.metroreader.app.data.repository.UserPreferencesRepository
import com.metroreader.app.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentBooks: List<Book> = emptyList(),
    val allBooks: List<Book> = emptyList(),
    val currentlyListeningBook: Book? = null,
    val totalBooks: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bookRepository.getRecentlyReadBooks(8),
                bookRepository.getAllBooks(),
            ) { recent, all ->
                HomeUiState(
                    recentBooks = recent,
                    allBooks = all,
                    totalBooks = all.size,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
