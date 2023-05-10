package net.byteflux.libby;

import net.byteflux.libby.classloader.IsolatedClassLoader;
import net.byteflux.libby.logging.LogLevel;
import net.byteflux.libby.logging.Logger;
import net.byteflux.libby.logging.adapters.LogAdapter;
import net.byteflux.libby.relocation.Relocation;
import net.byteflux.libby.relocation.RelocationHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A runtime dependency manager for plugins.
 * <p>
 * The library manager can resolve a dependency jar through the configured
 * Maven repositories, download it into a local cache, relocate it and then
 * load it into the plugin's classpath.
 * <p>
 * Transitive dependencies for a library aren't downloaded automatically and
 * must be explicitly loaded like every other library.
 * <p>
 * It's recommended that libraries are relocated to prevent any namespace
 * conflicts with different versions of the same library bundled with other
 * plugins or maybe even bundled with the server itself.
 *
 * @see Library
 */
public abstract class LibraryManager {

    /**
     * Wrapped plugin logger
     */
    protected final Logger logger;

    /**
     * Directory where downloaded library jars are saved to
     */
    protected final Path saveDirectory;

    /**
     * Maven repositories used to resolve artifacts
     */
    private final Set<String> repositories = new LinkedHashSet<>();

    /**
     * Lazily-initialized relocation helper that uses reflection to call into
     * Luck's Jar Relocator
     */
    private RelocationHelper relocator;

    /**
     * Map of isolated class loaders and theirs id
     */
    private final Map<String, IsolatedClassLoader> isolatedLibraries = new HashMap<>();

    /**
     * Creates a new library manager.
     *
     * @param logAdapter    plugin logging adapter
     * @param dataDirectory plugin's data directory
     * @param directoryName download directory name
     */
    protected LibraryManager(@NotNull final LogAdapter logAdapter, @NotNull final Path dataDirectory, @NotNull final String directoryName) {
        this.logger = new Logger(requireNonNull(logAdapter, "logAdapter"));
        this.saveDirectory = requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve(requireNonNull(directoryName, "directoryName"));
    }

    /**
     * Adds a file to the plugin's classpath.
     *
     * @param file the file to add
     */
    protected abstract void addToClasspath(Path file);

    /**
     * Adds a file to the isolated class loader
     *
     * @param library the library to add
     * @param file the file to add
     */
    protected void addToIsolatedClasspath(@NotNull final Library library, @NotNull final Path file) {
        final IsolatedClassLoader classLoader;
        final String id = library.getId();
        if (id != null) {
            classLoader = this.isolatedLibraries.computeIfAbsent(id, s -> new IsolatedClassLoader());
        } else {
            classLoader = new IsolatedClassLoader();
        }
        classLoader.addPath(file);
    }

    /**
     * Get the isolated class loader of the library
     *
     * @param libraryId the id of the library
     */
    public IsolatedClassLoader getIsolatedClassLoaderOf(@NotNull final String libraryId) {
        return this.isolatedLibraries.get(libraryId);
    }

    /**
     * Gets the logging level for this library manager.
     *
     * @return log level
     */
    public LogLevel getLogLevel() {
        return this.logger.getLevel();
    }

    /**
     * Sets the logging level for this library manager.
     * <p>
     * By setting this value, the library manager's logger will not log any
     * messages with a level less severe than the configured level. This can be
     * useful for silencing the download and relocation logging.
     * <p>
     * Setting this value to {@link LogLevel#WARN} would silence informational
     * logging but still print important things like invalid checksum warnings.
     *
     * @param level the log level to set
     */
    public void setLogLevel(@NotNull final LogLevel level) {
        this.logger.setLevel(level);
    }

    /**
     * Gets the currently added repositories used to resolve artifacts.
     * <p>
     * For each library this list is traversed to download artifacts after the
     * direct download URLs have been attempted.
     *
     * @return current repositories
     */
    public Collection<String> getRepositories() {
        final List<String> urls;
        synchronized (this.repositories) {
            urls = new LinkedList<>(this.repositories);
        }

        return Collections.unmodifiableList(urls);
    }

