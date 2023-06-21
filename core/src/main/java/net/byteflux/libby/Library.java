package net.byteflux.libby;

import net.byteflux.libby.relocation.Relocation;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * An immutable representation of a Maven artifact that can be downloaded,
 * relocated and then loaded into a plugin's classpath at runtime.
 *
 * @see #builder()
 */
public class Library {
    /**
     * Direct download URLs for this library
     */
    private final Collection<String> urls;

    /**
     * Repository URLs for this library
     */
    private final Collection<String> repositories;

    /**
     * Library id (used by Isolated Class Loaders)
     */
    private final String id;

    /**
     * Maven group ID
     */
    private final @NotNull String groupId;

    /**
     * Maven artifact ID
     */
    private final @NotNull String artifactId;

    /**
     * Artifact version
     */
    private final @NotNull String version;

    /**
     * Artifact classifier
     */
    private final String classifier;

    /**
     * Binary SHA-256 checksum for this library's jar file
     */
    private final byte[] checksum;

    /**
     * Jar relocations to apply
     */
    private final Collection<Relocation> relocations;

    /**
     * Relative Maven path to this library's artifact
     */
    private final String path;

    /**
     * Relative path to this library's relocated jar
     */
    private final String relocatedPath;

    /**
     * Should this library be loaded in an isolated class loader?
     */
    private final boolean isolatedLoad;

    /**
     * Creates a new library.
     *
     * @param urls         direct download URLs
     * @param id           library ID
     * @param groupId      Maven group ID
     * @param artifactId   Maven artifact ID
     * @param version      artifact version
     * @param classifier   artifact classifier or null
     * @param checksum     binary SHA-256 checksum or null
     * @param relocations  jar relocations or null
     * @param isolatedLoad isolated load for this library
     */
    private Library(final Collection<String> urls,
                    final String id,
                    final @NotNull String groupId,
                    final @NotNull String artifactId,
                    final @NotNull String version,
                    final String classifier,
                    final byte[] checksum,
                    final Collection<Relocation> relocations,
                    final boolean isolatedLoad) {

        this(urls, null, id, groupId, artifactId, version, classifier, checksum, relocations, isolatedLoad);
    }

    /**
     * Creates a new library.
     *
     * @param urls         direct download URLs
     * @param repositories repository URLs
     * @param id           library ID
     * @param groupId      Maven group ID
     * @param artifactId   Maven artifact ID
     * @param version      artifact version
     * @param classifier   artifact classifier or null
     * @param checksum     binary SHA-256 checksum or null
     * @param relocations  jar relocations or null
     * @param isolatedLoad isolated load for this library
     */
    private Library(final Collection<String> urls,
                    final Collection<String> repositories,
                    final String id,
                    final @NotNull String groupId,
                    final @NotNull String artifactId,
                    final @NotNull String version,
                    final String classifier,
                    final byte[] checksum,
                    final Collection<Relocation> relocations,
                    final boolean isolatedLoad) {

        this.urls = urls != null ? Collections.unmodifiableList(new LinkedList<>(urls)) : Collections.emptyList();
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.groupId = requireNonNull(groupId, "groupId").replace("{}", ".");
        this.artifactId = requireNonNull(artifactId, "artifactId");
        this.version = requireNonNull(version, "version");
        this.classifier = classifier;
        this.checksum = checksum;
        this.relocations = relocations != null ? Collections.unmodifiableList(new LinkedList<>(relocations)) : Collections.emptyList();

        String path = this.groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version;
        if (hasClassifier()) {
            path += '-' + classifier;
        }

        this.path = path + ".jar";

        this.repositories = repositories != null ? Collections.unmodifiableList(new LinkedList<>(repositories)) : Collections.emptyList();
        this.relocatedPath = hasRelocations() ? path + "-relocated.jar" : null;
        this.isolatedLoad = isolatedLoad;
    }

    /**
     * Gets the direct download URLs for this library.
     *
     * @return direct download URLs
     */
    public Collection<String> getUrls() {
        return this.urls;
    }

    /**
     * Gets the repositories URLs for this library.
     *
     * @return repositories URLs
     */
    public Collection<String> getRepositories() {
        return this.repositories;
    }

    /**
     * Gets the library ID
     *
     * @return the library id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the Maven group ID for this library.
     *
     * @return Maven group ID
     */
    public @NotNull String getGroupId() {
        return this.groupId;
    }

    /**
     * Gets the Maven artifact ID for this library.
     *
     * @return Maven artifact ID
     */
    public @NotNull String getArtifactId() {
        return this.artifactId;
    }

    /**
     * Gets the artifact version for this library.
     *
     * @return artifact version
     */
    public @NotNull String getVersion() {
        return this.version;
    }

    /**
     * Gets the artifact classifier for this library.
     *
     * @return artifact classifier or null
     */
    public String getClassifier() {
        return this.classifier;
    }

    /**
     * Gets whether this library has an artifact classifier.
     *
     * @return true if library has classifier, false otherwise
     */
    public boolean hasClassifier() {
        return this.classifier != null;
    }

    /**
     * Gets the binary SHA-256 checksum of this library's jar file.
     *
     * @return checksum or null
     */
    public byte[] getChecksum() {
        return this.checksum;
    }

    /**
     * Gets whether this library has a checksum.
     *
     * @return true if library has checksum, false otherwise
     */
    public boolean hasChecksum() {
        return this.checksum != null;
    }

    /**
     * Gets the jar relocations to apply to this library.
     *
     * @return jar relocations to apply
     */
    public Collection<Relocation> getRelocations() {
        return this.relocations;
    }

    /**
     * Gets whether this library has any jar relocations.
     *
     * @return true if library has relocations, false otherwise
     */
    public boolean hasRelocations() {
        return !this.relocations.isEmpty();
    }

