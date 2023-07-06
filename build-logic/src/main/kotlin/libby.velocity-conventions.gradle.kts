plugins {
    id("xyz.jpenilla.run-velocity")
//    id("net.kyori.blossom") // Doesn't support gradle 8.2 yet (awaiting release)
    id("libby.hangar-publish")
}

tasks {
    runVelocity  {
        velocityVersion("3.2.0-SNAPSHOT")
        systemProperty("terminal.jline", false)
        systemProperty("terminal.ansi", true)
    }

   named("clean", Delete::class) {
       delete(project.projectDir.resolve("run"))
   }
}