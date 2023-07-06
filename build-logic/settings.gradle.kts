rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://new-repo.deltapvp.net/releases") {
            name = "deltaReleases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://new-repo.deltapvp.net/snapshots") {
            name = "deltaSnapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.stellardrift.ca/repository/snapshots/") {
            name = "stellardriftSnapshots"
            mavenContent { snapshotsOnly() }
        }
    }

    versionCatalogs {
        register("libs") {
            from(files("../gradle/libs.versions.toml")) // include from parent project
        }
    }
}