package com.browser.app.ui.home;

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

  public HomeViewModel_Factory(Provider<PreferenceManager> preferenceManagerProvider) {
    this.preferenceManagerProvider = preferenceManagerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(preferenceManagerProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<PreferenceManager> preferenceManagerProvider) {
    return new HomeViewModel_Factory(preferenceManagerProvider);
  }

  public static HomeViewModel newInstance(PreferenceManager preferenceManager) {
    return new HomeViewModel(preferenceManager);
  }
}
