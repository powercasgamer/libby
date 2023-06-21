package net.byteflux.libby;

import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.Log4jLogAdapter;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

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
