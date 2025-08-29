# MeldMC Installer

A lightweight, cross-platform GUI installer for MeldMC Minecraft instances built with FLTK.

## Features

- **Lightweight & Self-Contained**: Small binary (~2MB) with minimal dependencies
- **Cross-Platform**: Works on Windows, macOS, and Linux
- **Version Management**: Automatically fetches available releases and snapshots
- **Smart Defaults**: Detects OS-specific Minecraft directories
- **User-Friendly**: Clean GUI interface with progress feedback
- **Error Handling**: Comprehensive error reporting with helpful messages
- **Launcher Integration**: Automatically adds MeldMC profile to Minecraft launcher

## Building

### Prerequisites

- CMake 3.31 or newer
- FLTK 1.3 development libraries
- libcurl development libraries
- nlohmann/json library
- tinyxml2 library

### Ubuntu/Debian

```bash
sudo apt install libfltk1.3-dev libcurl4-openssl-dev libtinyxml2-dev nlohmann-json3-dev
```

### Build Steps

```bash
mkdir build
cd build
cmake ..
make
```

The resulting binary (`MeldInstaller`) is self-contained and can be distributed as a single file.

## Usage

### As a Portable Application

Simply double-click the `MeldInstaller` executable - no installation required! The application will:

1. **Auto-detect your platform**: Automatically sets the correct Minecraft directory for your OS
2. **Fetch available versions**: Downloads the latest releases and snapshots
3. **Provide a simple interface**: Clean, lightweight GUI for easy installation

### Installation Steps

1. **Launch the installer**: Double-click or run `./MeldInstaller` 
2. **Select version type**: Choose between "Release" (stable) or "Snapshot" (development)
3. **Pick version**: Select the desired version from the dropdown (latest shown first)
4. **Set Minecraft directory**: Use the default or browse to select a custom location
5. **Install**: Click "Install" to complete the installation

### Default Minecraft Directories

- **Windows**: `%APPDATA%/.minecraft`
- **macOS**: `~/Library/Application Support/minecraft`
- **Linux**: `~/.minecraft`

## What the Installer Does

1. **Profile Creation**: Adds a "MeldMC" profile to your `launcher_profiles.json`
2. **Version Setup**: Creates the appropriate version directory structure
3. **Client Download**: Downloads the platform-specific client JSON configuration

## Architecture

The installer is built with the following lightweight components:

- **Version Fetcher**: Downloads and parses Maven metadata XML to get available versions
- **GUI Framework**: FLTK-based interface for minimal size and dependencies
- **OS Detection**: Automatic platform detection for directory paths and client downloads
- **Profile Manager**: Handles Minecraft launcher profile integration
- **Error Handling**: Comprehensive error reporting and graceful fallbacks

## Development

The codebase follows modern C++ practices with:
- FLTK for lightweight GUI and cross-platform support
- libcurl for HTTP requests
- nlohmann/json for JSON processing
- tinyxml2 for XML parsing
- CMake for cross-platform building

### Binary Size & Dependencies

- **Size**: ~2MB stripped binary
- **Dependencies**: Standard system libraries (libfltk, libcurl, etc.)
- **Portability**: Can be distributed as a single executable file

### Mock Data

When the MeldMC repository is not available, the installer falls back to mock data for testing purposes. This ensures the application can be tested even when the backend infrastructure is not yet deployed.

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.