package com.browser.app.repository;

import com.browser.app.data.dao.WindowDao;
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
public final class WindowRepository_Factory implements Factory<WindowRepository> {
  private final Provider<WindowDao> windowDaoProvider;

  public WindowRepository_Factory(Provider<WindowDao> windowDaoProvider) {
    this.windowDaoProvider = windowDaoProvider;
  }

  @Override
  public WindowRepository get() {
    return newInstance(windowDaoProvider.get());
  }

  public static WindowRepository_Factory create(Provider<WindowDao> windowDaoProvider) {
    return new WindowRepository_Factory(windowDaoProvider);
  }

  public static WindowRepository newInstance(WindowDao windowDao) {
    return new WindowRepository(windowDao);
  }
}
