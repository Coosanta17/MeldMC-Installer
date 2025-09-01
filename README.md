# MeldMC Installer

Cross-platform GUI installer for MeldMC Minecraft instances. Available in multiple implementations:

- **Java Version** (`java/`): Swing-based installer using Java 8+ with fat JAR distribution
- **Go Version** (root): Fyne-based installer with static linking
- **C++ Version** (`legacy/`): Original FLTK-based implementation (reference/legacy)

All versions provide identical functionality for the official Minecraft Launcher (or any launcher that imitates it).

## Implementations

### Java Version (Recommended)

Cross-platform Swing application that packages as a self-contained fat JAR.

**Requirements:**
- Java 8 or higher
- Maven for building

**Building:**
```bash
cd java
mvn clean package
java -jar target/meldmc-installer.jar
```

### Go Version

**Requirements:**
- Go 1.21 or newer
- System dependencies for GUI (platform-specific):
  - **Linux**: `libgl1-mesa-dev xorg-dev`
  - **Windows**: No additional dependencies
  - **macOS**: No additional dependencies

## Building (Go Version)

### Prerequisites

- Go 1.21 or newer
- System dependencies for GUI (platform-specific):
  - **Linux**: `libgl1-mesa-dev xorg-dev`
  - **Windows**: No additional dependencies
  - **macOS**: No additional dependencies

### Steps

**Install system dependencies (Linux only):**
```bash
sudo apt-get update && sudo apt-get install -y libgl1-mesa-dev xorg-dev
```

**Build the application:**

*All platforms:*
```bash
go mod download
go build -o meldmc-installer .
```

*For static builds:*

*Linux:*
```bash
CGO_ENABLED=1 go build -ldflags="-s -w -extldflags=-static" -a -installsuffix cgo -o meldmc-installer .
```

*Windows:*
```bash
go build -ldflags="-s -w -H windowsgui" -o meldmc-installer.exe .
```

*macOS:*
```bash
CGO_ENABLED=1 go build -ldflags="-s -w" -o meldmc-installer .
```

The resulting binary is self-contained and can be distributed as a single file.

## Usage

Simply double-click the `meldmc-installer` executable (or `meldmc-installer.exe` on Windows).

## What the Installer Does

1. Fetch available versions from repo
2. Adds a "MeldMC" profile to `launcher_profiles.json`
3. Creates the version folder in `./versions/{version}`
4. Downloads the platform-specific client JSON configuration from https://repo.coosanta.net

## Architecture

The application is built with:
- **Go**: Core language for robust, cross-platform development
- **Fyne**: Modern, native GUI framework
- **Standard library**: HTTP client, JSON/XML parsing, file operations
- **Static linking**: Self-contained binaries with no external dependencies

## License

This project is part of the MeldMC ecosystem. Please refer to the main MeldMC project for licensing information.
