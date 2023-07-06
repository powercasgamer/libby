plugins {
    id("libby.common-conventions")
}

dependencies {
    api(projects.libbyCore)
    compileOnly(libs.log4j.api)
}

applyJarMetadata("net.byteflux.libby.log4j")
