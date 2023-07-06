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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A runtime dependency manager for Paper Plugins. (Not to be confused with bukkit plugins loaded on paper)
 * See: <a href="https://docs.papermc.io/paper/dev/getting-started/paper-plugins">Paper docs</a>
 *
 * @since 2.0.0
 */
public class PaperLibraryManager extends LibraryManager {
    /**
     * Plugin classpath helper
     */
    private final URLClassLoaderHelper classLoader;

    /**
     * Creates a new Paper library manager.
     *
     * @param plugin the plugin to manage
     */
    public PaperLibraryManager(final Plugin plugin) {
        this(plugin, "libs");
    }

    /**
     * Creates a new Paper library manager.
     *
     * @param plugin        the plugin to manage
     * @param directoryName download directory name
     */
    public PaperLibraryManager(final Plugin plugin, final String directoryName) {
        super(new JDKLogAdapter(requireNonNull(plugin, "plugin").getLogger()), plugin.getDataFolder().toPath(), directoryName);

        final ClassLoader cl = plugin.getClass().getClassLoader();
        final Class<?> paperClClazz;

        try {
            paperClClazz = Class.forName("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
        } catch (final ClassNotFoundException e) {
            plugin.getSLF4JLogger().error("PaperPluginClassLoader not found, are you using Paper 1.19.3+?", e);
            throw new RuntimeException(e);
        }

        if (!paperClClazz.isAssignableFrom(cl.getClass())) {
            throw new RuntimeException("Plugin classloader is not a PaperPluginClassLoader, are you using paper-plugin.yml?");
        }

        final Field libraryLoaderField;

        try {
            libraryLoaderField = paperClClazz.getDeclaredField("libraryLoader");
        } catch (final NoSuchFieldException e) {
            plugin.getSLF4JLogger().error("Cannot find libraryLoader field in PaperPluginClassLoader, please open a bug report.", e);
            throw new RuntimeException(e);
        }

        libraryLoaderField.setAccessible(true);

        final URLClassLoader libraryLoader;
        try {
            libraryLoader = (URLClassLoader) libraryLoaderField.get(cl);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e); // Should never happen
        }

        this.classLoader = new URLClassLoaderHelper(libraryLoader, this);
    }

    /**
     * Adds a file to the Paper plugin's classpath.
     *
     * @param file the file to add
     */
    @Override
    protected void addToClasspath(final @NotNull Path file) {
        this.classLoader.addToClasspath(file);
    }
}
