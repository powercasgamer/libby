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
import net.byteflux.libby.logging.adapters.Log4jLogAdapter;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A runtime dependency manager for Sponge plugins.
 */
public class SpongeLibraryManager<T> extends LibraryManager {

    /**
     * The plugin instance required by the plugin manager to add files to the
     * plugin's classpath
     */
    private final T plugin;


    /**
     * Plugin classpath helper
     */
    private final URLClassLoaderHelper classLoader;

    /**
     * Creates a new Sponge library manager.
     *
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     * @param plugin        the plugin to manage
     * @param directoryName download directory name
     * @since 2.0.1
     */
    public SpongeLibraryManager(final Logger logger,
                                  final Path dataDirectory,
                                  final T plugin,
                                  final String directoryName) {

        super(new Log4jLogAdapter(logger), dataDirectory, directoryName);
        this.plugin = requireNonNull(plugin, "plugin");
        this.classLoader = new URLClassLoaderHelper((URLClassLoader) requireNonNull(plugin, "plugin").getClass().getClassLoader(), this);
    }

    /**
     * Creates a new Sponge library manager.
     *
     * @param logger        the plugin logger
     * @param dataDirectory plugin's data directory
     * @param plugin        the plugin to manage
     * @since 2.0.1
     */
    public SpongeLibraryManager(final Logger logger,
                                  final Path dataDirectory,
                                  final T plugin) {
        this(logger, dataDirectory, plugin, "libs");
    }

    /**
     * Adds a file to the Sponge plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(final @NotNull Path file) {
        this.classLoader.addToClasspath(file);
    }
}
