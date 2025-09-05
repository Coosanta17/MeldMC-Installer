#include <FL/Fl.H>
#include <FL/Fl_Window.H>
#include <FL/Fl_Button.H>
#include <FL/Fl_Choice.H>
#include <FL/Fl_Input.H>
#include <FL/Fl_Box.H>
#include <FL/Fl_Progress.H>
#include <FL/fl_ask.H>
#include <FL/Fl_File_Chooser.H>

#include <curl/curl.h>
#include <nlohmann/json.hpp>
#include <tinyxml2.h>

#include <iostream>
#include <string>
#include <utility>
#include <vector>
#include <filesystem>
#include <fstream>
#include <regex>
#include <thread>

// Helper structure for CURL write callback
struct WriteCallback {
    std::string data;

    static size_t writeFunction(void *contents, const size_t size, const size_t nmemb, WriteCallback *userp) {
        const size_t realsize = size * nmemb;
        userp->data.append(static_cast<char *>(contents), realsize);
        return realsize;
    }
};

// Version structure
struct Version {
    std::string version;
    bool isSnapshot;

    Version(std::string ver, const bool snap) : version(std::move(ver)), isSnapshot(snap) {
    }

    bool operator<(const Version &other) const {
        return version < other.version;
    }
};

class MeldInstaller {
    Fl_Window *window;
    Fl_Choice *versionTypeChoice;
    Fl_Choice *versionChoice;
    Fl_Input *minecraftDirInput;
    Fl_Button *browseButton;
    Fl_Button *installButton;
    Fl_Progress *progressBar;
    Fl_Box *statusLabel;

    std::vector<Version> releases;
    std::vector<Version> snapshots;
    std::thread loadingThread;
    bool versionsLoaded = false;

public:
    MeldInstaller() {
        // Create main window
        window = new Fl_Window(500, 400, "MeldMC Installer");

        // Version type selector
        new Fl_Box(20, 20, 100, 25, "Version Type:");
        versionTypeChoice = new Fl_Choice(130, 20, 150, 25);
        versionTypeChoice->add("Release");
        versionTypeChoice->add("Snapshot");
        versionTypeChoice->value(0);
        versionTypeChoice->callback(versionTypeChangedCB, this);

        // Version selector
        new Fl_Box(20, 60, 100, 25, "Version:");
        versionChoice = new Fl_Choice(130, 60, 200, 25);

        // Deactivate choices until versions are loaded to avoid menu update races
        versionTypeChoice->deactivate();
        versionChoice->deactivate();

        // Minecraft directory
        new Fl_Box(20, 100, 100, 25, "Minecraft Dir:");
        minecraftDirInput = new Fl_Input(130, 100, 250, 25);
        browseButton = new Fl_Button(390, 100, 80, 25, "Browse...");
        browseButton->callback(browseCB, this);

        // Install button (start disabled)
        installButton = new Fl_Button(200, 160, 100, 30, "Install");
        installButton->callback(installCB, this);
        installButton->deactivate();

        // Progress bar
        progressBar = new Fl_Progress(20, 220, 460, 20);
        progressBar->minimum(0);
        progressBar->maximum(100);
        progressBar->value(0);

        // Status label
        statusLabel = new Fl_Box(20, 250, 460, 120, "Loading versions...");
        statusLabel->align(FL_ALIGN_TOP_LEFT | FL_ALIGN_INSIDE | FL_ALIGN_WRAP);

        window->end();

        // Initialize
        setDefaultMinecraftDir();

        // Show window immediately
        window->show();

        // Start loading versions in background thread
        loadingThread = std::thread(&MeldInstaller::loadVersionsThreaded, this);
        loadingThread.detach();
    }

    void setDefaultMinecraftDir() const {
        std::string defaultDir;

#ifdef _WIN32
        if (const char *appdata = std::getenv("APPDATA")) {
            defaultDir = std::string(appdata) + "\\.minecraft";
        }
#elif __APPLE__
        const char* home = std::getenv("HOME");
        if (home) {
            defaultDir = std::string(home) + "/Library/Application Support/minecraft";
        }
#else
        const char* home = std::getenv("HOME");
        if (home) {
            defaultDir = std::string(home) + "/.minecraft";
        }
#endif

        if (!defaultDir.empty()) {
            minecraftDirInput->value(defaultDir.c_str());
        }
    }

