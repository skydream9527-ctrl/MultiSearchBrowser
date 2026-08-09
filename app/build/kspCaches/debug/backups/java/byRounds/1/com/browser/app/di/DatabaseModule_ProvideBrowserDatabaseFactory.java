package com.browser.app.di;

import android.content.Context;
import com.browser.app.data.BrowserDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideBrowserDatabaseFactory implements Factory<BrowserDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideBrowserDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BrowserDatabase get() {
    return provideBrowserDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideBrowserDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideBrowserDatabaseFactory(contextProvider);
  }

  public static BrowserDatabase provideBrowserDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBrowserDatabase(context));
  }
}
