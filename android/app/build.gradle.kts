plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.onemnemo.grapho"
    compileSdk = 35

    defaultConfig {
        applicationId = "one.mnemo.grapho"
        // Front-buffered rendering needs Android 10.
        minSdk = 29
        targetSdk = 35
        // The release workflow overrides these with -PversionCode / -PversionName.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "0.1.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        // Public key for test builds, so every local and CI build has the same
        // signature and installs over the previous one. Store releases use a
        // private key kept out of git.
        create("test") {
            storeFile = rootProject.file("test.keystore")
            storePassword = "grapho-spike"
            keyAlias = "spike"
            keyPassword = "grapho-spike"
        }
        // Store key, only present when the release workflow provides it.
        val releaseStore = System.getenv("GRAPHO_RELEASE_STORE_FILE")
        if (releaseStore != null) {
            create("release") {
                storeFile = file(releaseStore)
                storePassword = System.getenv("GRAPHO_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("GRAPHO_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("GRAPHO_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("test")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("test")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.graphics:graphics-core:1.0.1")
    implementation("androidx.input:input-motionprediction:1.0.0-beta03")

    testImplementation("junit:junit:4.13.2")
}

// Builds grapho-core for Android with cargo-ndk and drops the .so files into
// jniLibs. Needs `cargo install cargo-ndk`, the Rust Android targets, and
// ANDROID_NDK_HOME (see android/README.md).
val cargoNdk by tasks.registering(Exec::class) {
    workingDir = rootDir.parentFile
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", "$projectDir/src/main/jniLibs",
        "build", "--release", "-p", "grapho-android",
    )
}

tasks.named("preBuild") {
    dependsOn(cargoNdk)
}
