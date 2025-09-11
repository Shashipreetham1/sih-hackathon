plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Apply the plugin
}

android {
    namespace = "com.hackathon.attendlytics"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hackathon.attendlytics"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true // Enable view binding
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ZXing for QR Scanning (if you still need it directly, otherwise remove if only using a wrapper)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // This is a wrapper, usually enough

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.2.0")) // Using the version from your file
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")

    // CameraX
    val cameraxVersion = "1.5.0" // Use consistent versioning
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}