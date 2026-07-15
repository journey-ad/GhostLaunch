plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun getGitHash(): String = runCatching {
    ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
        .inputStream.bufferedReader().readText().trim()
}.getOrDefault("unknown")

fun getBaseVersion(): String = runCatching {
    val p = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val text = p.inputStream.bufferedReader().readText().trim()
    if (p.waitFor() != 0) throw RuntimeException("no tags")
    text.removePrefix("v")
}.getOrDefault("unknown")

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
        versionName = (project.findProperty("versionName") as? String ?: getBaseVersion())
            .removePrefix("v") + (project.findProperty("versionSuffix") as? String ?: "") + " (${getGitHash()})"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GITHUB_URL", "\"https://github.com/journey-ad/GhostLaunch\"")
    }

    val keystoreFile = rootProject.file("ghostlaunch.keystore")
    val storePwd = System.getenv("KEYSTORE_PASSWORD") ?: providers.gradleProperty("KEYSTORE_PASSWORD").orNull
    val keyPwd = System.getenv("KEY_PASSWORD") ?: providers.gradleProperty("KEY_PASSWORD").orNull
    val signingKeyAlias = System.getenv("KEY_ALIAS") ?: providers.gradleProperty("KEY_ALIAS").orNull ?: "ghostlaunch"

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
        debug {
            applicationIdSuffix = ".debug"
        }
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
    implementation(libs.androidx.lifecycle.runtime.compose)
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
