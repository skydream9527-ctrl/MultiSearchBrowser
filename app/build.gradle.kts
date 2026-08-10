plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.browser.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.browser.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // 让 Robolectric 在单元测试中可读取 manifest / resources
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Robolectric 运行时需要从 Maven Central 下载 android-all jar，
// 这里把环境里的 HTTP/HTTPS 代理透传给测试 JVM。
val httpProxyHost = System.getenv("HTTP_PROXY")?.let {
    Regex("https?://([^:/]+)(?::(\\d+))?").find(it)?.groupValues?.get(1)
} ?: System.getenv("http_proxy")?.let {
    Regex("https?://([^:/]+)(?::(\\d+))?").find(it)?.groupValues?.get(1)
}
val httpProxyPort = System.getenv("HTTP_PROXY")?.let {
    Regex("https?://([^:/]+)(?::(\\d+))?").find(it)?.groupValues?.get(2)
} ?: System.getenv("http_proxy")?.let {
    Regex("https?://([^:/]+)(?::(\\d+))?").find(it)?.groupValues?.get(2)
} ?: "8080"

tasks.withType<Test>().configureEach {
    // 给测试 JVM 加 -Djava.net.preferIPv4Stack=true，避开 IPv6 DNS 问题
    jvmArgs("-Djava.net.preferIPv4Stack=true")
    if (httpProxyHost != null) {
        systemProperty("http.proxyHost", httpProxyHost)
        systemProperty("http.proxyPort", httpProxyPort)
        systemProperty("https.proxyHost", httpProxyHost)
        systemProperty("https.proxyPort", httpProxyPort)
    }
    // Robolectric 默认从 Maven Central 拉 android-all jar，沙箱访问慢。
    // 指向阿里云 maven 镜像；同时把 cache 指向 ~/.m2（我们已预下载好 jar）
    systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/public")
    systemProperty("robolectric.dependency.repo.id", "aliyun")
    // 测试报告默认不输出标准输出，开 debug 时方便排查
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

// Room schema 导出：编译时把每个版本的 schema JSON 写到 app/schemas/，
// 用于版本对比、自动生成 Migration 校验和回归测试
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    testImplementation("junit:junit:4.13.2")

    // ---------- 单元测试 ----------
    // Room 内存数据库测试（MigrationTestHelper 也需要它）
    testImplementation("androidx.room:room-testing:2.6.1")
    // 协程 test 库（runTest / TestDispatcher）
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // LiveData / Flow 测试用的 InstantTaskExecutorRule
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // 在 JVM 上跑 Android 框架代码（ApplicationProvider / Context）
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.1.5")

    // Robolectric 4.13 + SDK 33 需要的 android-all-instrumented jar
    // 显式声明，让 Gradle 通过镜像预解析下载到本地 cache（避免 Robolectric 运行时从 Maven Central 拉超时）
    testImplementation("org.robolectric:android-all-instrumented:13-robolectric-9030017-i7")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
