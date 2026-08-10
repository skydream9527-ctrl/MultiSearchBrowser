package com.browser.app.ui.tabs;

import com.browser.app.repository.WindowRepository;
import com.browser.app.webview.WebViewPool;
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
public final class WindowsViewModel_Factory implements Factory<WindowsViewModel> {
  private final Provider<WindowRepository> windowRepositoryProvider;

  private final Provider<WebViewPool> webViewPoolProvider;

  public WindowsViewModel_Factory(Provider<WindowRepository> windowRepositoryProvider,
      Provider<WebViewPool> webViewPoolProvider) {
    this.windowRepositoryProvider = windowRepositoryProvider;
    this.webViewPoolProvider = webViewPoolProvider;
  }

  @Override
  public WindowsViewModel get() {
    return newInstance(windowRepositoryProvider.get(), webViewPoolProvider.get());
  }

  public static WindowsViewModel_Factory create(Provider<WindowRepository> windowRepositoryProvider,
      Provider<WebViewPool> webViewPoolProvider) {
    return new WindowsViewModel_Factory(windowRepositoryProvider, webViewPoolProvider);
  }

  public static WindowsViewModel newInstance(WindowRepository windowRepository,
      WebViewPool webViewPool) {
    return new WindowsViewModel(windowRepository, webViewPool);
  }
}
