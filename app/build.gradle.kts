import java.util.Properties
import java.io.FileInputStream

 plugins {
     id ("com.android.application")
     id ("org.jetbrains.kotlin.android")
     id( "org.jetbrains.kotlin.plugin.parcelize")
     id( "androidx.navigation.safeargs")
     id( "com.google.gms.google-services")
     id( "com.google.dagger.hilt.android")
     id("com.google.firebase.crashlytics")
     id("com.google.devtools.ksp")
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
        versionCode = 13
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField(
            "String",
            "LOCATION_DATA_PRIVACY_POLICY_URL",
            "\"https://ntc.re/0OMbJKeJ2m\""
        )
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
        debug {
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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // Android
    implementation("androidx.activity:activity-ktx:1.5.1")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.core:core-google-shortcuts:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.5.2")
    implementation("androidx.hilt:hilt-work:1.0.0")
    ksp("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("androidx.room:room-runtime:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")

    // Google
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("com.google.android.material:material:1.7.0-beta01")
    implementation("com.google.dagger:hilt-android:2.47")
    ksp("com.google.dagger:hilt-compiler:2.47")
    implementation("com.google.mlkit:barcode-scanning:17.0.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:30.0.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Notificare
    implementation("re.notifica:notificare:3.6.0")
    implementation("re.notifica:notificare-assets:3.6.0")
    implementation("re.notifica:notificare-geo:3.6.0")
    implementation("re.notifica:notificare-geo-gms:3.6.0")
    implementation("re.notifica:notificare-geo-hms:3.6.0")
    implementation("re.notifica:notificare-geo-beacons:3.6.0")
    implementation("re.notifica:notificare-in-app-messaging:3.6.0")
    implementation("re.notifica:notificare-inbox:3.6.0")
    implementation("re.notifica:notificare-loyalty:3.6.0")
    implementation("re.notifica:notificare-push:3.6.0")
    implementation("re.notifica:notificare-push-gms:3.6.0")
    implementation("re.notifica:notificare-push-hms:3.6.0")
    implementation("re.notifica:notificare-push-ui:3.6.0")
    implementation("re.notifica:notificare-push-ui-gms:3.6.0")
    implementation("re.notifica:notificare-push-ui-hms:3.6.0")
    implementation("re.notifica:notificare-scannables:3.6.0")
    implementation("re.notifica:notificare-scannables-gms:3.6.0")
    implementation("re.notifica:notificare-scannables-hms:3.6.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.13.2")
    ksp("com.github.bumptech.glide:compiler:4.13.2")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    // OkHttp & Retrofit
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    implementation("com.airbnb.android:lottie:5.0.3")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.tbuonomo:dotsindicator:4.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}
