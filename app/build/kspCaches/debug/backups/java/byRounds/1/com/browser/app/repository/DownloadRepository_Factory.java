package com.browser.app.repository;

import android.content.Context;
import com.browser.app.data.dao.DownloadDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DownloadRepository_Factory implements Factory<DownloadRepository> {
  private final Provider<DownloadDao> downloadDaoProvider;

  private final Provider<Context> contextProvider;

  public DownloadRepository_Factory(Provider<DownloadDao> downloadDaoProvider,
      Provider<Context> contextProvider) {
    this.downloadDaoProvider = downloadDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DownloadRepository get() {
    return newInstance(downloadDaoProvider.get(), contextProvider.get());
  }

  public static DownloadRepository_Factory create(Provider<DownloadDao> downloadDaoProvider,
      Provider<Context> contextProvider) {
    return new DownloadRepository_Factory(downloadDaoProvider, contextProvider);
  }

  public static DownloadRepository newInstance(DownloadDao downloadDao, Context context) {
    return new DownloadRepository(downloadDao, context);
  }
}
