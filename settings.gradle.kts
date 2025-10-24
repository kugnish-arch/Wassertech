pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.2"
        id("org.jetbrains.kotlin.android") version "1.9.25"
        id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) useVersion("1.9.25")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Wassertech"
include(":app")
