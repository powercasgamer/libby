plugins {
    id("libby.common-conventions")
}

dependencies {
    api(projects.libbyCore)
    compileOnly(libs.bungeecord)
}

applyJarMetadata("net.byteflux.libby.bungee")
