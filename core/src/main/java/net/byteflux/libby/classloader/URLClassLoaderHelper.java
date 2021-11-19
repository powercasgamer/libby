package net.byteflux.libby.classloader;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.Repositories;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based wrapper around {@link URLClassLoader} for adding URLs to
 * the classpath.
 */
public class URLClassLoaderHelper {
    /**
     * The class loader being managed by this helper.
     */
    private final URLClassLoader classLoader;

    /**
     * A reflected method in {@link URLClassLoader}, when invoked adds a URL
     * to the classpath
     */
    private final Method addURLMethod;

    /**
     * Creates a new URL class loader helper.
     *
     * @param classLoader    the class loader to manage
     * @param libraryManager the library manager used to download dependencies
     */
    public URLClassLoaderHelper(URLClassLoader classLoader, LibraryManager libraryManager) {
        requireNonNull(libraryManager, "libraryManager");
        this.classLoader = requireNonNull(classLoader, "classLoader");

        try {
            addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            try {
                openUrlClassLoaderModule();
            } catch (Exception ignored) {
            }

            try {
                addURLMethod.setAccessible(true);
            } catch (Exception exception) {
                // InaccessibleObjectException has been added in Java 9
                if (exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    // It is Java 9+, try to open java.net package
                    try {
                        addOpensWithAgent(libraryManager);
                        this.addURLMethod.setAccessible(true);
                    } catch (Exception e) {
                        System.err.println("Cannot access URLClassLoader#addURL(URL), if you are using Java 9+ try to add the following option to your java command: --add-opens java.base/java.net=ALL-UNNAMED");
                        throw new RuntimeException("Cannot access URLClassLoader#addURL(URL)", e);
                    }
                } else {
                    throw new RuntimeException("Cannot set accessible URLClassLoader#addURL(URL)", exception);
                }
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a URL to the class loader's classpath.
     *
     * @param url the URL to add
     */
    public void addToClasspath(URL url) {
        try {
            addURLMethod.invoke(classLoader, requireNonNull(url, "url"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a path to the class loader's classpath.
     *
     * @param path the path to add
     */
    public void addToClasspath(Path path) {
        try {
            addToClasspath(requireNonNull(path, "path").toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
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

        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule");
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        Object thisModule = getModuleMethod.invoke(URLClassLoaderHelper.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }

    private void addOpensWithAgent(LibraryManager libraryManager) throws Exception {

        // To open URLClassLoader's module we need permissions.
        // Try to add a java agent at runtime (specifically, ByteBuddy's agent) and use it to open the module,
        // since java agents should have such permission.

        // Download ByteBuddy's agent and load it through an IsolatedClassLoader
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
        try {
            isolatedClassLoader.addPath(libraryManager.downloadLibrary(
                Library.builder()
                       .groupId("net.bytebuddy")
                       .artifactId("byte-buddy-agent")
                       .version("1.12.1")
                       .checksum("mcCtBT9cljUEniB5ESpPDYZMfVxEs1JRPllOiWTP+bM=")
                       .repository(Repositories.MAVEN_CENTRAL)
                       .build()
            ));

            Class<?> byteBuddyAgent = isolatedClassLoader.loadClass("net.bytebuddy.agent.ByteBuddyAgent");

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

            Object instrumentation = byteBuddyAgent.getDeclaredMethod("install").invoke(null);
            Class<?> instrumentationClass = Class.forName("java.lang.instrument.Instrumentation");
            Method redefineModule = instrumentationClass.getDeclaredMethod("redefineModule", Class.forName("java.lang.Module"), Set.class, Map.class, Map.class, Set.class, Map.class);
            Method getModule = Class.class.getDeclaredMethod("getModule");
            Map<String, Set<?>> toOpen = Collections.singletonMap("java.net", Collections.singleton(getModule.invoke(getClass())));
            redefineModule.invoke(instrumentation, getModule.invoke(URLClassLoader.class), Collections.emptySet(), Collections.emptyMap(), toOpen, Collections.emptySet(), Collections.emptyMap());
        } finally {
            try {
                isolatedClassLoader.close();
            } catch (Exception ignored) {
            }
        }
    }
}
