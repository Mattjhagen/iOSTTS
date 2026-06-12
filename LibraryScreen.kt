package com.metroreader.app.ui.screen.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroreader.app.domain.model.Book
import com.metroreader.app.ui.components.*
import com.metroreader.app.ui.theme.InterFontFamily

@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBook(it) }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    // Import success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.importSuccess) {
        uiState.importSuccess?.let {
            snackbarHostState.showSnackbar("\"${it.title}\" imported successfully")
            viewModel.clearImportSuccess()
        }
    }
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let {
            snackbarHostState.showSnackbar("Import failed: $it")
            viewModel.clearImportError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    filePicker.launch(arrayOf(
                        "application/epub+zip",
                        "application/pdf",
                        "text/plain",
                        "text/html",
                        "*/*"
                    ))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(0.dp),  // Metro: square FAB
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Import Book")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "library",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-2).sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Sort button
                Box {
                    MetroIconButton(
                        icon = Icons.Filled.Sort,
                        contentDescription = "Sort",
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = InterFontFamily
                                        )
                                    )
                                },
                                onClick = {
                                    viewModel.onSortOrderChange(order)
                                    showSortMenu = false
                                },
                                leadingIcon = if (uiState.sortOrder == order) {
                                    { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            }

            // ── Search Bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "search books...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        MetroIconButton(
                            icon = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            onClick = { viewModel.onSearchQueryChange("") }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )

            MetroDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Content ───────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MetroLoadingDots()
                    }
                }
                uiState.books.isEmpty() -> {
                    EmptyLibraryState(
                        onImportClick = {
                            filePicker.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain", "*/*"))
                        }
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.books,
                            key = { it.id }
                        ) { book ->
                            BookTile(
                                book = book,
                                onClick = { onBookClick(book.id) },
                                onLongClick = { bookToDelete = book }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = {
                Text("Remove Book", fontFamily = InterFontFamily, fontWeight = FontWeight.Light)
            },
            text = {
                Text("Remove \"${book.title}\" from your library?", fontFamily = InterFontFamily)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book)
                    bookToDelete = null
                }) {
                    Text("REMOVE", color = MaterialTheme.colorScheme.error, fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("CANCEL", fontFamily = InterFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookTile(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Cover
        Box(modifier = Modifier.aspectRatio(0.65f)) {
            com.metroreader.app.ui.components.BookCoverImage(
                coverPath = book.coverPath,
                title = book.title,
                modifier = Modifier.fillMaxSize(),
                accentColor = try {
                    Color(android.graphics.Color.parseColor(book.accentColorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }
            )

            // Progress bar at bottom
            if (book.progressPercent > 0f) {
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }

        // Title & author
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "no books",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "tap + to import your first book",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = InterFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = onImportClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text(
                "IMPORT BOOK",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