    /**
     * Gets the relative Maven path to this library's artifact.
     *
     * @return Maven path for this library
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Gets the GAV of this library.
     *
     * @return the GAV in the format groupId:artifactId:version
     * @since 2.0.4
     */
    public String getGAV() {
        return this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    /**
     * Gets the relative path to this library's relocated jar.
     *
     * @return path to relocated artifact or null if has no relocations
     */
    public String getRelocatedPath() {
        return this.relocatedPath;
    }

    /**
     * Is the library loaded isolated?
     *
     * @return true if the library is loaded isolated
     */
    public boolean isIsolatedLoad() {
        return this.isolatedLoad;
    }

    /**
     * Gets a concise, human-readable string representation of this library.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        String name = this.groupId + ':' + this.artifactId + ':' + this.version;
        if (hasClassifier()) {
            name += ':' + this.classifier;
        }

        return name;
    }

    /**
     * Creates a new library builder.
     *
     * @return new library builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Due to the constructor complexity of an immutable {@link Library},
     * instead this fluent builder is used to configure and then construct
     * a new library.
     */
    public static class Builder {
        /**
         * Direct download URLs for this library
         */
        private final Collection<String> urls = new LinkedList<>();

        /**
         * Repository URLs for this library
         */
        private final Collection<String> repositories = new LinkedList<>();

        /**
         * The library ID
         */
        private String id;

        /**
         * Maven group ID
         */
        private @NotNull String groupId;

        /**
         * Maven artifact ID
         */
        private @NotNull String artifactId;

        /**
         * Artifact version
         */
        private @NotNull String version;

        /**
         * Artifact classifier
         */
        private String classifier;

        /**
         * Binary SHA-256 checksum for this library's jar file
         */
        private byte[] checksum;

        /**
         * Isolated load
         */
        private boolean isolatedLoad;

        /**
         * Jar relocations to apply
         */
        private final Collection<Relocation> relocations = new LinkedList<>();

        /**
         * Adds a direct download URL for this library.
         *
         * @param url direct download URL
         * @return this builder
         */
        public Builder url(final String url) {
            this.urls.add(requireNonNull(url, "url"));
            return this;
        }

        /**
         * Adds a repository URL for this library.
         * <p>Most common repositories can be found in {@link Repositories} class as constants.
         * <p>Note that repositories should be preferably added to the {@link LibraryManager} via {@link LibraryManager#addRepository(String)}.
         *
         * @param url repository URL
         * @return this builder
         */
        public Builder repository(final String url) {
            this.repositories.add(!requireNonNull(url, "repository").isEmpty() && requireNonNull(url, "repository").charAt(requireNonNull(url, "repository").length() - 1) == '/' ? url : url + '/');
            return this;
        }

        /**
         * Sets the id for this library.
         *
         * @param id the ID
         * @return this builder
         */
        public Builder id(final String id) {
            this.id = id != null ? id : UUID.randomUUID().toString();
            return this;
        }

        /**
         * Sets the Maven group ID for this library.
         *
         * @param groupId Maven group ID
         * @return this builder
         */
        public Builder groupId(final @NotNull String groupId) {
            this.groupId = requireNonNull(groupId, "groupId");
            return this;
        }

        /**
         * Sets the Maven artifact ID for this library.
         *
         * @param artifactId Maven artifact ID
         * @return this builder
         */
        public Builder artifactId(final @NotNull String artifactId) {
            this.artifactId = requireNonNull(artifactId, "artifactId");
            return this;
        }

        /**
         * Sets the artifact version for this library.
         *
         * @param version artifact version
         * @return this builder
         */
        public Builder version(final @NotNull String version) {
            this.version = requireNonNull(version, "version");
            return this;
        }

        /**
         * Sets the artifact classifier for this library.
         *
         * @param classifier artifact classifier
         * @return this builder
         */
        public Builder classifier(final String classifier) {
            this.classifier = requireNonNull(classifier, "classifier");
            return this;
        }

        /**
         * Sets the binary SHA-256 checksum for this library.
         *
         * @param checksum binary SHA-256 checksum
         * @return this builder
         */
        public Builder checksum(final byte[] checksum) {
            this.checksum = requireNonNull(checksum, "checksum");
            return this;
        }

        /**
         * Sets the Base64-encoded SHA-256 checksum for this library.
         *
         * @param checksum Base64-encoded SHA-256 checksum
         * @return this builder
         */
        public Builder checksum(final String checksum) {
            return checksum(Base64.getDecoder().decode(requireNonNull(checksum, "checksum")));
        }

        /**
         * Sets the isolated load for this library.
         *
         * @param isolatedLoad the isolated load boolean
         * @return this builder
         */
        public Builder isolatedLoad(final boolean isolatedLoad) {
            this.isolatedLoad = isolatedLoad;
            return this;
        }

        /**
         * Adds a jar relocation to apply to this library.
         *
         * @param relocation jar relocation to apply
         * @return this builder
         */
        public Builder relocate(final Relocation relocation) {
            this.relocations.add(requireNonNull(relocation, "relocation"));
            return this;
        }

        /**
         * Adds a jar relocation to apply to this library.
         *
         * @param pattern          search pattern
         * @param relocatedPattern replacement pattern
         * @return this builder
         */
        public Builder relocate(final String pattern, final String relocatedPattern) {
            return relocate(new Relocation(pattern, relocatedPattern));
        }

        /**
         * Creates a new library using this builder's configuration.
         *
         * @return new library
         */
        public Library build() {
            return new Library(this.urls, this.repositories, this.id, this.groupId, this.artifactId, this.version, this.classifier, this.checksum, this.relocations, this.isolatedLoad);
        }
    }
}
