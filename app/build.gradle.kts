plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// If google-services.json is supplied, enable the official Google Services plugin.
// Keeping this conditional makes the project buildable before Firebase is configured.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.anees.mobileguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anees.mobileguard"
        minSdk = 26          // Android 8.0 — required for showWhenLocked/turnScreenOn APIs
        targetSdk = 35
        versionCode = 7
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.cardview:cardview:1.0.0")

    // Firebase / FCM remote anti-theft transport
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
}
