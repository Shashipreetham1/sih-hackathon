plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services) // Added for Firebase
    alias(libs.plugins.kotlin.android) // Added Kotlin Android plugin
}

android {
    namespace = "com.hackathon.attendlytics"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hackathon.attendlytics"
        minSdk = 30
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }
    // If you are using viewBinding or dataBinding, enable it here
    // viewBinding {
    //     enable = true
    // }
}

dependencies {
    implementation(platform(libs.kotlin.bom)) // ADDED to enforce Kotlin versions
    implementation(libs.kotlin.stdlib)        // ADDED to ensure stdlib is present

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // ZXing for QR Scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
