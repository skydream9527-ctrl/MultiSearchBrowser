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
        )
            // 显式注册迁移：升级时按 Migration 增量迁移用户数据
            .addMigrations(*DatabaseMigrations.ALL)
            // 仅在「降级」（开发期切回旧分支）时允许破坏性重建，避免崩溃
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideHistoryDao(db: BrowserDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideBookmarkDao(db: BrowserDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideWindowDao(db: BrowserDatabase): WindowDao = db.windowDao()
}
