import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.navigation)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.hilt)
}

val properties = Properties()
properties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "re.notifica.go"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "re.notifica.go"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 16
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "LOCATION_DATA_PRIVACY_POLICY_URL", "\"https://ntc.re/0OMbJKeJ2m\"")
    }

    signingConfigs {
        getByName("debug") {
            if (project.file("debug.keystore").exists()) {
                storeFile = file("debug.keystore")
                storePassword = properties.getProperty("keystore.debug.store.password")
                keyAlias = properties.getProperty("keystore.debug.key.alias")
                keyPassword = properties.getProperty("keystore.debug.key.password")
            }
        }
        create("release") {
            if (project.file("release.keystore").exists()) {
                storeFile = file("release.keystore")
                storePassword = properties.getProperty("keystore.release.store.password")
                keyAlias = properties.getProperty("keystore.release.key.alias")
                keyPassword = properties.getProperty("keystore.release.key.password")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("debug")

            buildConfigField(
                "String",
                "GOOGLE_AUTH_SERVER_ID",
                "\"${properties.getProperty("google.auth.server.id.debug")}\""
            )

            manifestPlaceholders["configuration_link_host"] = "go-demo-dev.ntc.re"
            manifestPlaceholders["crashlyticsEnabled"] = false
            manifestPlaceholders["googleMapsApiKey"] = properties.getProperty("google.maps.key.debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")

            buildConfigField(
                "String",
                "GOOGLE_AUTH_SERVER_ID",
                "\"${properties.getProperty("google.auth.server.id.release")}\""
            )

            manifestPlaceholders["configuration_link_host"] = "go-demo.ntc.re"
            manifestPlaceholders["crashlyticsEnabled"] = true
            manifestPlaceholders["googleMapsApiKey"] = properties.getProperty("google.maps.key.release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.bundles.kotlinx.coroutines)

    // Android
    implementation(libs.androidx.activity)
    implementation(libs.androidx.app.compat)
    implementation(libs.androidx.browser)
    implementation(libs.bundles.androidx.camera)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.bundles.androidx.core)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.work.runtime)

    // Google
    implementation(libs.google.auth)
    implementation(libs.google.material)
    implementation(libs.google.hilt)
    ksp(libs.google.hilt.compiler)
    implementation(libs.google.barcodeScanning)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Notificare
    implementation(libs.notificare)
    implementation(libs.notificare.assets)
    implementation(libs.bundles.notificare.geo)
    implementation(libs.notificare.inAppMessaging)
    implementation(libs.notificare.inbox)
    implementation(libs.notificare.loyalty)
    implementation(libs.bundles.notificare.push)
    implementation(libs.bundles.notificare.push.ui)
    implementation(libs.bundles.notificare.scannables)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // Moshi
    implementation(libs.bundles.moshi)
    ksp(libs.moshi.codegen)

    // OkHttp & Retrofit
    implementation(libs.bundles.okhttp)
    implementation(libs.bundles.retrofit)

    implementation(libs.lottie)
    implementation(libs.timber)
    implementation(libs.dotsIndicator)

    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(libs.bundles.testing.android)
}
