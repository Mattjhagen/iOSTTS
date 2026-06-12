package com.metroreader.app.data.local.dao

import androidx.room.*
import com.metroreader.app.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgress(bookId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observeProgress(bookId: Long): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgress(bookId: Long)

    @Query("""
        UPDATE reading_progress 
        SET currentPage = :page, currentChapterIndex = :chapter, 
            scrollOffset = :offset, characterOffset = :charOffset,
            progressPercent = :percent, lastReadAt = :timestamp
        WHERE bookId = :bookId
    """)
    suspend fun updateProgress(
        bookId: Long,
        page: Int,
        chapter: Int,
        offset: Float,
        charOffset: Int,
        percent: Float,
        timestamp: Long = System.currentTimeMillis()
    )
}
