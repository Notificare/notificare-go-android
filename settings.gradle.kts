pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(url = "https://maven.notifica.re/releases")
        maven(url = "https://maven.notifica.re/prereleases")
        maven(url = "https://developer.huawei.com/repo")

        mavenLocal()
    }
}

rootProject.name = "notificare-go-android"

include(":app")
