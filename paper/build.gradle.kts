plugins {
    id("libby.paper-conventions")
}

dependencies {
    api(projects.libbyCore)
    compileOnly(libs.paper)
}

applyJarMetadata("net.byteflux.libby.paper")
