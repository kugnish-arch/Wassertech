pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.9.1"
        id("com.android.library") version "8.9.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
        id("com.google.devtools.ksp") version "2.0.21-1.0.28"
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
include(":core:screens")

// Feature modules
include(":feature:auth")
include(":feature:reports")
include(":feature:icons")

// App modules
include(":app-crm")
include(":app-client")
