# Libby (AlessioDP)

## Changes in this fork

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

# Libby

A runtime dependency management library for plugins running in Java-based Minecraft
server platforms.

Libraries can be downloaded from Maven repositories (or direct URLs) into a plugin's data
folder, relocated and then loaded into the plugin's classpath at runtime.

### Why use runtime dependency management?

Due to file size constraints on plugin hosting services like SpigotMC, some plugins with
bundled dependencies become too large to be uploaded.

Using runtime dependency management, dependencies are downloaded and cached by the server
and don't need to be bundled with the plugin, which significantly reduces the size of the
plugin jar.

A smaller plugin jar also means shorter download times and less network strain for authors
who self-host their plugins on servers with limited bandwidth.

### Usage

Firstly, add the maven artifact to your `pom.xml`
```xml
<!-- Libby (AlessioDP) Repository -->
<repository>
  <id>AlessioDP</id>
  <url>https://repo.alessiodp.com/releases/</url>
</repository>

<dependency>
    <groupId>net.byteflux</groupId>
    <artifactId>libby-bukkit</artifactId> <!-- Replace bukkit if you're using another platform -->
    <version>1.1.4</version>
</dependency>
```

Remember to **always** relocate Libby to avoid conflicts
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <relocations>
            <relocation>
                <pattern>net.byteflux.libby</pattern>
                <shadedPattern>yourPackage.libs.net.byteflux.libby</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

Then, create a new LibraryManager instance
```java
// Create a library manager for a Bukkit/Spigot plugin
BukkitLibraryManager bukkitLibraryManager = new BukkitLibraryManager(plugin);

// Create a library manager for a Bungee plugin
BungeeLibraryManager bungeeLibraryManager = new BungeeLibraryManager(plugin);

// Also Nukkit, Sponge, and Velocity are supported
```

Create a Library instance with the library builder
```java
Library lib = Library.builder()
    .groupId("your{}dependency{}groupId") // "{}" is replaced with ".", useful to avoid unwanted changes made by maven-shade-plugin
    .artifactId("artifactId")
    .version("version")
     // The following are optional

     // Sets an id for the library
    .id("my-lib")
     // Relocation is applied to the downloaded jar before loading it
    .relocate("package{}to{}relocate", "the{}relocated{}package") // "{}" is replaced with ".", useful to avoid unwanted changes made by maven-shade-plugin
     // The library is loaded into an IsolatedClassLoader, which is in common between every library with the same id
    .isolatedLoad(true)
    .classifier("customClassifier")
    .checksum("Base64-encoded SHA-256 checksum")
    .build();
```

Finally, add Maven Central (or other repositories) to the library manager and download your library. To do this,
you can use the `LibraryManager#loadLibrary(Library libraryToLoad)` method, which automatically downloads and then loads the provided library.
```java
libraryManager.addMavenCentral();
libraryManager.loadLibrary(lib);
```

<details><summary>Complete code</summary>

```java
BukkitLibraryManager libraryManager = new BukkitLibraryManager(plugin);

Library lib = Library.builder()
    .groupId("your{}dependency{}groupId") // "{}" is replaced with ".", useful to avoid unwanted changes made by maven-shade-plugin
    .artifactId("artifactId")
    .version("version")
     // The following are optional

     // Sets an id for the library
    .id("my-lib")
     // Relocation is applied to the downloaded jar before loading it
    .relocate("package{}to{}relocate", "the{}relocated{}package") // "{}" is replaced with ".", useful to avoid unwanted changes made by maven-shade-plugin
     // The library is loaded into an IsolatedClassLoader, which is in common between every library with the same id
    .isolatedLoad(true)
    .classifier("customClassifier")
    .checksum("Base64-encoded SHA-256 checksum")
    .build();

libraryManager.addMavenCentral();
libraryManager.loadLibrary(lib);
```

</details>

## Credits

Special thanks to:

* [Luck](https://github.com/lucko) for [LuckPerms](https://github.com/lucko/LuckPerms)
  and its dependency management system which was the original inspiration for this project
  and another thanks for [jar-relocator](https://github.com/lucko/jar-relocator) which is
  used by Libby to perform jar relocations.
* [Glare](https://github.com/darbyjack) for convincing me (Byteflux) that I should publish this
  library instead of letting it sit around collecting dust :)
