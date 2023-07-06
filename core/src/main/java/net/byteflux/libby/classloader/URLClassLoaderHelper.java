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
package net.byteflux.libby.classloader;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.Repositories;
import sun.misc.Unsafe;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A reflection-based wrapper around {@link URLClassLoader} for adding URLs to
 * the classpath.
 */
public class URLClassLoaderHelper {

    /**
     * Unsafe class instance. Used in {@link #getPrivilegedMethodHandle(Method)}.
     */
    private static final Unsafe theUnsafe;

    static {
        Unsafe unsafe = null; // Used to make theUnsafe field final

        // getDeclaredField("theUnsafe") is not used to avoid breakage on JVMs with changed field name
        for (final Field f : Unsafe.class.getDeclaredFields()) {
            try {
                if (f.getType() == Unsafe.class && Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    unsafe = (Unsafe) f.get(null);
                }
            } catch (final Exception ignored) {
            }
        }
        theUnsafe = unsafe;
    }

    /**
     * The class loader being managed by this helper.
     */
    private final URLClassLoader classLoader;

    /**
     * A reflected method in {@link URLClassLoader}, when invoked adds a URL to the classpath.
     */
    private MethodHandle addURLMethodHandle;

    /**
     * Creates a new URL class loader helper.
     *
     * @param classLoader    the class loader to manage
     * @param libraryManager the library manager used to download dependencies
     */
    public URLClassLoaderHelper(final URLClassLoader classLoader, final LibraryManager libraryManager) {
        requireNonNull(libraryManager, "libraryManager");
        this.classLoader = requireNonNull(classLoader, "classLoader");

        try {
            final Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            try {
                openUrlClassLoaderModule();
            } catch (final Exception ignored) {
            }

            try {
                addURLMethod.setAccessible(true);
            } catch (final Exception exception) {
                // InaccessibleObjectException has been added in Java 9
                if (exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    // It is Java 9+, try to open java.net package
                    if (theUnsafe != null)
                        try {
                            this.addURLMethodHandle = getPrivilegedMethodHandle(addURLMethod).bindTo(classLoader);
                            return; // We're done
                        } catch (final Exception ignored) {
                            this.addURLMethodHandle = null; // Just to be sure the field is set to null
                        }
                    // Cannot use privileged MethodHandles.Lookup, trying with java agent
                    try {
                        addOpensWithAgent(libraryManager);
                        addURLMethod.setAccessible(true);
                    } catch (final Exception e) {
                        // Cannot access at all
                        System.err.println("Cannot access URLClassLoader#addURL(URL), if you are using Java 9+ try to add the following option to your java command: --add-opens java.base/java.net=ALL-UNNAMED");
                        throw new RuntimeException("Cannot access URLClassLoader#addURL(URL)", e);
                    }
                } else {
                    throw new RuntimeException("Cannot set accessible URLClassLoader#addURL(URL)", exception);
                }
            }
            this.addURLMethodHandle = MethodHandles.lookup().unreflect(addURLMethod).bindTo(classLoader);
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void openUrlClassLoaderModule() throws Exception {
        //
        // Thanks to lucko (Luck) <luck@lucko.me> for this snippet used in his own class loader
        //
        // This is a workaround used to maintain Java 9+ support with reflections
        // Thanks to this you will be able to run this class loader with Java 8+

        // This is effectively calling:
        //
        // URLClassLoader.class.getModule().addOpens(
        //     URLClassLoader.class.getPackageName(),
        //     URLClassLoaderHelper.class.getModule()
        // );
        //
        // We use reflection since we build against Java 8.

        final Class<?> moduleClass = Class.forName("java.lang.Module");
        final Method getModuleMethod = Class.class.getMethod("getModule");
        final Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        final Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        final Object thisModule = getModuleMethod.invoke(URLClassLoaderHelper.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }

    /**
     * Adds a URL to the class loader's classpath.
     *
     * @param url the URL to add
     */
    public void addToClasspath(final URL url) {
        try {
            this.addURLMethodHandle.invokeWithArguments(requireNonNull(url, "url"));
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a path to the class loader's classpath.
     *
     * @param path the path to add
     */
    public void addToClasspath(final Path path) {
        try {
            addToClasspath(requireNonNull(path, "path").toUri().toURL());
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private MethodHandle getPrivilegedMethodHandle(final Method method) throws Exception {
        // Try to get a MethodHandle to URLClassLoader#addURL.
        // The Unsafe class is used to get a privileged MethodHandles.Lookup instance.

        // Looking for MethodHandles.Lookup#IMPL_LOOKUP private static field
        // getDeclaredField("IMPL_LOOKUP") is not used to avoid breakage on JVMs with changed field name
        for (final Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
            if (trustedLookup.getType() != MethodHandles.Lookup.class || !Modifier.isStatic(trustedLookup.getModifiers()) || trustedLookup.isSynthetic())
                continue;

            try {
                final MethodHandles.Lookup lookup = (MethodHandles.Lookup) theUnsafe.getObject(theUnsafe.staticFieldBase(trustedLookup), theUnsafe.staticFieldOffset(trustedLookup));
                return lookup.unreflect(method);
            } catch (final Exception ignored) {
                // Unreflect went wrong, trying the next field
            }
        }

        // Every field has been tried
        throw new RuntimeException("Cannot get privileged method handle.");
    }

    private void addOpensWithAgent(final LibraryManager libraryManager) throws Exception {
        // To open URLClassLoader's module we need permissions.
        // Try to add a java agent at runtime (specifically, ByteBuddy's agent) and use it to open the module,
        // since java agents should have such permission.

        // Download ByteBuddy's agent and load it through an IsolatedClassLoader
        try (final IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader()) {
            isolatedClassLoader.addPath(libraryManager.downloadLibrary(
                    Library.builder()
                            .groupId("net.bytebuddy")
                            .artifactId("byte-buddy-agent")
                            .version("1.14.2")
                            .checksum("9CjHQXtM9wMdiPH2PYdoEc6jsBWAMBe6ngbOKs3Voec=")
                            .repository(Repositories.MAVEN_CENTRAL)
                            .build()
            ));

            final Class<?> byteBuddyAgent = isolatedClassLoader.loadClass("net.bytebuddy.agent.ByteBuddyAgent");

            // This is effectively calling:
            //
            // Instrumentation instrumentation = ByteBuddyAgent.install();
            // instrumentation.redefineModule(
            //     URLClassLoader.class.getModule(),
            //     Collections.emptySet(),
            //     Collections.emptyMap(),
            //     Collections.singletonMap("java.net", Collections.singleton(getClass().getModule())),
            //     Collections.emptySet(),
            //     Collections.emptyMap()
            // );
            //
            // For more information see https://docs.oracle.com/en/java/javase/16/docs/api/java.instrument/java/lang/instrument/Instrumentation.html
            //
            // We use reflection since we build against Java 8.

            final Object instrumentation = byteBuddyAgent.getDeclaredMethod("install").invoke(null);
            final Class<?> instrumentationClass = Class.forName("java.lang.instrument.Instrumentation");
            final Method redefineModule = instrumentationClass.getDeclaredMethod("redefineModule", Class.forName("java.lang.Module"), Set.class, Map.class, Map.class, Set.class, Map.class);
            final Method getModule = Class.class.getDeclaredMethod("getModule");
            final Map<String, Set<?>> toOpen = Collections.singletonMap("java.net", Collections.singleton(getModule.invoke(getClass())));
            redefineModule.invoke(instrumentation, getModule.invoke(URLClassLoader.class), Collections.emptySet(), Collections.emptyMap(), toOpen, Collections.emptySet(), Collections.emptyMap());
        }
    }
}
