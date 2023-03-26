package net.byteflux.libby;

import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.byteflux.libby.logging.adapters.JDKLogAdapter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for Paper Plugins. (Not to be confused with bukkit plugins loaded on paper)
 * See: <a href="https://docs.papermc.io/paper/dev/getting-started/paper-plugins">Paper docs</a>
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
     * @param plugin the plugin to manage
     * @param directoryName download directory name
     */
    public PaperLibraryManager(final Plugin plugin, final String directoryName) {
        super(new JDKLogAdapter(requireNonNull(plugin, "plugin").getLogger()), plugin.getDataFolder().toPath(), directoryName);

        final ClassLoader cl = plugin.getClass().getClassLoader();
        final Class<?> paperClClazz;

        try {
             paperClClazz = Class.forName("io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader");
        } catch (final ClassNotFoundException e) {
            System.err.println("PaperPluginClassLoader not found, are you using Paper 1.19.3+?");
            throw new RuntimeException(e);
        }

        if (!paperClClazz.isAssignableFrom(cl.getClass())) {
            throw new RuntimeException("Plugin classloader is not a PaperPluginClassLoader, are you using paper-plugin.yml?");
        }

        final Field libraryLoaderField;

        try {
            libraryLoaderField = paperClClazz.getDeclaredField("libraryLoader");
        } catch (final NoSuchFieldException e) {
            System.err.println("Cannot find libraryLoader field in PaperPluginClassLoader, please open a bug report.");
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
    protected void addToClasspath(final Path file) {
        this.classLoader.addToClasspath(file);
    }
}