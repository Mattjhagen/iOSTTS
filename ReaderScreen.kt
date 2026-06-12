package com.metroreader.app.ui.screen.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroreader.app.domain.model.Chapter
import com.metroreader.app.ui.components.*
import com.metroreader.app.ui.theme.*

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    onOpenAudio: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readerColors = readerColorsFor(uiState.readerTheme)

    // Keep screen on
    val view = LocalView.current
    LaunchedEffect(uiState.keepScreenOn) {
        view.keepScreenOn = uiState.keepScreenOn
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerColors.background)
    ) {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MetroLoadingDots(color = readerColors.onBackground)
                }
            }
            uiState.error != null -> {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "error",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Thin,
                            color = readerColors.onBackground
                        )
                    )
                    Text(
                        uiState.error ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            color = readerColors.subtext
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text("GO BACK", fontFamily = InterFontFamily)
                    }
                }
            }
            else -> {
                // ── Main Reading Content ──────────────────────────────────────
                uiState.currentChapter?.let { chapter ->
                    ReaderContent(
                        chapter = chapter,
                        readerColors = readerColors,
                        fontSize = uiState.fontSize,
                        lineSpacing = uiState.lineSpacing,
                        marginHorizontal = uiState.marginHorizontal,
                        activeSentenceIndex = if (uiState.ttsActiveChapterIndex == uiState.currentChapterIndex)
                            uiState.ttsActiveSentenceIndex else -1,
                        onTap = viewModel::toggleControls,
                        onScrollChange = viewModel::onScrollOffsetChange,
                    )
                }

                // ── Top Controls (animated) ───────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ReaderTopBar(
                        title = uiState.book?.title ?: "",
                        chapterTitle = uiState.currentChapter?.title ?: "",
                        readerColors = readerColors,
                        onBack = onBack,
                        onToc = viewModel::toggleToc,
                        onBookmark = { viewModel.addBookmark() },
                        onBookmarkPanel = viewModel::toggleBookmarkPanel,
                        onSettings = { /* open settings sheet */ },
                    )
                }

                // ── Bottom Controls (animated) ────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomBar(
                        progressPercent = uiState.progressPercent,
                        currentChapter = uiState.currentChapterIndex + 1,
                        totalChapters = uiState.content?.chapters?.size ?: 1,
                        isPlaying = uiState.isPlaying,
                        readerColors = readerColors,
                        onPrevChapter = viewModel::navigatePreviousChapter,
                        onNextChapter = viewModel::navigateNextChapter,
                        onAudio = onOpenAudio,
                    )
                }

                // ── TOC Drawer ────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showToc,
                    enter = fadeIn() + slideInHorizontally { -it },
                    exit = fadeOut() + slideOutHorizontally { -it },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    TocPanel(
                        toc = uiState.content?.tableOfContents ?: emptyList(),
                        currentChapterIndex = uiState.currentChapterIndex,
                        readerColors = readerColors,
                        onChapterClick = viewModel::navigateToChapter,
                        onClose = viewModel::toggleToc,
                    )
                }

                // ── Settings Sheet ────────────────────────────────────────────
                // (Triggered by settings button in top bar)
                ReaderSettingsPanel(
                    visible = false, // controlled by separate state
                    fontSize = uiState.fontSize,
                    lineSpacing = uiState.lineSpacing,
                    margin = uiState.marginHorizontal,
                    theme = uiState.readerTheme,
                    onFontSizeChange = viewModel::setFontSize,
                    onLineSpacingChange = viewModel::setLineSpacing,
                    onMarginChange = viewModel::setMargin,
                    onThemeChange = viewModel::setReaderTheme,
                    onDismiss = {},
                )
            }
        }
    }
}

