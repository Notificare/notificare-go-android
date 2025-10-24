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

        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")

        mavenLocal()
    }
}

rootProject.name = "notificare-go-android"

include(":app")
