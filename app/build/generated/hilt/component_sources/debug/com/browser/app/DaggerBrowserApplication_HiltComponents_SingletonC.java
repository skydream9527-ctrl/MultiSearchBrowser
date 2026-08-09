package com.browser.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.browser.app.data.BrowserDatabase;
import com.browser.app.data.dao.BookmarkDao;
import com.browser.app.data.dao.HistoryDao;
import com.browser.app.data.dao.WindowDao;
import com.browser.app.di.DatabaseModule_ProvideBookmarkDaoFactory;
import com.browser.app.di.DatabaseModule_ProvideBrowserDatabaseFactory;
import com.browser.app.di.DatabaseModule_ProvideHistoryDaoFactory;
import com.browser.app.di.DatabaseModule_ProvideWindowDaoFactory;
import com.browser.app.di.PreferenceModule_ProvidePreferenceManagerFactory;
import com.browser.app.repository.BookmarkRepository;
import com.browser.app.repository.HistoryRepository;
import com.browser.app.repository.WindowRepository;
import com.browser.app.ui.home.HomeFragment;
import com.browser.app.ui.home.HomeViewModel;
import com.browser.app.ui.home.HomeViewModel_HiltModules;
import com.browser.app.ui.profile.BookmarksFragment;
import com.browser.app.ui.profile.BookmarksViewModel;
import com.browser.app.ui.profile.BookmarksViewModel_HiltModules;
import com.browser.app.ui.profile.HistoryFragment;
import com.browser.app.ui.profile.HistoryViewModel;
import com.browser.app.ui.profile.HistoryViewModel_HiltModules;
import com.browser.app.ui.profile.ProfileFragment;
import com.browser.app.ui.profile.ProfileViewModel;
import com.browser.app.ui.profile.ProfileViewModel_HiltModules;
import com.browser.app.ui.tabs.WindowsFragment;
import com.browser.app.ui.tabs.WindowsViewModel;
import com.browser.app.ui.tabs.WindowsViewModel_HiltModules;
import com.browser.app.ui.webview.WebviewFragment;
import com.browser.app.ui.webview.WebviewViewModel;
import com.browser.app.ui.webview.WebviewViewModel_HiltModules;
import com.browser.app.utils.PreferenceManager;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerBrowserApplication_HiltComponents_SingletonC {
  private DaggerBrowserApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public BrowserApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements BrowserApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements BrowserApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements BrowserApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements BrowserApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements BrowserApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements BrowserApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements BrowserApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public BrowserApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends BrowserApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends BrowserApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public void injectHomeFragment(HomeFragment homeFragment) {
    }

    @Override
    public void injectBookmarksFragment(BookmarksFragment bookmarksFragment) {
    }

    @Override
    public void injectHistoryFragment(HistoryFragment historyFragment) {
    }

    @Override
    public void injectProfileFragment(ProfileFragment profileFragment) {
    }

    @Override
    public void injectWindowsFragment(WindowsFragment windowsFragment) {
    }

    @Override
    public void injectWebviewFragment(WebviewFragment webviewFragment) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends BrowserApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends BrowserApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(LazyClassKeyProvider.com_browser_app_ui_profile_BookmarksViewModel, BookmarksViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_browser_app_ui_profile_HistoryViewModel, HistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_browser_app_ui_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_browser_app_ui_profile_ProfileViewModel, ProfileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_browser_app_ui_webview_WebviewViewModel, WebviewViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_browser_app_ui_tabs_WindowsViewModel, WindowsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_browser_app_ui_tabs_WindowsViewModel = "com.browser.app.ui.tabs.WindowsViewModel";

      static String com_browser_app_ui_profile_BookmarksViewModel = "com.browser.app.ui.profile.BookmarksViewModel";

      static String com_browser_app_ui_profile_HistoryViewModel = "com.browser.app.ui.profile.HistoryViewModel";

      static String com_browser_app_ui_webview_WebviewViewModel = "com.browser.app.ui.webview.WebviewViewModel";

      static String com_browser_app_ui_home_HomeViewModel = "com.browser.app.ui.home.HomeViewModel";

      static String com_browser_app_ui_profile_ProfileViewModel = "com.browser.app.ui.profile.ProfileViewModel";

      @KeepFieldType
      WindowsViewModel com_browser_app_ui_tabs_WindowsViewModel2;

      @KeepFieldType
      BookmarksViewModel com_browser_app_ui_profile_BookmarksViewModel2;

      @KeepFieldType
      HistoryViewModel com_browser_app_ui_profile_HistoryViewModel2;

      @KeepFieldType
      WebviewViewModel com_browser_app_ui_webview_WebviewViewModel2;

      @KeepFieldType
      HomeViewModel com_browser_app_ui_home_HomeViewModel2;

      @KeepFieldType
      ProfileViewModel com_browser_app_ui_profile_ProfileViewModel2;
    }
  }

  private static final class ViewModelCImpl extends BrowserApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<BookmarksViewModel> bookmarksViewModelProvider;

    private Provider<HistoryViewModel> historyViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<WebviewViewModel> webviewViewModelProvider;

    private Provider<WindowsViewModel> windowsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.bookmarksViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.historyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.webviewViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.windowsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(LazyClassKeyProvider.com_browser_app_ui_profile_BookmarksViewModel, ((Provider) bookmarksViewModelProvider)).put(LazyClassKeyProvider.com_browser_app_ui_profile_HistoryViewModel, ((Provider) historyViewModelProvider)).put(LazyClassKeyProvider.com_browser_app_ui_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_browser_app_ui_profile_ProfileViewModel, ((Provider) profileViewModelProvider)).put(LazyClassKeyProvider.com_browser_app_ui_webview_WebviewViewModel, ((Provider) webviewViewModelProvider)).put(LazyClassKeyProvider.com_browser_app_ui_tabs_WindowsViewModel, ((Provider) windowsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_browser_app_ui_profile_HistoryViewModel = "com.browser.app.ui.profile.HistoryViewModel";

      static String com_browser_app_ui_webview_WebviewViewModel = "com.browser.app.ui.webview.WebviewViewModel";

      static String com_browser_app_ui_profile_BookmarksViewModel = "com.browser.app.ui.profile.BookmarksViewModel";

      static String com_browser_app_ui_home_HomeViewModel = "com.browser.app.ui.home.HomeViewModel";

      static String com_browser_app_ui_profile_ProfileViewModel = "com.browser.app.ui.profile.ProfileViewModel";

      static String com_browser_app_ui_tabs_WindowsViewModel = "com.browser.app.ui.tabs.WindowsViewModel";

      @KeepFieldType
      HistoryViewModel com_browser_app_ui_profile_HistoryViewModel2;

      @KeepFieldType
      WebviewViewModel com_browser_app_ui_webview_WebviewViewModel2;

      @KeepFieldType
      BookmarksViewModel com_browser_app_ui_profile_BookmarksViewModel2;

      @KeepFieldType
      HomeViewModel com_browser_app_ui_home_HomeViewModel2;

      @KeepFieldType
      ProfileViewModel com_browser_app_ui_profile_ProfileViewModel2;

      @KeepFieldType
      WindowsViewModel com_browser_app_ui_tabs_WindowsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.browser.app.ui.profile.BookmarksViewModel 
          return (T) new BookmarksViewModel(singletonCImpl.bookmarkRepositoryProvider.get());

          case 1: // com.browser.app.ui.profile.HistoryViewModel 
          return (T) new HistoryViewModel(singletonCImpl.historyRepositoryProvider.get());

          case 2: // com.browser.app.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.providePreferenceManagerProvider.get());

          case 3: // com.browser.app.ui.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.providePreferenceManagerProvider.get());

          case 4: // com.browser.app.ui.webview.WebviewViewModel 
          return (T) new WebviewViewModel(singletonCImpl.historyRepositoryProvider.get(), singletonCImpl.bookmarkRepositoryProvider.get(), singletonCImpl.windowRepositoryProvider.get());

          case 5: // com.browser.app.ui.tabs.WindowsViewModel 
          return (T) new WindowsViewModel(singletonCImpl.windowRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends BrowserApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends BrowserApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends BrowserApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<BrowserDatabase> provideBrowserDatabaseProvider;

    private Provider<BookmarkRepository> bookmarkRepositoryProvider;

    private Provider<HistoryRepository> historyRepositoryProvider;

    private Provider<PreferenceManager> providePreferenceManagerProvider;

    private Provider<WindowRepository> windowRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private BookmarkDao bookmarkDao() {
      return DatabaseModule_ProvideBookmarkDaoFactory.provideBookmarkDao(provideBrowserDatabaseProvider.get());
    }

    private HistoryDao historyDao() {
      return DatabaseModule_ProvideHistoryDaoFactory.provideHistoryDao(provideBrowserDatabaseProvider.get());
    }

    private WindowDao windowDao() {
      return DatabaseModule_ProvideWindowDaoFactory.provideWindowDao(provideBrowserDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideBrowserDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<BrowserDatabase>(singletonCImpl, 1));
      this.bookmarkRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BookmarkRepository>(singletonCImpl, 0));
      this.historyRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HistoryRepository>(singletonCImpl, 2));
      this.providePreferenceManagerProvider = DoubleCheck.provider(new SwitchingProvider<PreferenceManager>(singletonCImpl, 3));
      this.windowRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<WindowRepository>(singletonCImpl, 4));
    }

    @Override
    public void injectBrowserApplication(BrowserApplication browserApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.browser.app.repository.BookmarkRepository 
          return (T) new BookmarkRepository(singletonCImpl.bookmarkDao());

          case 1: // com.browser.app.data.BrowserDatabase 
          return (T) DatabaseModule_ProvideBrowserDatabaseFactory.provideBrowserDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.browser.app.repository.HistoryRepository 
          return (T) new HistoryRepository(singletonCImpl.historyDao());

          case 3: // com.browser.app.utils.PreferenceManager 
          return (T) PreferenceModule_ProvidePreferenceManagerFactory.providePreferenceManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.browser.app.repository.WindowRepository 
          return (T) new WindowRepository(singletonCImpl.windowDao());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
