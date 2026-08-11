package com.browser.app.ui.home;

import com.browser.app.repository.HistoryRepository;
import com.browser.app.utils.PreferenceManager;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<PreferenceManager> preferenceManagerProvider;

  private final Provider<HistoryRepository> historyRepositoryProvider;

  public HomeViewModel_Factory(Provider<PreferenceManager> preferenceManagerProvider,
      Provider<HistoryRepository> historyRepositoryProvider) {
    this.preferenceManagerProvider = preferenceManagerProvider;
    this.historyRepositoryProvider = historyRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(preferenceManagerProvider.get(), historyRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<PreferenceManager> preferenceManagerProvider,
      Provider<HistoryRepository> historyRepositoryProvider) {
    return new HomeViewModel_Factory(preferenceManagerProvider, historyRepositoryProvider);
  }

  public static HomeViewModel newInstance(PreferenceManager preferenceManager,
      HistoryRepository historyRepository) {
    return new HomeViewModel(preferenceManager, historyRepository);
  }
}
