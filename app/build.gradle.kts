plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "re.ovo.ghostlaunch"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "re.ovo.ghostlaunch"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreFile = rootProject.file("calculator.keystore")
    val storePwd = System.getenv("KEYSTORE_PASSWORD") ?: providers.gradleProperty("KEYSTORE_PASSWORD").orNull
    val keyPwd = System.getenv("KEY_PASSWORD") ?: providers.gradleProperty("KEY_PASSWORD").orNull
    val signingKeyAlias = System.getenv("KEY_ALIAS") ?: providers.gradleProperty("KEY_ALIAS").orNull ?: "calculator"

    val hasSigningConfig = keystoreFile.exists() &&
        !storePwd.isNullOrEmpty() &&
        !keyPwd.isNullOrEmpty()

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = keystoreFile
                storePassword = storePwd
                keyAlias = signingKeyAlias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
