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
