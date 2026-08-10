package com.browser.app.webview;

import android.content.Context;
import com.browser.app.utils.PreferenceManager;
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
public final class WebViewPool_Factory implements Factory<WebViewPool> {
  private final Provider<Context> contextProvider;

  private final Provider<PreferenceManager> preferenceManagerProvider;

  public WebViewPool_Factory(Provider<Context> contextProvider,
      Provider<PreferenceManager> preferenceManagerProvider) {
    this.contextProvider = contextProvider;
    this.preferenceManagerProvider = preferenceManagerProvider;
  }

  @Override
  public WebViewPool get() {
    return newInstance(contextProvider.get(), preferenceManagerProvider.get());
  }

  public static WebViewPool_Factory create(Provider<Context> contextProvider,
      Provider<PreferenceManager> preferenceManagerProvider) {
    return new WebViewPool_Factory(contextProvider, preferenceManagerProvider);
  }

  public static WebViewPool newInstance(Context context, PreferenceManager preferenceManager) {
    return new WebViewPool(context, preferenceManager);
  }
}
