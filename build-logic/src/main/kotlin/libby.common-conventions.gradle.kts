import com.diffplug.gradle.spotless.FormatExtension
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import net.kyori.indra.licenser.spotless.HeaderFormat
import java.util.*

plugins {
    id("libby.base-conventions")
//    id("net.kyori.indra.crossdoc")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
    id("net.kyori.indra.licenser.spotless")
    id("com.github.johnrengelman.shadow")
    id("java-library")
}

val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

indra {
    javaVersions {
        minimumToolchain(17)
        target(Constants.JAVA_VERSION)
    }

    publishSnapshotsTo("drink", "https://new-repo.deltapvp.net/snapshots")
    publishReleasesTo("drink", "https://new-repo.deltapvp.net/releases")
}

java {
    withSourcesJar()
    withJavadocJar()
}

spotless {
    fun FormatExtension.applyCommon() {
        trimTrailingWhitespace()
        endWithNewline()
        encoding("UTF-8")
    }
    java {
        importOrderFile(rootProject.file(".spotless/delta.importorder"))
        removeUnusedImports()
        formatAnnotations()
        applyCommon()
    }
    kotlinGradle {
        applyCommon()
    }
}

indraSpotlessLicenser {
    headerFormat(HeaderFormat.starSlash())
    licenseHeaderFile(rootProject.projectDir.resolve("HEADER"))

    val currentYear = Calendar.getInstance().apply {
        time = Date()
    }.get(Calendar.YEAR)
    val createdYear = providers.gradleProperty("createdYear").map { it.toInt() }.getOrElse(currentYear)
    val year = if (createdYear == currentYear) createdYear.toString() else "$createdYear-$currentYear"

    property("name", providers.gradleProperty("projectName").getOrElse("Template"))
    property("year", year)
    property("description", project.description ?: "A template project")
    property("author", providers.gradleProperty("projectAuthor").getOrElse("Template"))

}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        relocationPrefix = "net.deltapvp.libby.libs"
        isEnableRelocation = false
        setOf(
            "org.apache.maven",
            "org.codehaus.plexus"
        ).forEach {
            this.relocate(it, this.relocationPrefix + "." + it)
        }
        mergeServiceFiles()

        from(rootProject.projectDir.resolve("LICENSE")) {
            rename { "LICENSE_${providers.gradleProperty("projectName").getOrElse("Template")}" }
        }
        archiveBaseName.set(project.nameString())

        transform(Log4j2PluginsCacheFileTransformer::class.java)

        minimize()
    }

    jar {
        archiveClassifier.set("unshaded")
        archiveBaseName.set(project.nameString())
    }

    sequenceOf(javadocJar, sourcesJar).forEach {
        it.configure {
            archiveBaseName.set(project.nameString())
        }
    }

    withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:-processing")
    }

    withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        val praps = mapOf(
            "pluginVersion" to project.versionString(),
            "pluginAuthor" to providers.gradleProperty("projectAuthor").getOrElse("Template"),
            "pluginName" to providers.gradleProperty("projectName").getOrElse("Template"),
            "pluginDescription" to (project.description ?: "A template project")
        )

        filesMatching("paper-plugin.yml") {
            expand(praps)
        }
        filesMatching("plugin.yml") {
            expand(praps)
        }
        filesMatching("velocity-plugin.json") {
            expand(praps)
        }
    }

    if (providers.gradleProperty("disableJavadoc").map { it.toBoolean() }.getOrElse(false)) {
        sequenceOf(javadocJar, javadoc).forEach {
            it.configure {
                onlyIf { false }
            }
        }
    } else {
        javadoc {
            val options = options as? StandardJavadocDocletOptions ?: return@javadoc
            options.tags(
                "sinceMinecraft:a:Since Minecraft:",
                "apiSpec:a:API Requirements:",
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )
            options.isAuthor = true
            options.encoding = "UTF-8"
            options.charSet = "UTF-8"
            options.linkSource(true)
        }
    }
}
