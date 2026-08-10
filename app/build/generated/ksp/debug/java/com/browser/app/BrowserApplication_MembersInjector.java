package com.browser.app;

import com.browser.app.repository.DownloadRepository;
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
public final class BrowserApplication_MembersInjector implements MembersInjector<BrowserApplication> {
  private final Provider<DownloadRepository> downloadRepositoryProvider;

  public BrowserApplication_MembersInjector(
      Provider<DownloadRepository> downloadRepositoryProvider) {
    this.downloadRepositoryProvider = downloadRepositoryProvider;
  }

  public static MembersInjector<BrowserApplication> create(
      Provider<DownloadRepository> downloadRepositoryProvider) {
    return new BrowserApplication_MembersInjector(downloadRepositoryProvider);
  }

  @Override
  public void injectMembers(BrowserApplication instance) {
    injectDownloadRepository(instance, downloadRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.browser.app.BrowserApplication.downloadRepository")
  public static void injectDownloadRepository(BrowserApplication instance,
      DownloadRepository downloadRepository) {
    instance.downloadRepository = downloadRepository;
  }
}
