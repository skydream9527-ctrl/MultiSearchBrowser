package com.browser.app.ui.profile;

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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<PreferenceManager> preferenceManagerProvider;

  public ProfileViewModel_Factory(Provider<PreferenceManager> preferenceManagerProvider) {
    this.preferenceManagerProvider = preferenceManagerProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(preferenceManagerProvider.get());
  }

  public static ProfileViewModel_Factory create(
      Provider<PreferenceManager> preferenceManagerProvider) {
    return new ProfileViewModel_Factory(preferenceManagerProvider);
  }

  public static ProfileViewModel newInstance(PreferenceManager preferenceManager) {
    return new ProfileViewModel(preferenceManager);
  }
}
