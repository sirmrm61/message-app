plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sirmrm.messageapp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sirmrm.messageapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        // Ship only the two ABIs the spec asks for — keeps x86/x86_64 libs out.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildFeatures {
        compose = true
    }

    lint {
        // False positive: the merged initializer is already removed via
        // tools:node="remove" in AndroidManifest; lint still flags it.
        disable += "RemoveWorkManagerInitializer"
    }

    buildTypes {
        debug {
            // x86_64 only in debug so the emulator can run the native TTS
            // libs without ARM translation (translated onnxruntime crashes).
            ndk { abiFilters += "x86_64" }
        }
        release {
            isMinifyEnabled = false
            // Sign release builds with the debug key so the released APK
            // installs over the debug builds users already have.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":classifier"))
    implementation(project(":feature-conversations"))
    implementation(project(":feature-chat"))
    implementation(project(":feature-promotions"))
    implementation(project(":feature-settings"))
    implementation(project(":tts"))

    implementation(libs.androidx.core.ktx)
    // Offline Persian TTS (sherpa-onnx, Apache-2.0) — bundled AAR, no INTERNET.
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
