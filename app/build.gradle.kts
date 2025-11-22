plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

android {
    namespace = "com.coffeecat.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coffeecat.player"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ===== Compose BOM (統一版本) =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.androidx.ui.graphics)
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.4")
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ===== Media3 (統一使用最新穩定版本，例如 1.4.0) =====
    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-ui:1.4.0")
    implementation("androidx.media3:media3-session:1.4.0")
    implementation("androidx.media3:media3-common:1.4.0") // 確保此處版本與其他Media3庫一致

    // ===== AndroidX 核心 =====
    implementation(libs.androidx.core.ktx)
    // implementation("androidx.core:core-ktx:1.17.0") // 已經有 libs.androidx.core.ktx, 移除重複

    // ===== Activity Compose =====
    implementation("androidx.activity:activity-compose:1.9.0")

    // ===== Lifecycle =====
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // ===== DataStore =====
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(libs.androidx.datastore.core)

    // ===== 其他依賴 =====
    // *** 重新引入 androidx.media:media，因為 MediaStyle 需要它來構建 MediaSessionCompat.Token ***
    implementation("androidx.media:media:1.7.1") // 或更新到最新穩定版，例如 1.6.0 或 1.7.1
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(libs.play.services.cast)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.material3)
    // implementation(libs.androidx.media3.session) // 已經有明確的 media3-session 依賴，移除重複

    // ===== Testing =====
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}