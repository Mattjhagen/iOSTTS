package com.metroreader.app.ui.screen.audio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metroreader.app.ui.components.*
import com.metroreader.app.ui.theme.InterFontFamily
import com.metroreader.app.ui.theme.MetroAccentBlue

@Composable
fun AudioScreen(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ttsState = uiState.ttsState

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            )
    ) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MetroLoadingDots()
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "now playing",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = viewModel::toggleVoiceSelector) {
                    Icon(
                        Icons.Filled.RecordVoiceOver,
                        "Voice",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Cover Art ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(0.dp))
            ) {
                BookCoverImage(
                    coverPath = uiState.book?.coverPath,
                    title = uiState.book?.title ?: "",
                    modifier = Modifier.fillMaxSize(),
                    accentColor = MetroAccentBlue
                )
                // Pulsing ring when playing
                if (ttsState.isPlaying) {
                    PlayingPulseRing()
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Book Info ─────────────────────────────────────────────────────
            Text(
                text = uiState.book?.title ?: "",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = uiState.book?.author ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(8.dp))

            // ── Chapter Info ──────────────────────────────────────────────────
            val chapterTitle = uiState.content?.chapters
                ?.getOrNull(uiState.currentChapterIndex)?.title ?: ""
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(24.dp))

            // ── Progress ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                val totalSentences = ttsState.totalSentences.coerceAtLeast(1)
                val progress = uiState.currentSentenceIndex.toFloat() / totalSentences

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${uiState.currentSentenceIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        "$totalSentences sentences",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Playback Controls ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous chapter
                IconButton(
                    onClick = viewModel::previousChapter,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        "Previous Chapter",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Rewind 1 sentence
                IconButton(
                    onClick = {
                        viewModel.seekToSentence(
                            (uiState.currentSentenceIndex - 1).coerceAtLeast(0)
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Replay5,
                        "Previous Sentence",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause (large Metro button)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                        .clickable {
                            if (ttsState.isPlaying) viewModel.pause() else viewModel.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ttsState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (ttsState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Forward 1 sentence
                IconButton(
                    onClick = {
                        viewModel.seekToSentence(
                            (uiState.currentSentenceIndex + 1).coerceAtMost(
                                (ttsState.totalSentences - 1).coerceAtLeast(0)
                            )
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Forward5,
                        "Next Sentence",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next chapter
                IconButton(
                    onClick = viewModel::nextChapter,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        "Next Chapter",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Speed & Pitch Controls ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${ttsState.speed}x",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        "SPEED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Row {
                        TextButton(onClick = { viewModel.setSpeed((ttsState.speed - 0.25f).coerceAtLeast(0.25f)) }) {
                            Text("-", fontFamily = InterFontFamily)
                        }
                        TextButton(onClick = { viewModel.setSpeed((ttsState.speed + 0.25f).coerceAtMost(4f)) }) {
                            Text("+", fontFamily = InterFontFamily)
                        }
                    }
                }

                // Stop button
                IconButton(
                    onClick = viewModel::stop,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(0.dp))
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        "Stop",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pitch
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${ttsState.pitch}x",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        "PITCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Row {
                        TextButton(onClick = { viewModel.setPitch((ttsState.pitch - 0.1f).coerceAtLeast(0.5f)) }) {
                            Text("-", fontFamily = InterFontFamily)
                        }
                        TextButton(onClick = { viewModel.setPitch((ttsState.pitch + 0.1f).coerceAtMost(2f)) }) {
                            Text("+", fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }

        // ── Voice Selector Sheet ──────────────────────────────────────────────
        if (uiState.showVoiceSelector) {
            VoiceSelectorSheet(
                voices = ttsState.availableVoices,
                selectedVoice = ttsState.selectedVoice,
                onVoiceSelect = viewModel::setVoice,
                onDismiss = viewModel::toggleVoiceSelector
            )
        }
    }
}

@Composable
private fun PlayingPulseRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
private fun VoiceSelectorSheet(
    voices: List<android.speech.tts.Voice>,
    selectedVoice: android.speech.tts.Voice?,
    onVoiceSelect: (android.speech.tts.Voice) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "select voice",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Thin
                )
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(voices) { _, voice ->
                    val isSelected = voice.name == selectedVoice?.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onVoiceSelect(voice); onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.RecordVoiceOver,
                            null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                voice.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            )
                            Text(
                                voice.locale.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Filled.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
