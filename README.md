# MeldMC Installer

A cross-platform GUI installer for MeldMC Minecraft instances built with Qt6.

## Features

- **Cross-Platform**: Works on Windows, macOS, and Linux
- **Version Management**: Automatically fetches available releases and snapshots
- **Smart Defaults**: Detects OS-specific Minecraft directories
- **User-Friendly**: Clean GUI interface with progress feedback
- **Error Handling**: Comprehensive error reporting with copyable messages
- **Launcher Integration**: Automatically adds MeldMC profile to Minecraft launcher

## Building

### Prerequisites

- CMake 3.31 or newer
- Qt6 development libraries
- libcurl development libraries
- nlohmann/json library
- tinyxml2 library

### Ubuntu/Debian

```bash
sudo apt install qt6-base-dev libqt6widgets6 qt6-base-dev-tools libcurl4-openssl-dev libtinyxml2-dev nlohmann-json3-dev
```

### Build Steps

```bash
mkdir build
cd build
cmake ..
make
```

## Usage

1. **Launch the installer**: Run the `MeldInstaller` executable
2. **Select version type**: Choose between "Releases" (stable) or "Snapshots" (development)
3. **Pick version**: Select the desired version from the dropdown (latest shown first)
4. **Set Minecraft directory**: Use the default or browse to select a custom location
5. **Install**: Click "Install MeldMC" to complete the installation

### Default Minecraft Directories

- **Windows**: `%APPDATA%/.minecraft`
- **macOS**: `~/Library/Application Support/minecraft`
- **Linux**: `~/.minecraft`

## What the Installer Does

1. **Profile Creation**: Adds a "MeldMC" profile to your `launcher_profiles.json`
2. **Version Setup**: Creates the appropriate version directory structure
3. **Client Download**: Downloads the platform-specific client JSON configuration

## Architecture

The installer is built with the following components:

- **Version Fetcher**: Downloads and parses Maven metadata XML to get available versions
- **GUI Framework**: Qt6-based interface with cross-platform compatibility
- **OS Detection**: Automatic platform detection for directory paths and client downloads
- **Profile Manager**: Handles Minecraft launcher profile integration
- **Error Handling**: Comprehensive error reporting and graceful fallbacks

## Development

The codebase follows modern C++ practices with:
- Qt6 for GUI and cross-platform support
- libcurl for HTTP requests
- nlohmann/json for JSON processing
- tinyxml2 for XML parsing
- CMake for cross-platform building

### Mock Data

When the MeldMC repository is not available, the installer falls back to mock data for testing purposes. This ensures the application can be tested even when the backend infrastructure is not yet deployed.

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.