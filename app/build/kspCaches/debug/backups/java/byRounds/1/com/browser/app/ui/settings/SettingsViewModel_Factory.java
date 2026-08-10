package com.browser.app.ui.settings;

import com.browser.app.repository.HistoryRepository;
import com.browser.app.utils.PreferenceManager;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferenceManager> preferenceManagerProvider;

  private final Provider<HistoryRepository> historyRepositoryProvider;

  private final Provider<WebViewPool> webViewPoolProvider;

  public SettingsViewModel_Factory(Provider<PreferenceManager> preferenceManagerProvider,
      Provider<HistoryRepository> historyRepositoryProvider,
      Provider<WebViewPool> webViewPoolProvider) {
    this.preferenceManagerProvider = preferenceManagerProvider;
    this.historyRepositoryProvider = historyRepositoryProvider;
    this.webViewPoolProvider = webViewPoolProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(preferenceManagerProvider.get(), historyRepositoryProvider.get(), webViewPoolProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<PreferenceManager> preferenceManagerProvider,
      Provider<HistoryRepository> historyRepositoryProvider,
      Provider<WebViewPool> webViewPoolProvider) {
    return new SettingsViewModel_Factory(preferenceManagerProvider, historyRepositoryProvider, webViewPoolProvider);
  }

  public static SettingsViewModel newInstance(PreferenceManager preferenceManager,
      HistoryRepository historyRepository, WebViewPool webViewPool) {
    return new SettingsViewModel(preferenceManager, historyRepository, webViewPool);
  }
}
