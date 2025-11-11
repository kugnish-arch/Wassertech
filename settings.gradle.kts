pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.2"
        id("com.android.library") version "8.7.2"
        id("org.jetbrains.kotlin.android") version "1.9.25"
        id("com.google.devtools.ksp") version "1.9.25-1.0.20"
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

// Core modules
include(":core:ui")
include(":core:network")
include(":core:auth")

// Feature modules
include(":feature:auth")
include(":feature:reports")

// App modules
include(":app-crm")
include(":app-client")
