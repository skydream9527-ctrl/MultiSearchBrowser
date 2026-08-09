package com.browser.app.di

import android.content.Context
import androidx.room.Room
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.dao.WindowDao
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
    fun provideBrowserDatabase(@ApplicationContext context: Context): BrowserDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BrowserDatabase::class.java,
            "browser_database"
        ).build()
    }

    @Provides
    fun provideHistoryDao(db: BrowserDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideBookmarkDao(db: BrowserDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideWindowDao(db: BrowserDatabase): WindowDao = db.windowDao()
}
