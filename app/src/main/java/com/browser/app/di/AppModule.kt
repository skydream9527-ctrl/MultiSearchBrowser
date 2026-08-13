package com.browser.app.di

import android.content.Context
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.dao.NoteDao
import com.browser.app.data.dao.PasswordDao
import com.browser.app.data.dao.RssDao
import com.browser.app.data.dao.UserScriptDao
import com.browser.app.data.dao.WindowDao
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.NoteRepository
import com.browser.app.repository.PasswordRepository
import com.browser.app.repository.RssRepository
import com.browser.app.repository.UserScriptRepository
import com.browser.app.repository.WindowRepository
import com.browser.app.utils.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 全局 Hilt DI 模块：提供 Database / DAO / Repository / PreferenceManager 单例。
 * v1.9.0: 新增 RssDao/NoteDao/PasswordDao/UserScriptDao + 对应 Repository。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBrowserDatabase(@ApplicationContext context: Context): BrowserDatabase =
        BrowserDatabase.getInstance(context)

    @Provides
    fun provideHistoryDao(db: BrowserDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideBookmarkDao(db: BrowserDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideWindowDao(db: BrowserDatabase): WindowDao = db.windowDao()

    // v1.9.0: 新增 DAO
    @Provides
    fun provideRssDao(db: BrowserDatabase): RssDao = db.rssDao()

    @Provides
    fun provideNoteDao(db: BrowserDatabase): NoteDao = db.noteDao()

    @Provides
    fun providePasswordDao(db: BrowserDatabase): PasswordDao = db.passwordDao()

    @Provides
    fun provideUserScriptDao(db: BrowserDatabase): UserScriptDao = db.userScriptDao()

    @Provides
    fun provideHistoryRepository(dao: HistoryDao): HistoryRepository = HistoryRepository(dao)

    @Provides
    fun provideBookmarkRepository(dao: BookmarkDao): BookmarkRepository = BookmarkRepository(dao)

    @Provides
    fun provideWindowRepository(dao: WindowDao): WindowRepository = WindowRepository(dao)

    // v1.9.0: 新增 Repository
    @Provides
    fun provideRssRepository(dao: RssDao): RssRepository = RssRepository(dao)

    @Provides
    fun provideNoteRepository(dao: NoteDao): NoteRepository = NoteRepository(dao)

    @Provides
    fun providePasswordRepository(dao: PasswordDao): PasswordRepository = PasswordRepository(dao)

    @Provides
    fun provideUserScriptRepository(dao: UserScriptDao): UserScriptRepository = UserScriptRepository(dao)

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager =
        PreferenceManager(context)
}
