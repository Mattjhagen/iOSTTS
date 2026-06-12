package com.metroreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
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
data class ReadingProgressEntity(
    @PrimaryKey
    val bookId: Long,
    val currentPage: Int = 0,
    val currentChapterIndex: Int = 0,
    val scrollOffset: Float = 0f,
    val characterOffset: Int = 0,   // character position within chapter text
    val progressPercent: Float = 0f,
    val lastReadAt: Long = System.currentTimeMillis(),
    val totalReadingMinutes: Int = 0,
)
