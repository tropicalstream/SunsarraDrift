plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sunsarradrift"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sunsarradrift"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // audio must not be recompressed (same gotcha as Pale Blue)
    androidResources { noCompress += listOf("ogg", "wav", "mp3") }

    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // optional RayNeo SDK aars (app runs flat without them)
    implementation(files("libs").asFileTree.matching { include("*.aar") })
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
