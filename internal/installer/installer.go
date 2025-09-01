package installer

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"time"

	"github.com/Coosanta17/MeldMC-Installer/internal/http"
	"github.com/Coosanta17/MeldMC-Installer/internal/types"
)

// Installer handles the installation process
type Installer struct {
	client *http.Client
}

// New creates a new installer instance
func New() *Installer {
	return &Installer{
		client: http.NewClient(),
	}
}

// GetDefaultMinecraftDir returns the default Minecraft directory for the current OS
func GetDefaultMinecraftDir() string {
	switch runtime.GOOS {
	case "windows":
		if appdata := os.Getenv("APPDATA"); appdata != "" {
			return filepath.Join(appdata, ".minecraft")
		}
	case "darwin":
		if home := os.Getenv("HOME"); home != "" {
			return filepath.Join(home, "Library", "Application Support", "minecraft")
		}
	default: // Linux and others
		if home := os.Getenv("HOME"); home != "" {
			return filepath.Join(home, ".minecraft")
		}
	}
	return ""
}

// LoadVersions loads available versions from the repository
func (i *Installer) LoadVersions() (releases, snapshots []types.Version, err error) {
	// Load releases
	releases, err = i.client.GetVersions(false)
	if err != nil {
		// Don't fail completely if one fails
		releases = []types.Version{}
	}

	// Load snapshots
	snapshots, err2 := i.client.GetVersions(true)
	if err2 != nil {
		snapshots = []types.Version{}
	}

	// Return error only if both failed
	if len(releases) == 0 && len(snapshots) == 0 {
		if err != nil {
			return nil, nil, fmt.Errorf("failed to load any versions: %w", err)
		}
		return nil, nil, fmt.Errorf("no versions available")
	}

	return releases, snapshots, nil
}

// InstallVersion installs the specified version
func (i *Installer) InstallVersion(version types.Version, minecraftDir string, progressCh chan<- types.InstallProgress) error {
	defer close(progressCh)

	// Validate inputs
	if minecraftDir == "" {
		progressCh <- types.InstallProgress{ErrorString: "Please select a Minecraft directory"}
		return fmt.Errorf("minecraft directory not specified")
	}

	// Create version directory
	progressCh <- types.InstallProgress{Step: "Creating directories...", Progress: 10}
	versionDir := filepath.Join(minecraftDir, "versions", "meldmc-"+version.Version)
	if err := os.MkdirAll(versionDir, 0755); err != nil {
		progressCh <- types.InstallProgress{ErrorString: "Failed to create version directory"}
		return fmt.Errorf("failed to create version directory: %w", err)
	}

	// Download client configuration
	progressCh <- types.InstallProgress{Step: "Downloading client configuration...", Progress: 50}
	clientURL := i.client.BuildClientURL(version.Version)
	clientData, err := i.client.DownloadFile(clientURL)
	if err != nil {
		progressCh <- types.InstallProgress{ErrorString: "Failed to download client configuration"}
		return fmt.Errorf("failed to download client configuration: %w", err)
	}

	// Save client configuration
	clientPath := filepath.Join(versionDir, "meldmc-"+version.Version+".json")
	if err := os.WriteFile(clientPath, clientData, 0644); err != nil {
		progressCh <- types.InstallProgress{ErrorString: "Failed to save client configuration"}
		return fmt.Errorf("failed to save client configuration: %w", err)
	}

	// Create/update launcher profile
	progressCh <- types.InstallProgress{Step: "Creating launcher profile...", Progress: 90}
	if err := i.createProfile(minecraftDir, version.Version); err != nil {
		progressCh <- types.InstallProgress{ErrorString: "Failed to create launcher profile"}
		return fmt.Errorf("failed to create launcher profile: %w", err)
	}

	// Installation complete
	progressCh <- types.InstallProgress{Step: "Installation complete!", Progress: 100}
	return nil
}

// createProfile creates or updates the Minecraft launcher profile
func (i *Installer) createProfile(minecraftDir, version string) error {
	profilesPath := filepath.Join(minecraftDir, "launcher_profiles.json")
	
	// Load existing profiles or create new structure
	var profiles types.LauncherProfiles
	if data, err := os.ReadFile(profilesPath); err == nil {
		if err := json.Unmarshal(data, &profiles); err != nil {
			// If parsing fails, start with empty profiles
			profiles = types.LauncherProfiles{Profiles: make(map[string]types.LauncherProfile)}
		}
	} else {
		profiles = types.LauncherProfiles{Profiles: make(map[string]types.LauncherProfile)}
	}

	// Ensure profiles map exists
	if profiles.Profiles == nil {
		profiles.Profiles = make(map[string]types.LauncherProfile)
	}

	// Create profile entry
	profileName := "MeldMC " + version
	epoch := time.Unix(0, 0)
	
	profile := types.LauncherProfile{
		Name:          profileName,
		Type:          "custom",
		Created:       epoch,
		LastUsed:      epoch,
		Icon:          "Grass", // TODO: change icon
		LastVersionID: "meldmc-" + version,
	}

	profiles.Profiles[profileName] = profile

	// Save profiles
	data, err := json.MarshalIndent(profiles, "", "  ")
	if err != nil {
		return fmt.Errorf("failed to marshal profiles: %w", err)
	}

	if err := os.WriteFile(profilesPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write profiles: %w", err)
	}

	return nil
}