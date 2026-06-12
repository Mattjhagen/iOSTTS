package com.metroreader.app.data.local.dao

import androidx.room.*
import com.metroreader.app.data.local.entity.AudioCacheEntity
import com.metroreader.app.data.local.entity.TtsProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TtsDao {

    // ── TTS Progress ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM tts_progress WHERE bookId = :bookId")
    suspend fun getTtsProgress(bookId: Long): TtsProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTtsProgress(progress: TtsProgressEntity)

    @Query("DELETE FROM tts_progress WHERE bookId = :bookId")
    suspend fun deleteTtsProgress(bookId: Long)

    // ── Audio Cache ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM audio_cache WHERE bookId = :bookId ORDER BY chapterIndex")
    fun getAudioCache(bookId: Long): Flow<List<AudioCacheEntity>>

    @Query("SELECT * FROM audio_cache WHERE bookId = :bookId AND chapterIndex = :chapter")
    suspend fun getChapterAudio(bookId: Long, chapter: Int): AudioCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioCache(entry: AudioCacheEntity): Long

    @Delete
    suspend fun deleteAudioCache(entry: AudioCacheEntity)

    @Query("DELETE FROM audio_cache WHERE bookId = :bookId")
    suspend fun clearAudioCacheForBook(bookId: Long)

    @Query("SELECT SUM(fileSizeBytes) FROM audio_cache")
    fun getTotalCacheSize(): Flow<Long?>

    @Query("SELECT * FROM audio_cache ORDER BY createdAt ASC LIMIT :count")
    suspend fun getOldestCacheEntries(count: Int): List<AudioCacheEntity>
}
