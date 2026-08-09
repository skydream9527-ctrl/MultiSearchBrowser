package com.browser.app.repository;

import com.browser.app.data.dao.HistoryDao;
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
public final class HistoryRepository_Factory implements Factory<HistoryRepository> {
  private final Provider<HistoryDao> historyDaoProvider;

  public HistoryRepository_Factory(Provider<HistoryDao> historyDaoProvider) {
    this.historyDaoProvider = historyDaoProvider;
  }

  @Override
  public HistoryRepository get() {
    return newInstance(historyDaoProvider.get());
  }

  public static HistoryRepository_Factory create(Provider<HistoryDao> historyDaoProvider) {
    return new HistoryRepository_Factory(historyDaoProvider);
  }

  public static HistoryRepository newInstance(HistoryDao historyDao) {
    return new HistoryRepository(historyDao);
  }
}
