package com.browser.app.ui.tabs;

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
public final class WindowsViewModel_Factory implements Factory<WindowsViewModel> {
  private final Provider<WindowRepository> windowRepositoryProvider;

  public WindowsViewModel_Factory(Provider<WindowRepository> windowRepositoryProvider) {
    this.windowRepositoryProvider = windowRepositoryProvider;
  }

  @Override
  public WindowsViewModel get() {
    return newInstance(windowRepositoryProvider.get());
  }

  public static WindowsViewModel_Factory create(
      Provider<WindowRepository> windowRepositoryProvider) {
    return new WindowsViewModel_Factory(windowRepositoryProvider);
  }

  public static WindowsViewModel newInstance(WindowRepository windowRepository) {
    return new WindowsViewModel(windowRepository);
  }
}
