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
package net.byteflux.libby;

import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;
import net.md_5.bungee.api.plugin.Plugin;

import static java.util.Objects.requireNonNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A runtime dependency manager for Bungee plugins.
 */
public class BungeeLibraryManager extends LibraryManager {

    /**
     * Plugin classpath helper
     */
    private final URLClassLoaderHelper classLoader;

    /**
     * Creates a new Bungee library manager.
     *
     * @param plugin the plugin to manage
     */
    public BungeeLibraryManager(final Plugin plugin) {
        this(plugin, "libs");
    }

    /**
     * Creates a new Bungee library manager.
     *
     * @param plugin        the plugin to manage
     * @param directoryName download directory name
     */
    public BungeeLibraryManager(final Plugin plugin, final String directoryName) {
        super(new JDKLogAdapter(requireNonNull(plugin, "plugin").getLogger()), plugin.getDataFolder().toPath(), directoryName);
        this.classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
    }

    /**
     * Adds a file to the Bungee plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(final Path file) {
        this.classLoader.addToClasspath(file);
    }
}
