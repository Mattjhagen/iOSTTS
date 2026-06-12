package com.metroreader.app.di

import android.content.Context
import androidx.room.Room
import com.metroreader.app.data.local.dao.*
import com.metroreader.app.data.local.database.MetroReaderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MetroReaderDatabase =
        Room.databaseBuilder(
            context,
            MetroReaderDatabase::class.java,
            MetroReaderDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBookDao(db: MetroReaderDatabase): BookDao = db.bookDao()

    @Provides
    fun provideReadingProgressDao(db: MetroReaderDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides
    fun provideAnnotationDao(db: MetroReaderDatabase): AnnotationDao = db.annotationDao()

    @Provides
    fun provideTtsDao(db: MetroReaderDatabase): TtsDao = db.ttsDao()
}
