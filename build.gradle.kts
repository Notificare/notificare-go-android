plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("androidx.navigation.safeargs") version "2.5.1" apply false
    id("com.google.dagger.hilt.android") version "2.47" apply false
    id("com.google.firebase.crashlytics") version "2.8.1" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false


}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
