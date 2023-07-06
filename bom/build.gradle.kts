plugins {
    id("java-platform")
    id("libby.base-conventions")
}

indra {
    configurePublications {
        from(components["javaPlatform"])
    }
}

dependencies {
    constraints {
        sequenceOf(
            "core",
            "bukkit",
            "paper",
            "bungee",
            "log4j",
            "slf4j",
            "sponge",
            "velocity",
        ).forEach {
            api(project(":libby-$it"))
        }
    }
}

applyJarMetadata("net.byteflux.libby.bom")