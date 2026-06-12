package com.metroreader.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.metroreader.app.data.local.dao.*
import com.metroreader.app.data.local.entity.*

@Database(
    entities = [
        BookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        TtsProgressEntity::class,
        AudioCacheEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class MetroReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun ttsDao(): TtsDao

    companion object {
        const val DATABASE_NAME = "metro_reader.db"
    }
}
