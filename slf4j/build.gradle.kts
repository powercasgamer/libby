plugins {
    id("libby.common-conventions")
}

dependencies {
    api(projects.libbyCore)
    compileOnly("org.slf4j:slf4j-api:2.0.7")
}

applyJarMetadata("net.byteflux.libby.slf4j")
