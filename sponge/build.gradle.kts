plugins {
    id("libby.common-conventions")
}

dependencies {
    api(projects.libbyCore)
    api(projects.libbyLog4j)
    compileOnly(libs.sponge.api)
}

applyJarMetadata("net.byteflux.libby.sponge")
