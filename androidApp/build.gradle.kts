import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val keystoreDebugPropertiesFile = rootProject.file("androidApp/signing/debug.key.properties")
val keystoreDebugProperties = Properties()
if (keystoreDebugPropertiesFile.exists()) {
    keystoreDebugProperties.load(FileInputStream(keystoreDebugPropertiesFile))
}

val keystoreReleasePropertiesFile = rootProject.file("androidApp/signing/release.key.properties")
val keystoreReleaseProperties = Properties()
if (keystoreReleasePropertiesFile.exists()) {
    keystoreReleaseProperties.load(FileInputStream(keystoreReleasePropertiesFile))
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
}

android {
    namespace = "com.mediabeam.fitness"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mediabeam.fitness"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val alias = keystoreReleaseProperties.getProperty("keyAlias")
            val pass = keystoreReleaseProperties.getProperty("keyPassword")
            val storePass = keystoreReleaseProperties.getProperty("storePassword")
            val storePath = keystoreReleaseProperties.getProperty("storeFile")
            
            if (alias != null && pass != null && storePass != null && storePath != null) {
                keyAlias = alias
                keyPassword = pass
                storePassword = storePass
                storeFile = rootProject.file("androidApp/signing/" + storePath.removePrefix("../signing/"))
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
}
