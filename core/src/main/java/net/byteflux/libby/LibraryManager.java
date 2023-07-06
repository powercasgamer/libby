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

import net.byteflux.libby.classloader.IsolatedClassLoader;
import net.byteflux.libby.logging.LogLevel;
import net.byteflux.libby.logging.Logger;
import net.byteflux.libby.logging.adapters.LogAdapter;
import net.byteflux.libby.relocation.Relocation;
import net.byteflux.libby.relocation.RelocationHelper;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .priority(5)
            .build();

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
     * Map of isolated class loaders and theirs id
     */
    private final Map<String, IsolatedClassLoader> isolatedLibraries = new HashMap<>();

    /**
     * Lazily-initialized relocation helper that uses reflection to call into
     * Luck's Jar Relocator
     */
    private RelocationHelper relocator;

    /**
     * Creates a new library manager.
     *
     * @param logAdapter    plugin logging adapter
     * @param dataDirectory plugin's data directory
     * @param directoryName download directory name
     */
    protected LibraryManager(final @NotNull LogAdapter logAdapter, final @NotNull Path dataDirectory, final @NotNull String directoryName) {
        this.logger = new Logger(requireNonNull(logAdapter, "logAdapter"));
        this.saveDirectory = requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve(requireNonNull(directoryName, "directoryName"));
    }

    /**
     * Adds a file to the plugin's classpath.
     *
     * @param file the file to add
     */
    protected abstract void addToClasspath(final @NotNull Path file);

    /**
     * Adds a file to the isolated class loader
     *
     * @param library the library to add
     * @param file    the file to add
     */
    protected void addToIsolatedClasspath(final @NotNull Library library, final @NotNull Path file) {
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
    public @Nullable IsolatedClassLoader getIsolatedClassLoaderOf(final @NotNull String libraryId) {
        return this.isolatedLibraries.get(libraryId);
    }

    /**
     * Gets the logging level for this library manager.
     *
     * @return log level
     */
    public @NotNull LogLevel getLogLevel() {
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
    public void setLogLevel(final @NotNull LogLevel level) {
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
    public @NotNull Collection<@Nullable String> getRepositories() {
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
    public void addRepository(final @NotNull String url) {
        final String repo = !requireNonNull(url, "url").isEmpty() && requireNonNull(url, "url").charAt(requireNonNull(url, "url").length() - 1) == '/' ? url : url + '/';
        synchronized (this.repositories) {
            this.repositories.add(repo);
        }
    }

    /**
     * Adds a {@link Collection} of repository URLs to this library manager.
     * <p>
     * Artifacts will be resolved using these repositories when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param urls repository URLs to add
     * @since 2.0.1
     */
    @ApiStatus.AvailableSince("2.0.1")
    public void addRepositories(final @NotNull Collection<@NotNull String> urls) {
        for (final String url : urls) {
            addRepository(url);
        }
    }

    /**
     * Adds an array of repository URLs to this library manager.
     * <p>
     * Artifacts will be resolved using these repositories when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param urls repository URLs to add
     * @since 2.0.1
     */
    @ApiStatus.AvailableSince("2.0.1")
    public void addRepositories(final @NotNull String @NotNull ... urls) {
        for (final String url : urls) {
            addRepository(url);
        }
    }

    /**
     * Adds the current user's local Maven repository.
     */
    public void addMavenLocal() {
        addRepository(Path.of(System.getProperty("user.home")).resolve(".m2/repository").toUri().toString());
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
     *
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
    @Deprecated(
            forRemoval = true,
            since = "2.0.0"
    )
    @ApiStatus.ScheduledForRemoval(inVersion = "2.1.0")
    @ApiStatus.Obsolete
    public void addJCenter() {
        throw new UnsupportedOperationException("JCenter has shut down. Use another repository.");
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
    public @NotNull Collection<@Nullable String> resolveLibrary(final @NotNull Library library) {
        final Set<String> urls = new LinkedHashSet<>(requireNonNull(library, "library").getUrls());
        final boolean snapshot = library.getVersion().endsWith("-SNAPSHOT");
        final Set<String> repos = new HashSet<>();
        repos.addAll(library.getRepositories());
        repos.addAll(getRepositories());

        if (!snapshot) {
            for (final String repository : repos) {
                urls.add(repository + library.getPath());
            }
        } else {
            final MetadataXpp3Reader reader = new MetadataXpp3Reader();
            final String pathv2 = library.getPath().substring(0, library.getGAV().length()) + "/maven-metadata.xml";
            final String pathv3 = library.getPath().substring(0, library.getGAV().length());
            try {
                for (final String repository : repos) {
                    final HttpResponse<InputStream> response = HTTP_CLIENT.send(
                            HttpRequest.newBuilder()
                                    .GET()
                                    .header("User-Agent", "powercasgamer/libby")
                                    .uri(URI.create(repository + pathv2))
                                    .build(), HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 200) continue;
                    final Metadata metadata = reader.read(response.body());
                    final Versioning versioning = metadata.getVersioning();
                    final Snapshot snapshat = versioning.getSnapshot();
                    urls.add(repository + pathv3 + "/" + library.getArtifactId() + "-" + library.getVersion().replace("-SNAPSHOT", "") + "-" + snapshat.getTimestamp() + "-" + snapshat.getBuildNumber() + ".jar");
                }
            } catch (final Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return Collections.unmodifiableSet(urls);
    }

    /**
     * Downloads a library jar and returns the contents as a byte array.
     *
     * @param url the URL to the library jar
     * @return downloaded jar as byte array or null if nothing was downloaded
     */
    private byte[] downloadLibrary(final @NotNull String url) {
        try {
            final HttpResponse<InputStream> input = HTTP_CLIENT.send(HttpRequest.newBuilder()
                    .GET()
                    .header("User-Agent", "powercasgamer/libby")
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(5000))
                    .build(), HttpResponse.BodyHandlers.ofInputStream());

            try (final InputStream in = input.body()) {
                int len;
                final byte[] buf = new byte[8192];
                final ByteArrayOutputStream out = new ByteArrayOutputStream();

                try {
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                } catch (final SocketTimeoutException e) {
                    this.logger.warn("Download timed out: " + input.uri(), e);
                    return null;
                }

                this.logger.info("Downloaded library " + input.uri());
                return out.toByteArray();
            }
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (final IOException e) {
            if (e instanceof FileNotFoundException) {
                this.logger.debug("File not found: " + url, e);
            } else if (e instanceof SocketTimeoutException) {
                this.logger.debug("Connect timed out: " + url, e);
            } else if (e instanceof UnknownHostException) {
                this.logger.debug("Unknown host: " + url, e);
            } else {
                this.logger.debug("Unexpected IOException", e);
            }
            return null;
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
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
    public @UnknownNullability Path downloadLibrary(final @NotNull Library library) {
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
        } catch (final Exception e) {
            throw new RuntimeException("Failed to download library '" + library + "'", e);
        } finally {
            try {
                Files.deleteIfExists(out);
            } catch (final IOException exc) {
                this.logger.debug("Failed to delete temporary file: " + out, exc);
            } catch (final Exception exc) {
                this.logger.error("Failed to download library '" + library + "'", exc);
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
    private @NotNull Path relocate(final @NotNull Path in, final @NotNull String out, final @NotNull Collection<@Nullable Relocation> relocations) {
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
    public void loadLibrary(final @NotNull Library library) {
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
     * Loads a {@link Collection} of {@link Library}'s jars into the plugin's classpath. If the library jar
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
    public void loadLibraries(final @NotNull Collection<? extends Library> libraries) {
        for (final Library library : libraries) {
            loadLibrary(library);
        }
    }

    /**
     * Loads an array of {@link Library}'s jars into the plugin's classpath. If the library jar
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
    public void loadLibraries(final @NotNull Library... libraries) {
        for (final Library library : libraries) {
            loadLibrary(library);
        }
    }
}
