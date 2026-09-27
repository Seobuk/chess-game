plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.seobuk.chess"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.seobuk.chess"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    // Release key: keystore/chess.jks + keystore/PASSWORD.txt (both git-ignored, see keystore/README.txt).
    // Without them the release build is signed with the debug key and must not be distributed: the in-app
    // updater can only install an APK signed with the same key as the one already installed.
    val keystore = rootProject.file("keystore/chess.jks")
    val passwordFile = rootProject.file("keystore/PASSWORD.txt")
    if (keystore.exists() && passwordFile.exists()) {
        signingConfigs.create("release") {
            storeFile = keystore
            storePassword = passwordFile.readText().trim()
            keyAlias = "chess"
            keyPassword = storePassword
        }
    } else {
        println("⚠ release keystore not found: signing with debug key, do not distribute")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    testImplementation("junit:junit:4.13.2")
}
