import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Push (FCM) is configured by androidApp/google-services.json (not committed). Without it the app builds and runs, but
// receives no pushes.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

android {
    namespace = "com.wandr.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mediabeam.fitness"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
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
                storeFile =
                    rootProject.file("androidApp/signing/" + storePath.removePrefix("../signing/"))
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            // R8: shrinks, obfuscates and optimizes the code. scripts/build-release.sh keeps the resulting mapping.txt,
            // which is needed to read crash stack traces of that build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.location)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.health.connect.client)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.exifinterface)
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.jetbrains.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.navigation.compose)
    debugImplementation(libs.ui.tooling)
    testImplementation(libs.junit)
}

