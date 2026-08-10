package com.browser.app.ui.webview;

import com.browser.app.repository.DownloadRepository;
import com.browser.app.utils.PreferenceManager;
import com.browser.app.webview.WebViewPool;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WebviewFragment_MembersInjector implements MembersInjector<WebviewFragment> {
  private final Provider<WebViewPool> webViewPoolProvider;

  private final Provider<PreferenceManager> preferenceManagerProvider;

  private final Provider<DownloadRepository> downloadRepositoryProvider;

  public WebviewFragment_MembersInjector(Provider<WebViewPool> webViewPoolProvider,
      Provider<PreferenceManager> preferenceManagerProvider,
      Provider<DownloadRepository> downloadRepositoryProvider) {
    this.webViewPoolProvider = webViewPoolProvider;
    this.preferenceManagerProvider = preferenceManagerProvider;
    this.downloadRepositoryProvider = downloadRepositoryProvider;
  }

  public static MembersInjector<WebviewFragment> create(Provider<WebViewPool> webViewPoolProvider,
      Provider<PreferenceManager> preferenceManagerProvider,
      Provider<DownloadRepository> downloadRepositoryProvider) {
    return new WebviewFragment_MembersInjector(webViewPoolProvider, preferenceManagerProvider, downloadRepositoryProvider);
  }

  @Override
  public void injectMembers(WebviewFragment instance) {
    injectWebViewPool(instance, webViewPoolProvider.get());
    injectPreferenceManager(instance, preferenceManagerProvider.get());
    injectDownloadRepository(instance, downloadRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.browser.app.ui.webview.WebviewFragment.webViewPool")
  public static void injectWebViewPool(WebviewFragment instance, WebViewPool webViewPool) {
    instance.webViewPool = webViewPool;
  }

  @InjectedFieldSignature("com.browser.app.ui.webview.WebviewFragment.preferenceManager")
  public static void injectPreferenceManager(WebviewFragment instance,
      PreferenceManager preferenceManager) {
    instance.preferenceManager = preferenceManager;
  }

  @InjectedFieldSignature("com.browser.app.ui.webview.WebviewFragment.downloadRepository")
  public static void injectDownloadRepository(WebviewFragment instance,
      DownloadRepository downloadRepository) {
    instance.downloadRepository = downloadRepository;
  }
}