    static std::string httpGet(const std::string &url) {
        CURL *curl = curl_easy_init();
        if (!curl) return "";

        WriteCallback writeCallback;

        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback::writeFunction);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &writeCallback);
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);

        const CURLcode res = curl_easy_perform(curl);
        curl_easy_cleanup(curl);

        return (res == CURLE_OK) ? writeCallback.data : "";
    }

    static std::vector<Version> parseVersionsFromXML(const std::string &xmlData, bool isSnapshot) {
        std::vector<Version> versions;

        tinyxml2::XMLDocument doc;
        if (doc.Parse(xmlData.c_str()) != tinyxml2::XML_SUCCESS) {
            return versions;
        }

        const auto metadata = doc.FirstChildElement("metadata");
        if (!metadata) return versions;

        const auto versioning = metadata->FirstChildElement("versioning");
        if (!versioning) return versions;

        const auto versionsList = versioning->FirstChildElement("versions");
        if (!versionsList) return versions;

        for (auto version = versionsList->FirstChildElement("version");
             version; version = version->NextSiblingElement("version")) {
            if (const char *versionText = version->GetText()) {
                versions.emplace_back(versionText, isSnapshot);
            }
        }

        // Sort versions (latest first)
        std::sort(versions.rbegin(), versions.rend());

        return versions;
    }

    void loadVersionsThreaded() {
        // Load releases
        std::vector<Version> tempReleases;
        if (const std::string releasesXML = httpGet(
            "https://repo.coosanta.net/releases/net/coosanta/meldmc/maven-metadata.xml"); !releasesXML.empty()) {
            tempReleases = parseVersionsFromXML(releasesXML, false);
        }

        // Load snapshots
        std::vector<Version> tempSnapshots;
        if (const std::string snapshotsXML = httpGet(
            "https://repo.coosanta.net/snapshots/net/coosanta/meldmc/maven-metadata.xml"); !snapshotsXML.empty()) {
            tempSnapshots = parseVersionsFromXML(snapshotsXML, true);
        }

        // Store temporary results for the callback
        this->tempReleases = std::move(tempReleases);
        this->tempSnapshots = std::move(tempSnapshots);

        // Update UI in main thread
        Fl::awake([](void *data) {
            auto *installer = static_cast<MeldInstaller *>(data);
            installer->onVersionsLoaded(std::move(installer->tempReleases), std::move(installer->tempSnapshots));
        }, this);
    }

    void onVersionsLoaded(std::vector<Version> loadedReleases, std::vector<Version> loadedSnapshots) {
        releases = std::move(loadedReleases);
        snapshots = std::move(loadedSnapshots);
        versionsLoaded = true;

        // Show error if no versions could be loaded
        if (releases.empty() && snapshots.empty()) {
            statusLabel->label("Error: Failed to load versions from repository");
            fl_alert("Error: Could not connect to the MeldMC repository.\n\n"
                "Please check your internet connection and try again.\n"
                "If the problem persists, the repository may be temporarily unavailable.");
            installButton->deactivate();
        } else {
            statusLabel->label("Versions loaded successfully");
            versionTypeChoice->activate();
            versionChoice->activate();
            installButton->activate();
            installButton->activate();
        }

        updateVersionChoice();
    }

    void updateVersionChoice() const {
        versionChoice->clear();

        const std::vector<Version> &currentVersions =
                (versionTypeChoice->value() == 0) ? releases : snapshots;

        for (const auto &version: currentVersions) {
            versionChoice->add(version.version.c_str());
        }

        if (!currentVersions.empty()) {
            versionChoice->value(0);
        }
    }

    static std::string getOSString() {
#ifdef _WIN32
        return "win";
#elif __APPLE__
#ifdef __aarch64__
        return "mac-aarch64";
#else
        return "mac";
#endif
#else
        return "linux";
#endif
    }

    static bool downloadFile(const std::string &url, const std::string &filePath) {
        const std::string content = httpGet(url);
        if (content.empty()) return false;

        std::ofstream file(filePath);
        if (!file.is_open()) return false;

        file << content;
        return true;
    }

    static bool createProfile(const std::string &minecraftDir, const std::string &version) {
        std::string profilesPath = minecraftDir + "/launcher_profiles.json";

        nlohmann::json profiles;

        // Load existing profiles if they exist
        if (std::filesystem::exists(profilesPath)) {
            if (std::ifstream file(profilesPath); file.is_open()) {
                try {
                    file >> profiles;
                } catch (...) {
                    // If parsing fails, start with empty profiles
                    profiles = nlohmann::json::object();
                }
            }
        }

        // Ensure basic structure exists
        if (!profiles.contains("profiles")) {
            profiles["profiles"] = nlohmann::json::object();
        }

        // Create MeldMC profile
        std::string profileName = "MeldMC " + version;
        auto &profile = profiles["profiles"][profileName];

        profile["name"] = profileName;
        profile["type"] = "custom";
        profile["created"] = "1970-01-01T00:00:00.000Z";
        profile["lastUsed"] = "1970-01-01T00:00:00.000Z";
        profile["icon"] = "Grass"; // TODO change icon
        profile["lastVersionId"] = "meldmc-" + version;

        // Save profiles
        std::ofstream file(profilesPath);
        if (!file.is_open()) return false;

        file << profiles.dump(2);
        return true;
    }

    void performInstall() const {
        const std::string minecraftDir = minecraftDirInput->value();
        if (minecraftDir.empty()) {
            fl_alert("Please select a Minecraft directory");
            return;
        }

        const int versionIndex = versionChoice->value();
        if (versionIndex < 0) {
            fl_alert("Please select a version");
            return;
        }

        const std::vector<Version> &currentVersions =
                (versionTypeChoice->value() == 0) ? releases : snapshots;

        if (versionIndex >= currentVersions.size()) {
            fl_alert("Invalid version selected");
            return;
        }

        const std::string version = currentVersions[versionIndex].version;
        const std::string osString = getOSString();

        // Update status
        statusLabel->label("Installing MeldMC...");
        progressBar->value(10);
        Fl::check();

        // Create directories
        const std::string versionDir = minecraftDir + "/versions/meldmc-" + version;
        try {
            std::filesystem::create_directories(versionDir);
        } catch (...) {
            fl_alert("Failed to create version directory");
            progressBar->value(0);
            statusLabel->label("Installation failed");
            return;
        }

        progressBar->value(30);
        Fl::check();

        // Download client JSON
        const bool isSnapshot = versionTypeChoice->value() == 1;
        const std::string baseUrl = isSnapshot
                                        ? "https://repo.coosanta.net/snapshots/net/coosanta/meldmc/"
                                        : "https://repo.coosanta.net/releases/net/coosanta/meldmc/";

        const std::string clientUrl = baseUrl + version + "/meldmc-" +
                                      version + "-client-" + osString + ".json";

        const std::string clientPath = versionDir + "/meldmc-" + version + ".json";

        statusLabel->label("Downloading client configuration...");
        progressBar->value(50);
        Fl::check();

        if (!downloadFile(clientUrl, clientPath)) {
            fl_alert("Failed to download client configuration from repository");
            progressBar->value(0);
            statusLabel->label("Installation failed");
            return;
        }

        progressBar->value(70);
        Fl::check();

        // Create/update launcher profile
        statusLabel->label("Creating launcher profile...");
        progressBar->value(90);
        Fl::check();

        if (!createProfile(minecraftDir, version)) {
            fl_alert("Failed to create launcher profile");
            progressBar->value(0);
            statusLabel->label("Installation failed");
            return;
        }

        progressBar->value(100);
        statusLabel->label("MeldMC installed successfully!");
        fl_message(
            "MeldMC %s has been installed successfully!\n\nYou can now select the MeldMC profile in your Minecraft launcher.",
            version.c_str());
    }

    // Static callbacks
    static void versionTypeChangedCB(Fl_Widget *, void *data) {
        static_cast<MeldInstaller *>(data)->updateVersionChoice();
    }

    static void browseCB(Fl_Widget *, void *data) {
        const auto *installer = static_cast<MeldInstaller *>(data);
        if (const char *dir = fl_dir_chooser("Select Minecraft Directory", installer->minecraftDirInput->value())) {
            installer->minecraftDirInput->value(dir);
        }
    }

    static void installCB(Fl_Widget *, void *data) {
        const auto *installer = static_cast<MeldInstaller *>(data);
        installer->performInstall();
    }

    ~MeldInstaller() {
        if (loadingThread.joinable()) {
            loadingThread.join();
        }
        delete window;
    }

private:
    std::vector<Version> tempReleases;
    std::vector<Version> tempSnapshots;
};

int main() {
    curl_global_init(CURL_GLOBAL_DEFAULT);

    Fl::scheme("basic");

    const MeldInstaller installer;

    const int result = Fl::run();

    curl_global_cleanup();
    return result;
}
