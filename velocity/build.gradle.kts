plugins {
    id("libby.velocity-conventions")
}

dependencies {
    api(projects.libbyCore)
    api(projects.libbySlf4j)
    compileOnly(libs.velocity)
}

applyJarMetadata("net.byteflux.libby.velocity")
