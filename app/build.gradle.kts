plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    // v2.0.0: detekt 静态代码分析
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

android {
    namespace = "com.browser.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.browser.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // v2.0.0 安全加固：优先读取 CI 注入的专用 keystore
            // 本地未配置时 fallback 到 debug 签名（仅用于调试构建）
            signingConfig = if (System.getenv("MSB_SIGNING_KEYSTORE") != null) {
                signingConfigs.create("release") {
                    storeFile = file(System.getenv("MSB_SIGNING_KEYSTORE")!!)
                    storePassword = System.getenv("MSB_SIGNING_STORE_PASSWORD") ?: ""
                    keyAlias = System.getenv("MSB_SIGNING_KEY_ALIAS") ?: ""
                    keyPassword = System.getenv("MSB_SIGNING_KEY_PASSWORD") ?: ""
                }
            } else {
                signingConfigs.getByName("debug")
            }
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

    // v2.0.0: Room schema 导出目录（exportSchema = true 时用于 Migration 验证）
    ksp {
        arg("room.schemaLocation", "${projectDir}/schemas")
    }
}

// v2.0.0: detekt 配置
detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/detekt.yml")
    parallel = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil:2.5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-android-compiler:2.48.1")
    // v1.9.0: WorkManager + Hilt-Work 集成，支持后台定时同步
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    testImplementation("junit:junit:4.13.2")
    // v2.0.0: 单元测试增强
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}