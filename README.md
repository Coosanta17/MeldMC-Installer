# MeldMC Installer

A lightweight, cross-platform GUI installer for MeldMC Minecraft instances built with FLTK. Installer for the official Minecraft Launcher only (or any launcher that imitates it) - however other launchers are planned.

## Building

### Prerequisites

- CMake 3.31 or newer
- Conan 2 or newer

### Steps
**Install dependencies with conan**

*Linux*:
```bash
mkdir build && cd build
conan install . --build=missing -c tools.system.package_manager:mode=install -c tools.system.package_manager:sudo=True
```
*Windows and Mac:*
```bash
mkdir build
cd build
conan install . --build=missing
```

**Build with CMake**

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

The resulting binary (`MeldInstaller`) is self-contained and can be distributed as a single file.

## Usage

Simply double-click the `MeldInstaller` executable.

## What the Installer Does

1. Fetch available versions from repo
2. Adds a "MeldMC" profile to `launcher_profiles.json`
3. Creates the version folder in `./versions/{version}`
4. Downloads the platform-specific client JSON configuration from https://repo.coosanta.net

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.
