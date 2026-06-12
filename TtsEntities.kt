package com.metroreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── TTS Playback Progress ────────────────────────────────────────────────────
@Entity(
    tableName = "tts_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class TtsProgressEntity(
    @PrimaryKey
    val bookId: Long,
    val chapterIndex: Int = 0,
    val sentenceIndex: Int = 0,
    val characterOffset: Int = 0,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val speedMultiplier: Float = 1.0f,
    val pitchMultiplier: Float = 1.0f,
    val voiceId: String = "",
)

// ─── Audio Cache Entry ────────────────────────────────────────────────────────
@Entity(
    tableName = "audio_cache",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId"), Index(value = ["bookId", "chapterIndex"], unique = true)]
)
data class AudioCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val chapterTitle: String = "",
    val audioFilePath: String,
    val format: String = "mp3",
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)
