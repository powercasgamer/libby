package net.byteflux.libby.relocation;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.Repositories;
import net.byteflux.libby.classloader.IsolatedClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based helper for relocating library jars. It automatically
 * downloads and invokes Luck's Jar Relocator to perform jar relocations.
 *
 * @see <a href="https://github.com/lucko/jar-relocator">Luck's Jar Relocator</a>
 */
public class RelocationHelper {
    /**
     * Reflected constructor for creating new jar relocator instances
     */
    private final Constructor<?> jarRelocatorConstructor;

    /**
     * Reflected method for running a jar relocator
     */
    private final Method jarRelocatorRunMethod;

    /**
     * Reflected constructor for creating relocation instances
     */
    private final Constructor<?> relocationConstructor;

    /**
     * Creates a new relocation helper using the provided library manager to
     * download the dependencies required for runtime relocation.
     *
     * @param libraryManager the library manager used to download dependencies
     */
    public RelocationHelper(final LibraryManager libraryManager) {
        requireNonNull(libraryManager, "libraryManager");

        final IsolatedClassLoader classLoader = new IsolatedClassLoader();

        // ObjectWeb ASM Commons
        classLoader.addPath(libraryManager.downloadLibrary(
            Library.builder()
                   .groupId("org.ow2.asm")
                   .artifactId("asm-commons")
                   .version("9.5")
                   .checksum("cu7p+6+53o2UY/IN1YSkjO635RUq1MmHv74X3UgRya4=")
                   .repository(Repositories.MAVEN_CENTRAL)
                   .build()
        ));

        // ObjectWeb ASM
        classLoader.addPath(libraryManager.downloadLibrary(
            Library.builder()
                   .groupId("org.ow2.asm")
                   .artifactId("asm")
                   .version("9.5")
                   .checksum("ti6EtZgHKXUbBFjFNM8TZvcnVCu40VhiEzVoKkYPA1M=")
                   .repository(Repositories.MAVEN_CENTRAL)
                   .build()
        ));

        // Luck's Jar Relocator
        classLoader.addPath(libraryManager.downloadLibrary(
            Library.builder()
                   .groupId("me.lucko")
                   .artifactId("jar-relocator")
                   .version("1.6")
                   .checksum("0EkdPKSRotTUGvB7nEy5HNaB4nxTxlQOhcmWJsRyhlg=")
                   .repository(Repositories.MAVEN_CENTRAL)
                   .build()
        ));

        try {
            final Class<?> jarRelocatorClass = classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");
            final Class<?> relocationClass = classLoader.loadClass("me.lucko.jarrelocator.Relocation");

            // me.lucko.jarrelocator.JarRelocator(File, File, Collection)
            this.jarRelocatorConstructor = jarRelocatorClass.getConstructor(File.class, File.class, Collection.class);

            // me.lucko.jarrelocator.JarRelocator#run()
            this.jarRelocatorRunMethod = jarRelocatorClass.getMethod("run");

            // me.lucko.jarrelocator.Relocation(String, String, Collection, Collection)
            this.relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes the jar relocator to process the input jar and generate an
     * output jar with the provided relocation rules applied.
     *
     * @param in          input jar
     * @param out         output jar
     * @param relocations relocations to apply
     */
    public void relocate(final Path in, final Path out, final Collection<Relocation> relocations) {
        requireNonNull(in, "in");
        requireNonNull(out, "out");
        requireNonNull(relocations, "relocations");

        try {
            final List<Object> rules = new LinkedList<>();
            for (final Relocation relocation : relocations) {
                rules.add(this.relocationConstructor.newInstance(
                    relocation.getPattern(),
                    relocation.getRelocatedPattern(),
                    relocation.getIncludes(),
                    relocation.getExcludes()
                ));
            }

            this.jarRelocatorRunMethod.invoke(this.jarRelocatorConstructor.newInstance(in.toFile(), out.toFile(), rules));
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