    /**
     * Adds a repository URL to this library manager.
     * <p>
     * Artifacts will be resolved using this repository when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param url repository URL to add
     */
    public void addRepository(@NotNull final String url) {
        final String repo = !requireNonNull(url, "url").isEmpty() && requireNonNull(url, "url").charAt(requireNonNull(url, "url").length() - 1) == '/' ? url : url + '/';
        synchronized (this.repositories) {
            this.repositories.add(repo);
        }
    }

    /**
     * Adds a {@link Collection} of repository URLs to this library manager.
     * <p>
     * Artifacts will be resolved using this repository when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param urls repository URL to add
     */
    public void addRepositories(@NotNull final Collection<String> urls) {
        for (final String url : urls) {
            addRepository(url);
        }
    }

    /**
     * Adds the current user's local Maven repository.
     */
    public void addMavenLocal() {
        addRepository(Paths.get(System.getProperty("user.home")).resolve(".m2/repository").toUri().toString());
    }

    /**
     * Adds the Maven Central repository.
     */
    public void addMavenCentral() {
        addRepository(Repositories.MAVEN_CENTRAL);
    }

    /**
     * Adds the Sonatype OSS repository.
     */
    public void addSonatype() {
        addRepository(Repositories.SONATYPE);
    }

    /**
     * Adds the Sonatype OSS repository.
     * @since 2.0.0
     */
    public void addSonatype(final int alt) {
        addRepository(String.format(Repositories.SONATYPE_ALT, alt));
    }

    /**
     * Adds the Bintray JCenter repository.
     * <p>
     * NOTE: This repository is being shut down. Use another repository.
     * DOES NOT DO ANYTHING
     *
     * @deprecated This repository is being shut down. Use another repository
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "2.1.0")
    @ApiStatus.Obsolete
    public void addJCenter() {
        throw new UnsupportedOperationException("JCenter is shut down. Use another repository.");
    }

    /**
     * Adds the JitPack repository.
     */
    public void addJitPack() {
        addRepository(Repositories.JITPACK);
    }

    /**
     * Gets all the possible download URLs for this library. Entries are
     * ordered by direct download URLs first and then repository download URLs.
     *
     * @param library the library to resolve
     * @return download URLs
     */
    public Collection<String> resolveLibrary(@NotNull final Library library) {
        final Set<String> urls = new LinkedHashSet<>(requireNonNull(library, "library").getUrls());

        // Try from library-declared repos first
        for (final String repository : library.getRepositories()) {
            urls.add(repository + library.getPath());
        }

        for (final String repository : getRepositories()) {
            urls.add(repository + library.getPath());
        }

        return Collections.unmodifiableSet(urls);
    }

    /**
     * Downloads a library jar and returns the contents as a byte array.
     *
     * @param url the URL to the library jar
     * @return downloaded jar as byte array or null if nothing was downloaded
     */
    private byte[] downloadLibrary(@NotNull final String url) {
        try {
            final URLConnection connection = new URL(requireNonNull(url, "url")).openConnection();

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "powercasgamer/libby");

            try (final InputStream in = connection.getInputStream()) {
                int len;
                final byte[] buf = new byte[8192];
                final ByteArrayOutputStream out = new ByteArrayOutputStream();

                try {
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                } catch (final SocketTimeoutException e) {
                    this.logger.warn("Download timed out: " + connection.getURL());
                    return null;
                }

                this.logger.info("Downloaded library " + connection.getURL());
                return out.toByteArray();
            }
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (final IOException e) {
            if (e instanceof FileNotFoundException) {
                this.logger.debug("File not found: " + url);
            } else if (e instanceof SocketTimeoutException) {
                this.logger.debug("Connect timed out: " + url);
            } else if (e instanceof UnknownHostException) {
                this.logger.debug("Unknown host: " + url);
            } else {
                this.logger.debug("Unexpected IOException", e);
            }

            return null;
        }
    }

