/*
 * This file is part of Libby, licensed under the MIT License.
 *
 * Copyright (c) 2019-2023 Matthew Harris
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.byteflux.libby.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.byteflux.libby.Library;
import net.byteflux.libby.PaperLibraryManager;
import net.byteflux.libby.relocation.Relocation;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.LogManager;

public class LibbyTestPlugin extends org.bukkit.plugin.java.JavaPlugin {

    @Override
    public void onEnable() {
        LogManager.getLogManager().getLogger("LibbyTestPlugin").setLevel(java.util.logging.Level.ALL);
        final PaperLibraryManager libraryManager = new PaperLibraryManager(this, "libs");
        final Relocation configurate = new Relocation("org{}spongepowered{}configurate", "libby.libs." + "configurate");
        libraryManager.addMavenCentral();
        libraryManager.addRepository("https://maven.deltapvp.net/");
        libraryManager.addRepository("https://repo.spongepowered.org/repository/maven-public/");
        libraryManager.addRepository("https://repo.spongepowered.org/repository/maven-snapshots/");
        final Library configurateCore = Library.builder()
                .groupId("org{}spongepowered")
                .artifactId("configurate-core")
                .version("4.1.2-SNAPSHOT")
                .id("configurateCore")
                .relocate(configurate)
                .build();
        final Library math = Library.builder()
                .groupId("org{}spongepowered")
                .artifactId("math")
                .version("2.1.0-SNAPSHOT")
                .id("math")
//                .relocate(configurate)
                .build();
        final Library luckPerms = Library.builder()
                .groupId("net{}luckperms")
                .artifactId("api")
                .version("5.4")
                .build();
        final Library axel = Library.builder()
                .groupId("org{}minearcade{}axel")
                .artifactId("axel-velocity")
                .classifier("sources")
                .version("0.0.40-SNAPSHOT")
                .build();
        libraryManager.loadLibraries(luckPerms, math, axel, configurateCore);

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onAsyncChat(final AsyncChatEvent event) {
                event.renderer((source, sourceDisplayName, message, viewer) -> {
                    return Component.text()
                            .append(sourceDisplayName.append(Component.text(": ")))
                            .append(message)
                            .appendNewline()
                            .append(Component.text("Viewer: ").append(viewer.getOrDefault(Identity.DISPLAY_NAME, Component.text("Unknown"))))
                            .appendNewline()
                            .append(Component.text("Source: ").append(source.name()))
                            .build();
                });
            }
        }, this);

    }
}
