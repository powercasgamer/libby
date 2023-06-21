package net.byteflux.libby.paper;

import net.byteflux.libby.Library;
import net.byteflux.libby.PaperLibraryManager;

import java.util.logging.LogManager;

public class LibbyTestPlugin extends org.bukkit.plugin.java.JavaPlugin {

    @Override
    public void onEnable() {
        LogManager.getLogManager().getLogger("LibbyTestPlugin").setLevel(java.util.logging.Level.ALL);
        final PaperLibraryManager libraryManager = new PaperLibraryManager(this, "libs");
        libraryManager.addMavenCentral();
        libraryManager.addRepository("https://maven.deltapvp.net/");
        libraryManager.addRepository("https://repo.spongepowered.org/repository/maven-public/");
        libraryManager.addRepository("https://repo.spongepowered.org/repository/maven-snapshots/");
        final Library configurateCore = Library.builder()
                .groupId("org{}spongepowered")
                .artifactId("configurate-core")
                .version("4.1.2-SNAPSHOT")
                .id("configurateCore")
//                .relocate(configurate)
                .build();
        final Library math = Library.builder()
                .groupId("org{}spongepowered")
                .artifactId("math")
                .version("2.1.0-SNAPSHOT")
                .id("math")
//                .relocate(configurate)
                .build();
        final Library luckPerms = Library.builder()
                .groupId("net.luckperms")
                .artifactId("api")
                .version("5.4")
                .build();
        libraryManager.loadLibraries(luckPerms, math, configurateCore);
    }
}
