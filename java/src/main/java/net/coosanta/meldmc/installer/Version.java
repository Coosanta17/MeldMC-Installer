package net.coosanta.meldmc.installer;

/**
 * Represents a MeldMC version
 */
public class Version implements Comparable<Version> {
    private final String version;
    private final boolean isSnapshot;

    public Version(String version, boolean isSnapshot) {
        this.version = version;
        this.isSnapshot = isSnapshot;
    }

    public String getVersion() {
        return version;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    @Override
    public int compareTo(Version other) {
        return this.version.compareTo(other.version);
    }

    @Override
    public String toString() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Version version1 = (Version) obj;
        return isSnapshot == version1.isSnapshot && version.equals(version1.version);
    }

    @Override
    public int hashCode() {
        return version.hashCode() * 31 + (isSnapshot ? 1 : 0);
    }
}