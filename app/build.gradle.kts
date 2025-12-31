plugins {
    alias(libs.plugins.android.application)
    // Add the Google Services plugin for Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.expense_tracker"
    compileSdk = 34 // Reverted to stable SDK 34 (36 is often preview/beta)

    defaultConfig {
        applicationId = "com.example.expense_tracker"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    // --- STABLE ANDROID LIBRARIES ---
    // We explicitly set versions here to avoid the "Requires AGP 8.9.1" error
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // This was the problem library. We pin it to a stable version (1.9.3)
    implementation("androidx.activity:activity:1.9.3")

    // --- FIREBASE & GOOGLE LOGIN ---
    // Import the Firebase BoM (Controls all firebase versions)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // --- TESTING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("com.google.firebase:firebase-firestore")
}