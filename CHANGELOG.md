## Changes in this fork

### Version 2.0.4

* Change gradle build script stuff
* Add support for being able to download snapshots.

### Version 2.0.1

* **Switch group id to `net.deltapvp.libby` instead of `org.mineorbit.libby`**

* Update sponge to v8
* Add log4j log adapter
* Updated gradle to 8.1.1
* Added convenience methods to add a Collection of libraries or repositories

### Version 2.0.0

* Updated Nukkit maven repository
* Use Gradle instead of Maven
* Added java 9 module support [Credit](https://github.com/AlessioDP/libby/pull/12) by [4drian3d](https://github.com/4drian3d)
* Added paper-plugins support. [Credit](https://github.com/AlessioDP/libby/pull/13) by [kyngs](https://github.com/kyngs)
* Dropped support for anything under Java 11
* Updated dependencies

### Version 1.1.5

* Fixed Velocity and Sponge support:
    * Removed the constructor that didn't specify `directoryName` from Sponge
    * Removed @Inject from Velocity constructors

### Version 1.1.4

* Added another way to support Java 16+ without needing any additional command line parameters (using the Unsafe class)
* Updated libraries used by Libby

### Version 1.1.3

* Added support for Java 16+ without needing `--illegal-access=permit` or `--add-opens java.base/java.net=ALL-UNNAMED` (using [ByteBuddy's Java Agent](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-agent))
* Added possibility to specify per-library repositories with `Library.Builder#repository(String repositoryURL)`
* Avoid registration of duplicated repositories

### Version 1.1.2

* Added support for libraries compiled with Java 16
* Updated libraries used by Libby

### Version 1.1.1

* Download directory name can now be changed when instantiating the LibraryManager
* When loading a library with `libraryBuilder.isolatedLoad(true).id(aId)` and an IsolatedClassLoader with id `aId` is present
  it will be used instead of creating a new one

### Version 1.1.0

* Libraries can be loaded from an `IsolatedClassLoader`
    * Use `LibraryManager#getIsolatedClassLoaderOf(...)` to get the `IsolatedClassLoader` via its `id`
    * Use `Library.Builder#id(...)` to set an ID to the library
    * Use `Library.Builder#isolatedLoad(...)` to load it via `IsolatedClassLoader`
* Libraries are updated
* Support for Java 9+ Modules to prevent deprecations
* Distribution management to repo.alessiodp.com
