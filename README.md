# MeldMC Installer

A lightweight, cross-platform GUI installer for MeldMC Minecraft instances built with FLTK. Installer for the official Minecraft Launcher only (or any launcher that imitates it) - however other launchers are planned.

## Building

### Prerequisites

- CMake 3.31 or newer
- FLTK 1.3 development libraries
- libcurl development libraries
- nlohmann/json library
- tinyxml2 library

### Steps
**Install dependencies with APT:**
```bash
sudo apt install libfltk1.3-dev libcurl4-openssl-dev libtinyxml2-dev nlohmann-json3-dev
```

**Install dependencies on Windows/Mac:**

Good luck!

***

**Build with CMake**
```bash
mkdir build && cd build
cmake ..
make
```

The resulting binary (`MeldInstaller`) is self-contained and can be distributed as a single file.

## Usage

Simply double-click the `MeldInstaller` executable - no installation required! The application will:

1. **Auto-detect your platform**: Automatically sets the correct Minecraft directory for your OS
2. **Fetch available versions**: Downloads the latest releases and snapshots
3. **Provide a simple interface**: Clean, lightweight GUI for easy installation

## What the Installer Does

1. **Profile Creation**: Adds a "MeldMC" profile to your `launcher_profiles.json`
2. **Version Setup**: Creates the version folder in ./versions/{version}
3. **Client Download**: Downloads the platform-specific client JSON configuration from [https://repo.coosanta.net]

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.
