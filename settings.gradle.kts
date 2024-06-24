pluginManagement {
    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/public/p/krpc/maven")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories{
        google()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/krpc/maven")
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kode-runner"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":server")
include(":discord-bot")
include(":shared")
