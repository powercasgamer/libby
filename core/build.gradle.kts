plugins {
    id("libby.common-conventions")
}

dependencies {
    compileOnly(libs.annotations)
    implementation("org.apache.maven:maven-repository-metadata:3.9.9")
}

applyJarMetadata("net.byteflux.libby.core")
