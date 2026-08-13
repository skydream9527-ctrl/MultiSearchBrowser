# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes EnclosingMethod

# AndroidX basics
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Service
-keep public class * extends android.view.View

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp (consumer rules usually included, but explicit for safety)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coil
-dontwarn coil.**

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# ============ v2.0.0: Hilt / Dagger ============
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltWorker class * { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory$ViewModelFactoriesEntryPoint { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-dontwarn dagger.hilt.**

# ============ v2.0.0: WorkManager / Hilt-Work ============
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.impl.foreground.ForegroundService { *; }
-keep class androidx.hilt.work.HiltWorkerFactory { *; }
-dontwarn androidx.work.**

# ============ v2.0.0: Coroutines ============
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ============ v2.0.0: Navigation Safe Args ============
# 自动生成的 *Args / *Directions 类位于 com.browser.app 包下
-keep class com.browser.app.*Args { *; }
-keep class com.browser.app.*Directions { *; }
-keep class com.browser.app.*FragmentDirections { *; }
-keep class com.browser.app.*FragmentArgs { *; }

# ============ v2.0.0: ViewBinding ============
-keep class com.browser.app.databinding.** { *; }

# ============ v2.0.0: CryptoUtils（Keystore 反射） ============
-keep class com.browser.app.utils.CryptoUtils { *; }
-keep class javax.crypto.** { *; }
-keep class android.security.keystore.** { *; }
