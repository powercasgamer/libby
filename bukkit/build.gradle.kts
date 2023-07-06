plugins {
    id("libby.paper-conventions")
}

dependencies {
    api(projects.libbyCore)
    compileOnly(libs.spigot)
}

applyJarMetadata("net.byteflux.libby.bukkit")
