# Add project specific ProGuard rules here.

# 保留注解与调试堆栈信息
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod

# 四大组件与 View 不被混淆（保留类名供系统反射）
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Service
-keep public class * extends android.view.View

# ---------- Room ----------
# 实体类被 Room 生成的代码以反射方式访问字段，必须保留
-keep class com.browser.app.data.entity.** { *; }
# DAO 接口由 Room 生成实现，保留接口与签名
-keep interface com.browser.app.data.dao.** { *; }
# Database 子类被 Room 内部反射实例化
-keep class com.browser.app.data.BrowserDatabase { *; }

# ---------- Hilt ----------
# @HiltAndroidApp / @AndroidEntryPoint 入口点生成的 _Hilt 类需保留类名
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *
# @Inject 构造函数：Hilt 生成的工厂会反射调用
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ---------- Kotlin ----------
# 保留 Kotlin Metadata，避免反射/序列化相关特性失效
-keep class kotlin.Metadata { *; }
