plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
    implementation(libs.hangar.publish)
    implementation(libs.indra.common)
    implementation(libs.indra.git)
    implementation(libs.indra.spotless)
    implementation(libs.indra.crossdoc)
    implementation(libs.shadow)
    implementation(libs.run.task)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}