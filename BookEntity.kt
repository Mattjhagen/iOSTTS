package com.metroreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookFormat {
    EPUB, PDF, MOBI, AZW3, TXT, HTML, UNKNOWN
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val filePath: String,
    val coverPath: String? = null,
    val format: BookFormat = BookFormat.UNKNOWN,
    val totalPages: Int = 0,
    val totalChapters: Int = 0,
    val fileSize: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val estimatedReadingMinutes: Int = 0,
    val description: String = "",
    val publisher: String = "",
    val language: String = "",
    val isbn: String = "",
    val publishedDate: String = "",
    val accentColorHex: String = "#0078D4",  // Metro tile accent
    val isFinished: Boolean = false,
)
