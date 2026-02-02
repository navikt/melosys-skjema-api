rootProject.name = "melosys-skjema-api"

include("melosys-skjema-api-types")

// Configure plugin repositories to avoid Maven Central 403 issues
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
        maven {
            url = uri("https://maven.google.com")
        }
    }
}

// Enable toolchain auto-provisioning
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}