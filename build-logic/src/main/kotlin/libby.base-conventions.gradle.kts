plugins {
    id("net.kyori.indra.publishing")
    id("idea")
}

val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

group = rootProject.group
version = rootProject.version
description = rootProject.description

indra {
    mitLicense()
    github(providers.gradleProperty("githubOrg").get(), providers.gradleProperty("githubRepo").get()) {
        ci(true)
        issues(true)
        scm(true)
    }

    configurePublications {
        pom {
            developers {
                developer {
                    id = "powercas_gamer"
                    name = "Cas"
                    url = "https://deltapvp.net"
                    email = "cas@deltapvp.net"
                    timezone = "Europe/Amsterdam"
                }
            }
        }
    }
}

tasks {
    named("idea") {
        notCompatibleWithConfigurationCache("something")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}