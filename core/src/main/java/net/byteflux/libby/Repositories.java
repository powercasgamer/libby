package net.byteflux.libby;

import org.jetbrains.annotations.ApiStatus;

/**
 * Class containing URLs of public repositories.
 */
public final class Repositories {

    /**
     * Maven Central repository URL.
     */
    public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    /**
     * Sonatype OSS repository URL.
     */
    public static final String SONATYPE = "https://oss.sonatype.org/content/groups/public/";

    /**
     * Sonatype OSS repository URL.
     * @since 2.0.0
     */
    public static final String SONATYPE_ALT = "https://s%s.oss.sonatype.org/content/repositories/snapshots/";

    /**
     * Bintray JCenter repository URL.
     */
    @Deprecated(forRemoval = true, since = "2.0.0")
    @ApiStatus.ScheduledForRemoval(inVersion = "2.1.0")
    @ApiStatus.Obsolete
    public static final String JCENTER = "https://jcenter.bintray.com/";

    /**
     * JitPack repository URL.
     */
    public static final String JITPACK = "https://jitpack.io/";

    private Repositories() {
        throw new UnsupportedOperationException("Private constructor");
    }
}
