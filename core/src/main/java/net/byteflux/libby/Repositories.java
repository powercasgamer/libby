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
     *
     * @since 2.0.0
     */
    public static final String SONATYPE_ALT = "https://s%s.oss.sonatype.org/content/repositories/snapshots/";

    /**
     * Bintray JCenter repository URL.
     */
    @Deprecated(
            forRemoval = true,
            since = "2.0.0"
    )
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
