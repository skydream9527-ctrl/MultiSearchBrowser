package com.browser.app.ui.profile;

import com.browser.app.repository.BookmarkRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class BookmarksViewModel_Factory implements Factory<BookmarksViewModel> {
  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  public BookmarksViewModel_Factory(Provider<BookmarkRepository> bookmarkRepositoryProvider) {
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
  }

  @Override
  public BookmarksViewModel get() {
    return newInstance(bookmarkRepositoryProvider.get());
  }

  public static BookmarksViewModel_Factory create(
      Provider<BookmarkRepository> bookmarkRepositoryProvider) {
    return new BookmarksViewModel_Factory(bookmarkRepositoryProvider);
  }

  public static BookmarksViewModel newInstance(BookmarkRepository bookmarkRepository) {
    return new BookmarksViewModel(bookmarkRepository);
  }
}
