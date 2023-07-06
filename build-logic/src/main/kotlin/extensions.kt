import net.kyori.indra.git.IndraGitExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.text.SimpleDateFormat
import java.util.*

val Project.libs: LibrariesForLibs
    get() = the()

fun Project.channel(): String {
    return if (this.versionString().endsWith("-SNAPSHOT")) {
        "Beta"
    } else {
        "Release"
    }
}

fun Project.versionString(): String = this.version as String

fun Project.nameString(): String = this.name.split("-").joinToString("-") { it.uppercaseFirstChar() }

private fun Project.configurePublication(configurer: MavenPublication.() -> Unit) {
    extensions.configure<PublishingExtension> {
        publications.named<MavenPublication>("mavenJava") {
            apply(configurer)
        }
    }
}

fun Project.applyJarMetadata(moduleName: String) {
    if ("jar" in tasks.names) {
        tasks.named<Jar>("jar") {
            manifest.attributes(
                "Multi-Release" to "true",
                "Built-By" to System.getProperty("user.name"),
                "Created-By" to System.getProperty("java.vendor.version"),
                "Build-Jdk" to System.getProperty("java.version"),
                "Build-Time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm.sssZ").format(Date()),
                "Automatic-Module-Name" to moduleName,
                "Specification-Title" to moduleName,
                "Specification-Version" to project.versionString(),
                "Specification-Vendor" to "powercas_gamer"
            )
            val indraGit = project.extensions.findByType<IndraGitExtension>()
            indraGit?.applyVcsInformationToManifest(manifest)
        }
    }
}
