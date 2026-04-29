plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // 🔥 WAJIB
}

android {
    namespace = "com.example.attendanceapp"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        compose = true   // 🔥 tambahin ini
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    defaultConfig {
        applicationId = "com.example.attendanceapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // ✅ bukan minifyEnabled false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"  // ✅ pakai tanda " bukan '
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8  // ✅ pakai =
        targetCompatibility = JavaVersion.VERSION_1_8  // ✅ pakai =
    }

    kotlinOptions {
        jvmTarget = "1.8"  // ✅ pakai tanda " bukan '
    }

    buildFeatures {
        viewBinding = true  // ✅ pakai =
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.1")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Biometric (Face ID + Fingerprint)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    // Wajib untuk BiometricPrompt di Compose
    implementation ("androidx.fragment:fragment-ktx:1.6.2")
    implementation ("androidx.biometric:biometric:1.2.0-alpha05")
    implementation ("androidx.compose.material:material-icons-extended")

    // ML Kit - deteksi wajah
    implementation("com.google.mlkit:face-detection:16.1.5")
// CameraX - preview kamera
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
// Accompanist - handle izin kamera di Compose
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Scan QR pakai ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
// Generate gambar QR pakai ZXing
    implementation("com.google.zxing:core:3.5.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}