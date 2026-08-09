package com.browser.app.repository;

import com.browser.app.data.dao.BookmarkDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class BookmarkRepository_Factory implements Factory<BookmarkRepository> {
  private final Provider<BookmarkDao> bookmarkDaoProvider;

  public BookmarkRepository_Factory(Provider<BookmarkDao> bookmarkDaoProvider) {
    this.bookmarkDaoProvider = bookmarkDaoProvider;
  }

  @Override
  public BookmarkRepository get() {
    return newInstance(bookmarkDaoProvider.get());
  }

  public static BookmarkRepository_Factory create(Provider<BookmarkDao> bookmarkDaoProvider) {
    return new BookmarkRepository_Factory(bookmarkDaoProvider);
  }

  public static BookmarkRepository newInstance(BookmarkDao bookmarkDao) {
    return new BookmarkRepository(bookmarkDao);
  }
}
