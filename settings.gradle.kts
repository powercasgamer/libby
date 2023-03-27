pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.stellardrift.ca/repository/snapshots/") {
            mavenContent {
                snapshotsOnly()
            }
        }
        maven("https://repo.deltapvp.net/")
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "LibbyParent"
sequenceOf(
    "core",
    "bukkit",
    "bungee",
    "velocity",
    "paper",
    "nukkit",
    "sponge",
    "slf4j",
    "log4j"
).forEach {
    include("libby-$it")
    project(":libby-$it").projectDir = file(it)
}
