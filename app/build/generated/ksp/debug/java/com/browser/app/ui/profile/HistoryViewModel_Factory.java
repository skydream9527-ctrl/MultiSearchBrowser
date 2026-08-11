package com.browser.app.ui.profile;

import android.content.Context;
import com.browser.app.repository.HistoryRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<HistoryRepository> historyRepositoryProvider;

  private final Provider<Context> appContextProvider;

  public HistoryViewModel_Factory(Provider<HistoryRepository> historyRepositoryProvider,
      Provider<Context> appContextProvider) {
    this.historyRepositoryProvider = historyRepositoryProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(historyRepositoryProvider.get(), appContextProvider.get());
  }

  public static HistoryViewModel_Factory create(
      Provider<HistoryRepository> historyRepositoryProvider, Provider<Context> appContextProvider) {
    return new HistoryViewModel_Factory(historyRepositoryProvider, appContextProvider);
  }

  public static HistoryViewModel newInstance(HistoryRepository historyRepository,
      Context appContext) {
    return new HistoryViewModel(historyRepository, appContext);
  }
}
