plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.smriti.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.smriti.app"
        minSdk = 27            // same floor as the ring SDK
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // Where the home server lives (Tailscale name). Change in local builds.
        buildConfigField("String", "TRANSCRIBER_URL", "\"http://smriti-server:8000\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Vendor SDKs: copied in from the private svsashank/wearable-sdks repo (see app/libs/README.md).
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // Libraries the ChipletRing AAR needs at runtime (taken from the vendor demo's build file).
    // Deliberately NOT included: Tencent Bugly (sends crash data to Tencent).
    implementation("org.greenrobot:greendao:3.3.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.zhy:okhttputils:2.6.2")
    implementation("org.ligboy.retrofit2:converter-fastjson-android:2.1.0")
    implementation("com.squareup.retrofit2:adapter-rxjava:2.3.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