    /**
     * Downloads a library jar to the save directory if it doesn't already
     * exist and returns the local file path.
     * <p>
     * If the library has a checksum, it will be compared against the
     * downloaded jar's checksum to verify the integrity of the download. If
     * the checksums don't match, a warning is generated and the next download
     * URL is attempted.
     * <p>
     * Checksum comparison is ignored if the library doesn't have a checksum
     * or if the library jar already exists in the save directory.
     * <p>
     * Most of the time it is advised to use {@link #loadLibrary(Library)}
     * instead of this method because this one is only concerned with
     * downloading the jar and returning the local path. It's usually more
     * desirable to download the jar and add it to the plugin's classpath in
     * one operation.
     *
     * @param library the library to download
     * @return local file path to library
     * @see #loadLibrary(Library)
     */
    public Path downloadLibrary(@NotNull final Library library) {
        final Path file = this.saveDirectory.resolve(requireNonNull(library, "library").getPath());
        if (Files.exists(file)) {
            return file;
        }

        final Collection<String> urls = resolveLibrary(library);
        if (urls.isEmpty()) {
            throw new RuntimeException("Library '" + library + "' couldn't be resolved, add a repository");
        }

        MessageDigest md = null;
        if (library.hasChecksum()) {
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        final Path out = file.resolveSibling(file.getFileName() + ".tmp");
        out.toFile().deleteOnExit();

        try {
            Files.createDirectories(file.getParent());

            for (final String url : urls) {
                final byte[] bytes = downloadLibrary(url);
                if (bytes == null) {
                    continue;
                }

                if (md != null) {
                    final byte[] checksum = md.digest(bytes);
                    if (!Arrays.equals(checksum, library.getChecksum())) {
                        this.logger.warn("*** INVALID CHECKSUM ***");
                        this.logger.warn(" Library :  " + library);
                        this.logger.warn(" URL :  " + url);
                        this.logger.warn(" Expected :  " + Base64.getEncoder().encodeToString(library.getChecksum()));
                        this.logger.warn(" Actual :  " + Base64.getEncoder().encodeToString(checksum));
                        continue;
                    }
                }

                Files.write(out, bytes);
                Files.move(out, file);

                return file;
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(out);
            } catch (final IOException ignored) {
            }
        }

        throw new RuntimeException("Failed to download library '" + library + "'");
    }

    /**
     * Processes the input jar and generates an output jar with the provided
     * relocation rules applied, then returns the path to the relocated jar.
     *
     * @param in          input jar
     * @param out         output jar
     * @param relocations relocations to apply
     * @return the relocated file
     * @see RelocationHelper#relocate(Path, Path, Collection)
     */
    private Path relocate(@NotNull final Path in, @NotNull final String out, @NotNull final Collection<Relocation> relocations) {
        requireNonNull(in, "in");
        requireNonNull(out, "out");
        requireNonNull(relocations, "relocations");

        final Path file = this.saveDirectory.resolve(out);
        if (Files.exists(file)) {
            return file;
        }

        final Path tmpOut = file.resolveSibling(file.getFileName() + ".tmp");
        tmpOut.toFile().deleteOnExit();

        synchronized (this) {
            if (this.relocator == null) {
                this.relocator = new RelocationHelper(this);
            }
        }

        try {
            this.relocator.relocate(in, tmpOut, relocations);
            Files.move(tmpOut, file);

            this.logger.info("Relocations applied to " + this.saveDirectory.getParent().relativize(in));

            return file;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                Files.deleteIfExists(tmpOut);
            } catch (final IOException ignored) {
            }
        }
    }

    /**
     * Loads a library jar into the plugin's classpath. If the library jar
     * doesn't exist locally, it will be downloaded.
     * <p>
     * If the provided library has any relocations, they will be applied to
     * create a relocated jar and the relocated jar will be loaded instead.
     *
     * @param library the library to load
     * @see #downloadLibrary(Library)
     */
    public void loadLibrary(@NotNull final Library library) {
        Path file = downloadLibrary(requireNonNull(library, "library"));
        if (library.hasRelocations()) {
            file = relocate(file, library.getRelocatedPath(), library.getRelocations());
        }

        if (library.isIsolatedLoad()) {
            addToIsolatedClasspath(library, file);
        } else {
            addToClasspath(file);
        }
    }

    /**
     * Loads a {@link Collection} of {@link Library}'s jar into the plugin's classpath. If the library jar
     * doesn't exist locally, it will be downloaded.
     * <p>
     * If the provided library has any relocations, they will be applied to
     * create a relocated jar and the relocated jar will be loaded instead.
     *
     * @param libraries the libraries to load
     * @see #loadLibrary(Library)
     * @since 2.0.1
     */
    @ApiStatus.AvailableSince("2.0.1")
    public void loadLibraries(@NotNull final Collection<Library> libraries) {
        for (final Library library : libraries) {
            loadLibrary(library);
        }
    }
}
