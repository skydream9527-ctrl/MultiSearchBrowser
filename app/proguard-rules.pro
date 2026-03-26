# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Service
-keep public class * extends android.view.View
-keep class com.browser.app.data.entity.** { *; }
-keep class com.browser.app.data.dao.** { *; }