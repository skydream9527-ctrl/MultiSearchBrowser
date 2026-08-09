package com.browser.app.di;

import com.browser.app.data.BrowserDatabase;
import com.browser.app.data.dao.WindowDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideWindowDaoFactory implements Factory<WindowDao> {
  private final Provider<BrowserDatabase> dbProvider;

  public DatabaseModule_ProvideWindowDaoFactory(Provider<BrowserDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public WindowDao get() {
    return provideWindowDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideWindowDaoFactory create(
      Provider<BrowserDatabase> dbProvider) {
    return new DatabaseModule_ProvideWindowDaoFactory(dbProvider);
  }

  public static WindowDao provideWindowDao(BrowserDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWindowDao(db));
  }
}
