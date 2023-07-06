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
package net.byteflux.libby.relocation;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Relocations are used to describe a search and replace pattern for renaming
 * packages in a library jar for the purpose of preventing namespace conflicts
 * with other plugins that bundle their own version of the same library.
 */
public class Relocation {
    /**
     * Search pattern
     */
    private final String pattern;

    /**
     * Replacement pattern
     */
    private final String relocatedPattern;

    /**
     * Classes and resources to include
     */
    private final Collection<String> includes;

    /**
     * Classes and resources to exclude
     */
    private final Collection<String> excludes;

    /**
     * Creates a new relocation.
     *
     * @param pattern          search pattern
     * @param relocatedPattern replacement pattern
     * @param includes         classes and resources to include
     * @param excludes         classes and resources to exclude
     */
    public Relocation(final String pattern, final String relocatedPattern, final Collection<String> includes, final Collection<String> excludes) {
        this.pattern = requireNonNull(pattern, "pattern").replace("{}", ".");
        this.relocatedPattern = requireNonNull(relocatedPattern, "relocatedPattern").replace("{}", ".");
        this.includes = includes != null ? Collections.unmodifiableList(new LinkedList<>(includes)) : Collections.emptyList();
        this.excludes = excludes != null ? Collections.unmodifiableList(new LinkedList<>(excludes)) : Collections.emptyList();
    }

    /**
     * Creates a new relocation with empty includes and excludes.
     *
     * @param pattern          search pattern
     * @param relocatedPattern replacement pattern
     */
    public Relocation(final String pattern, final String relocatedPattern) {
        this(pattern, relocatedPattern, null, null);
    }

    /**
     * Creates a new relocation builder.
     *
     * @return new relocation builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the search pattern.
     *
     * @return pattern to search
     */
    public String getPattern() {
        return this.pattern;
    }

    /**
     * Gets the replacement pattern.
     *
     * @return pattern to replace with
     */
    public String getRelocatedPattern() {
        return this.relocatedPattern;
    }

    /**
     * Gets included classes and resources.
     *
     * @return classes and resources to include
     */
    public Collection<String> getIncludes() {
        return this.includes;
    }

    /**
     * Gets excluded classes and resources.
     *
     * @return classes and resources to exclude
     */
    public Collection<String> getExcludes() {
        return this.excludes;
    }

    /**
     * Provides an alternative method of creating a {@link Relocation}. This
     * builder may be more intuitive for configuring relocations that also have
     * any includes or excludes.
     */
    public static class Builder {
        /**
         * Classes and resources to include
         */
        private final Collection<String> includes = new LinkedList<>();
        /**
         * Classes and resources to exclude
         */
        private final Collection<String> excludes = new LinkedList<>();
        /**
         * Search pattern
         */
        private String pattern;
        /**
         * Replacement pattern
         */
        private String relocatedPattern;

        /**
         * Sets the search pattern.
         *
         * @param pattern pattern to search
         * @return this builder
         */
        public Builder pattern(final String pattern) {
            this.pattern = requireNonNull(pattern, "pattern");
            return this;
        }

        /**
         * Sets the replacement pattern.
         *
         * @param relocatedPattern pattern to replace with
         * @return this builder
         */
        public Builder relocatedPattern(final String relocatedPattern) {
            this.relocatedPattern = requireNonNull(relocatedPattern, "relocatedPattern");
            return this;
        }

        /**
         * Adds a class or resource to be included.
         *
         * @param include class or resource to include
         * @return this builder
         */
        public Builder include(final String include) {
            this.includes.add(requireNonNull(include, "include"));
            return this;
        }

        /**
         * Adds a class or resource to be excluded.
         *
         * @param exclude class or resource to exclude
         * @return this builder
         */
        public Builder exclude(final String exclude) {
            this.excludes.add(requireNonNull(exclude, "exclude"));
            return this;
        }

        /**
         * Creates a new relocation using this builder's configuration.
         *
         * @return new relocation
         */
        public Relocation build() {
            return new Relocation(this.pattern, this.relocatedPattern, this.includes, this.excludes);
        }
    }
}
