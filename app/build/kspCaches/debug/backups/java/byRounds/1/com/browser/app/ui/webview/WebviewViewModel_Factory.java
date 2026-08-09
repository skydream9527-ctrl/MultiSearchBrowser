package com.browser.app.ui.webview;

import com.browser.app.repository.BookmarkRepository;
import com.browser.app.repository.HistoryRepository;
import com.browser.app.repository.WindowRepository;
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
public final class WebviewViewModel_Factory implements Factory<WebviewViewModel> {
  private final Provider<HistoryRepository> historyRepositoryProvider;

  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<WindowRepository> windowRepositoryProvider;

  public WebviewViewModel_Factory(Provider<HistoryRepository> historyRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<WindowRepository> windowRepositoryProvider) {
    this.historyRepositoryProvider = historyRepositoryProvider;
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.windowRepositoryProvider = windowRepositoryProvider;
  }

  @Override
  public WebviewViewModel get() {
    return newInstance(historyRepositoryProvider.get(), bookmarkRepositoryProvider.get(), windowRepositoryProvider.get());
  }

  public static WebviewViewModel_Factory create(
      Provider<HistoryRepository> historyRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<WindowRepository> windowRepositoryProvider) {
    return new WebviewViewModel_Factory(historyRepositoryProvider, bookmarkRepositoryProvider, windowRepositoryProvider);
  }

  public static WebviewViewModel newInstance(HistoryRepository historyRepository,
      BookmarkRepository bookmarkRepository, WindowRepository windowRepository) {
    return new WebviewViewModel(historyRepository, bookmarkRepository, windowRepository);
  }
}