@Composable
private fun ReaderContent(
    chapter: Chapter,
    readerColors: ReaderColors,
    fontSize: Float,
    lineSpacing: Float,
    marginHorizontal: Float,
    activeSentenceIndex: Int,
    onTap: () -> Unit,
    onScrollChange: (Float, Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeSentenceIndex) {
        if (activeSentenceIndex >= 0) {
            listState.animateScrollToItem(activeSentenceIndex)
        }
    }

    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .padding(horizontal = marginHorizontal.dp),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)
        ) {
            // Chapter title
            item {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Light,
                        color = readerColors.onBackground,
                        fontSize = (fontSize * 1.4f).sp,
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Sentences (for TTS sync)
            if (chapter.sentences.isNotEmpty()) {
                itemsIndexed(chapter.sentences) { index, sentence ->
                    val isActive = index == activeSentenceIndex
                    val bgColor by animateColorAsState(
                        targetValue = if (isActive) readerColors.highlightSentence else Color.Transparent,
                        animationSpec = tween(300),
                        label = "sentence_bg"
                    )
                    Text(
                        text = sentence + " ",
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineSpacing).sp,
                            color = readerColors.onBackground,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind { drawRect(bgColor) }
                            .padding(vertical = 2.dp)
                    )
                }
            } else {
                // Fallback: render full chapter text
                item {
                    Text(
                        text = chapter.content,
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineSpacing).sp,
                            color = readerColors.onBackground,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapterTitle: String,
    readerColors: ReaderColors,
    onBack: () -> Unit,
    onToc: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkPanel: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = readerColors.background.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = readerColors.onBackground)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = readerColors.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        color = readerColors.subtext
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToc) {
                Icon(Icons.Filled.FormatListBulleted, "Table of Contents", tint = readerColors.onBackground)
            }
            IconButton(onClick = onBookmark) {
                Icon(Icons.Filled.BookmarkAdd, "Add Bookmark", tint = readerColors.onBackground)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.TextFields, "Reader Settings", tint = readerColors.onBackground)
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    progressPercent: Float,
    currentChapter: Int,
    totalChapters: Int,
    isPlaying: Boolean,
    readerColors: ReaderColors,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onAudio: () -> Unit,
) {
    Surface(
        color = readerColors.background.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = readerColors.surface,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevChapter) {
                    Icon(Icons.Filled.SkipPrevious, "Previous Chapter", tint = readerColors.onBackground)
                }
                Text(
                    text = "$currentChapter / $totalChapters",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        color = readerColors.subtext
                    )
                )
                Text(
                    text = "${progressPercent.toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        color = readerColors.subtext
                    )
                )
                IconButton(onClick = onAudio) {
                    Icon(
                        if (isPlaying) Icons.Filled.VolumeUp else Icons.Filled.Headphones,
                        "Audio Player",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else readerColors.onBackground
                    )
                }
                IconButton(onClick = onNextChapter) {
                    Icon(Icons.Filled.SkipNext, "Next Chapter", tint = readerColors.onBackground)
                }
            }
        }
    }
}

@Composable
private fun TocPanel(
    toc: List<com.metroreader.app.domain.model.TocEntry>,
    currentChapterIndex: Int,
    readerColors: ReaderColors,
    onChapterClick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.75f),
        color = readerColors.surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "contents",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Thin,
                        color = readerColors.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close", tint = readerColors.onBackground)
                }
            }
            HorizontalDivider(color = readerColors.onBackground.copy(alpha = 0.1f))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(toc) { entry ->
                    val isActive = entry.chapterIndex == currentChapterIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onChapterClick(entry.chapterIndex) }
                            .padding(
                                start = (16 + entry.level * 12).dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else readerColors.onBackground
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsPanel(
    visible: Boolean,
    fontSize: Float,
    lineSpacing: Float,
    margin: Float,
    theme: ReaderThemeMode,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onMarginChange: (Float) -> Unit,
    onThemeChange: (ReaderThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "reading settings",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Thin
                )
            )
            Spacer(Modifier.height(16.dp))

            // Font size
            SettingSlider(
                label = "Font Size",
                value = fontSize,
                valueRange = 12f..32f,
                displayValue = "${fontSize.toInt()}sp",
                onValueChange = onFontSizeChange
            )

            // Line spacing
            SettingSlider(
                label = "Line Spacing",
                value = lineSpacing,
                valueRange = 1.0f..2.5f,
                displayValue = String.format("%.1f", lineSpacing),
                onValueChange = onLineSpacingChange
            )

            // Margin
            SettingSlider(
                label = "Margin",
                value = margin,
                valueRange = 8f..48f,
                displayValue = "${margin.toInt()}dp",
                onValueChange = onMarginChange
            )

            Spacer(Modifier.height(16.dp))

            // Theme selection
            Text(
                "Theme",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderThemeMode.values().forEach { t ->
                    MetroChip(
                        label = t.name,
                        selected = theme == t,
                        onClick = { onThemeChange(t) }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily))
            Text(displayValue, style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = InterFontFamily,
                color = MaterialTheme.colorScheme.primary
            ))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}
