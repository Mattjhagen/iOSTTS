package com.metroreader.app.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroreader.app.domain.model.Book
import com.metroreader.app.ui.components.*
import com.metroreader.app.ui.theme.InterFontFamily
import com.metroreader.app.ui.theme.MetroAccentBlue
import com.metroreader.app.ui.theme.MetroAccentCyan
import com.metroreader.app.ui.theme.MetroAccentGreen

@Composable
fun HomeScreen(
    onBookClick: (Long) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── App Title ─────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "metro",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-3).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "reader",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        letterSpacing = (-3).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // ── Stats Tiles ───────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "BOOKS",
                    value = uiState.totalBooks.toString(),
                    color = MetroAccentBlue,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "READING",
                    value = uiState.recentBooks.count { it.progressPercent > 0f && !it.isFinished }.toString(),
                    color = MetroAccentCyan,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "FINISHED",
                    value = uiState.allBooks.count { it.isFinished }.toString(),
                    color = MetroAccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Recently Read ─────────────────────────────────────────────────────
        if (uiState.recentBooks.isNotEmpty()) {
            item {
                MetroSectionHeader(title = "recently read")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentBooks, key = { it.id }) { book ->
                        RecentBookCard(
                            book = book,
                            onClick = { onBookClick(book.id) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Library ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetroSectionHeader(
                    title = "library",
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        "SEE ALL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (uiState.allBooks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "no books yet",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Thin,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("IMPORT BOOK", fontFamily = InterFontFamily)
                    }
                }
            }
        } else {
            items(uiState.allBooks.take(6), key = { it.id }) { book ->
                LibraryListItem(
                    book = book,
                    onClick = { onBookClick(book.id) }
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(0.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        color = Color.White
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
private fun RecentBookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.aspectRatio(0.65f)) {
            com.metroreader.app.ui.components.BookCoverImage(
                coverPath = book.coverPath,
                title = book.title,
                modifier = Modifier.fillMaxSize(),
                accentColor = try {
                    Color(android.graphics.Color.parseColor(book.accentColorHex))
                } catch (e: Exception) { MetroAccentBlue }
            )
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
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LibraryListItem(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.metroreader.app.ui.components.BookCoverImage(
            coverPath = book.coverPath,
            title = book.title,
            modifier = Modifier
                .width(48.dp)
                .height(68.dp),
            accentColor = try {
                Color(android.graphics.Color.parseColor(book.accentColorHex))
            } catch (e: Exception) { MetroAccentBlue }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
            if (book.progressPercent > 0f) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Text(
            text = book.format.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
