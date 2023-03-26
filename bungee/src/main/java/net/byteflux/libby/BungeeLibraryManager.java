package net.byteflux.libby;

import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

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
