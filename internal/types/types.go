package types

import "time"

// Version represents a MeldMC version
type Version struct {
	Version    string `xml:",chardata" json:"version"`
	IsSnapshot bool   `json:"isSnapshot"`
}

// VersionMetadata represents the Maven metadata structure
type VersionMetadata struct {
	GroupID    string `xml:"groupId"`
	ArtifactID string `xml:"artifactId"`
	Versioning struct {
		Latest   string `xml:"latest"`
		Release  string `xml:"release"`
		Versions struct {
			Versions []string `xml:"version"`
		} `xml:"versions"`
	} `xml:"versioning"`
}

// LauncherProfile represents a Minecraft launcher profile
type LauncherProfile struct {
	Name          string    `json:"name"`
	Type          string    `json:"type"`
	Created       time.Time `json:"created"`
	LastUsed      time.Time `json:"lastUsed"`
	Icon          string    `json:"icon"`
	LastVersionID string    `json:"lastVersionId"`
}

// LauncherProfiles represents the launcher_profiles.json structure
type LauncherProfiles struct {
	Profiles map[string]LauncherProfile `json:"profiles"`
}

// InstallProgress represents installation progress
type InstallProgress struct {
	Step        string
	Progress    float64
	ErrorString string
}

// Config holds application configuration
type Config struct {
	ReleasesURL  string
	SnapshotsURL string
	RepoBaseURL  string
}