import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("libby.common-conventions")
    id("io.papermc.hangar-publish-plugin")
}

hangarPublish {
    publications.register("testing") {
        apiEndpoint.set("https://hangar.papermc.dev/api/v1/")
        version.set(project.versionString())
//        namespace("powercas_gamer", providers.gradleProperty("projectName").getOrElse("Template"))
        namespace("powercas_gamer", "aaaaa")
        channel.set(rootProject.channel())
        // changelog.set(provider { rootProject.projectDir.resolve("CHANGELOG.md").readText() })
        platforms {
            if (project.name.endsWith("paper", ignoreCase = true)) {
                register(Platforms.PAPER) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.set(listOf("1.20", "1.20.1"))
                }
            } else if (project.name.endsWith("velocity", ignoreCase = true)) {
                register(Platforms.VELOCITY) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.addAll("3.2")
                }
            }
        pages {
            resourcePage(provider { rootProject.projectDir.resolve("README.md").readText() })
        }
    }
}
}
