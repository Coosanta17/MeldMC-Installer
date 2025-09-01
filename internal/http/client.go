package http

import (
	"encoding/xml"
	"fmt"
	"io"
	"net/http"
	"runtime"
	"sort"
	"time"

	"github.com/Coosanta17/MeldMC-Installer/internal/types"
)

// Client handles HTTP operations for the installer
type Client struct {
	httpClient *http.Client
	config     *types.Config
}

// NewClient creates a new HTTP client
func NewClient() *Client {
	return &Client{
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
		config: &types.Config{
			ReleasesURL:  "https://repo.coosanta.net/releases/net/coosanta/meldmc/maven-metadata.xml",
			SnapshotsURL: "https://repo.coosanta.net/snapshots/net/coosanta/meldmc/maven-metadata.xml",
			RepoBaseURL:  "https://repo.coosanta.net/releases/net/coosanta/meldmc",
		},
	}
}

// Get performs a GET request and returns the response body
func (c *Client) Get(url string) ([]byte, error) {
	resp, err := c.httpClient.Get(url)
	if err != nil {
		return nil, fmt.Errorf("failed to make request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("request failed with status: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	return body, nil
}

// GetVersions fetches and parses versions from the Maven metadata
func (c *Client) GetVersions(isSnapshot bool) ([]types.Version, error) {
	url := c.config.ReleasesURL
	if isSnapshot {
		url = c.config.SnapshotsURL
	}

	data, err := c.Get(url)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch versions: %w", err)
	}

	var metadata types.VersionMetadata
	if err := xml.Unmarshal(data, &metadata); err != nil {
		return nil, fmt.Errorf("failed to parse metadata XML: %w", err)
	}

	var versions []types.Version
	for _, versionStr := range metadata.Versioning.Versions.Versions {
		versions = append(versions, types.Version{
			Version:    versionStr,
			IsSnapshot: isSnapshot,
		})
	}

	// Sort versions (latest first)
	sort.Slice(versions, func(i, j int) bool {
		return versions[i].Version > versions[j].Version
	})

	return versions, nil
}

// DownloadFile downloads a file from the given URL and returns its content
func (c *Client) DownloadFile(url string) ([]byte, error) {
	return c.Get(url)
}

// GetOSString returns the platform-specific OS string for downloads
func GetOSString() string {
	switch runtime.GOOS {
	case "windows":
		return "win"
	case "darwin":
		if runtime.GOARCH == "arm64" {
			return "mac-aarch64"
		}
		return "mac"
	default:
		return "linux"
	}
}

// BuildClientURL builds the URL for downloading client configuration
func (c *Client) BuildClientURL(version string) string {
	osString := GetOSString()
	return fmt.Sprintf("%s/%s/meldmc-%s-client-%s.json", 
		c.config.RepoBaseURL, version, version, osString)
}