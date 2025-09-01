# MeldMC Installer

A lightweight, cross-platform GUI installer for MeldMC Minecraft instances built with FLTK.

- **C++ Version** (`c++/`): FLTK-based installer using C++20 with statically linked binaries.
- **Java Version** (`java/`): Swing-based installer using Java 8+ with fat JAR distribution.

All versions provide identical functionality for the official Minecraft Launcher (or any launcher that imitates it).

Third-party Minecraft launchers are a planned feature, however an installer for these launchers is likely not needed.

## Antivirus detection

Many antivirus softwares will flag the installer as a "Trojan" or "Generic", this is unfortunately due to the nature of the installation modifying Minecraft files. If the antivirus is being particularly persistant, you can use the Java version (if you aren't already). **The installer is virus free!** If you want to confirm, you can build it yourself!

## Building

### C++ Version

**Prerequisites:**
- CMake 3.31 or newer
- Conan 2 or newer

**Building:**

1. Install dependencies with conan:

    *Linux*:
    ```bash
    mkdir build && cd build
    conan install . --build=missing -c tools.system.package_manager:mode=install -c tools.system.package_manager:sudo=True
    ```

    *Windows and Mac:* (Windows must use MSVC, because OpenSSL is not supported on GCC MingW)
    ```bash
    mkdir build
    cd build
    conan install . --build=missing
    ```

2. Build with CMake

    *Linux and Mac:*
    ```bash
    cmake -S .. -DCMAKE_TOOLCHAIN_FILE=build/Release/generators/conan_toolchain.cmake -DCMAKE_BUILD_TYPE=Release
    make -j
    ```
    *Windows:*
    ```bash
    cmake -S .. -DCMAKE_TOOLCHAIN_FILE=generators\conan_toolchain.cmake -DCMAKE_BUILD_TYPE=Release
    cmake --build . --config Release
    ```

3. The resulting binary (`MeldInstaller`) is self-contained and can be distributed as a single file.

### Java Version

**Prerequisites:**
- Java 8 or newer
- Maven

**Building:**
```bash
cd java
mvn clean package
java -jar target/meldmc-installer.jar
```

## Usage

Simply double-click the meld installer executable (or jar).

## What the Installer Does

1. Fetch available versions from repo
2. Adds a "MeldMC" profile to `launcher_profiles.json`
3. Creates the version folder in `./versions/{version}`
4. Downloads the platform-specific client JSON configuration from https://repo.coosanta.net

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.
