# MeldMC Installer - Java Version

A cross-platform Java/Swing implementation of the MeldMC installer that maintains identical functionality to the original C++ version.

## Features

- **Identical Interface**: Recreates the exact same UI layout and behavior as the C++ version
- **Cross-Platform**: Works on Windows, macOS, and Linux with Java 8+
- **Self-Contained**: Builds as a fat JAR with all dependencies included
- **Version Management**: Fetches release and snapshot versions from Maven repositories
- **Profile Creation**: Automatically creates Minecraft launcher profiles
- **Progress Tracking**: Real-time installation progress with status updates

## Requirements

- **Java**: Version 8 or higher
- **Maven**: For building the project

## Building

```bash
cd java
mvn clean package
```

This creates a self-contained fat JAR at `target/meldmc-installer.jar`.

## Running

```bash
java -jar target/meldmc-installer.jar
```

## Dependencies

- **Jackson**: For JSON processing (launcher profiles)
- **Java XML APIs**: Built-in XML parsing for Maven metadata
- **Swing**: GUI framework (included with Java)

## Architecture

The Java version mirrors the C++ implementation:

- `MeldInstaller`: Main application class with Swing GUI
- `Version`: Version data structure matching C++ Version struct
- Maven Shade Plugin: Creates fat JAR equivalent to static linking in C++

## Compatibility

- **Java 8+**: Compatible with Java 8 through latest versions
- **Look and Feel**: Uses system native look and feel when available
- **Thread Safety**: Uses SwingWorker for background operations
- **Error Handling**: Identical error messages and user feedback as C++ version