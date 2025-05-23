plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKsp)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "ltd.evilcorp.atox"
    compileSdk = libs.versions.sdk.target.get().toInt()
    defaultConfig {
        applicationId = "ltd.evilcorp.atox"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.target.get().toInt()
        versionCode = 14
        versionName = "0.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-tox4j.pro", getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("debug.keystore")
            storePassword = "android"
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint {
        disable += setOf("GoogleAppIndexingWarning", "MissingTranslation")
    }
    packaging {
        // Work around scala-compiler and scala-library (via tox4j) trying to place files in the
        // same place.
        resources.excludes.add("rootdoc.txt")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)

    implementation(libs.google.android.material)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation(libs.androidx.preference)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.google.dagger.core)
    ksp(libs.google.dagger.compiler)

    implementation(libs.nayuki.qrcodegen)

    implementation(libs.square.picasso)

    debugImplementation(libs.square.leakcanary)

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.espresso.contrib)
    androidTestImplementation(libs.test.junit.ext)
    kspAndroidTest(libs.google.dagger.compiler)

    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }
}
