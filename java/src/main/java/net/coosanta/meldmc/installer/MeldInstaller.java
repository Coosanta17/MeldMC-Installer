package net.coosanta.meldmc.installer;

import com.google.gson.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MeldMC Installer main application using Swing
 */
public class MeldInstaller extends JFrame {
    private static final String WINDOW_TITLE = "MeldMC Installer";
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 400;

    private JComboBox<String> versionTypeChoice;
    private JComboBox<Version> versionChoice;
    private JTextField minecraftDirInput;
    private JButton browseButton;
    private JButton installButton;
    private JProgressBar progressBar;
    private JTextArea statusLabel;

    private List<Version> releases = new ArrayList<>();
    private List<Version> snapshots = new ArrayList<>();
    private boolean versionsLoaded = false;

    public MeldInstaller() {
        initializeUI();
        setDefaultMinecraftDir();
        loadVersionsAsync();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MeldInstaller().setVisible(true));
    }

    private void initializeUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        setTitle(WINDOW_TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        setLayout(null);

        JLabel versionTypeLabel = new JLabel("Version Type:");
        versionTypeLabel.setBounds(20, 20, 100, 25);
        add(versionTypeLabel);

        versionTypeChoice = new JComboBox<>(new String[]{"Release", "Snapshot"});
        versionTypeChoice.setBounds(130, 20, 150, 25);
        versionTypeChoice.setEnabled(false);
        versionTypeChoice.addActionListener(e -> updateVersionChoice());
        add(versionTypeChoice);

        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setBounds(20, 60, 100, 25);
        add(versionLabel);

        versionChoice = new JComboBox<>();
        versionChoice.setBounds(130, 60, 200, 25);
        versionChoice.setEnabled(false);
        add(versionChoice);

        JLabel minecraftDirLabel = new JLabel("Minecraft Dir:");
        minecraftDirLabel.setBounds(20, 100, 100, 25);
        add(minecraftDirLabel);

        minecraftDirInput = new JTextField();
        minecraftDirInput.setBounds(130, 100, 250, 25);
        add(minecraftDirInput);

        browseButton = new JButton("Browse...");
        browseButton.setBounds(390, 100, 80, 25);
        browseButton.addActionListener(e -> browseForDirectory());
        add(browseButton);

        installButton = new JButton("Install");
        installButton.setBounds(200, 160, 100, 30);
        installButton.setEnabled(false);
        installButton.addActionListener(e -> performInstall());
        add(installButton);

        progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(20, 220, 460, 20);
        progressBar.setValue(0);
        add(progressBar);

        statusLabel = new JTextArea("Loading versions...");
        statusLabel.setBounds(20, 250, 460, 120);
        statusLabel.setEditable(false);
        statusLabel.setOpaque(false);
        statusLabel.setLineWrap(true);
        statusLabel.setWrapStyleWord(true);
        add(statusLabel);
    }

    private void setDefaultMinecraftDir() {
        String defaultDir = "";
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null) {
                defaultDir = appdata + "\\.minecraft";
            }
        } else if (os.contains("mac")) {
            defaultDir = userHome + "/Library/Application Support/minecraft";
        } else {
            // Linux and others
            defaultDir = userHome + "/.minecraft";
        }

        if (!defaultDir.isEmpty()) {
            minecraftDirInput.setText(defaultDir);
        }
    }

    private void loadVersionsAsync() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            protected Void doInBackground() {
                publish("Loading versions...");

                List<Version> tempReleases = new ArrayList<>();
                try {
                    String releasesXML = httpGet("https://repo.coosanta.net/releases/net/coosanta/meldmc/maven-metadata.xml");
                    if (!releasesXML.isEmpty()) {
                        tempReleases = parseVersionsFromXML(releasesXML, false);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load releases: " + e.getMessage());
                }

                List<Version> tempSnapshots = new ArrayList<>();
                try {
                    String snapshotsXML = httpGet("https://repo.coosanta.net/snapshots/net/coosanta/meldmc/maven-metadata.xml");
                    if (!snapshotsXML.isEmpty()) {
                        tempSnapshots = parseVersionsFromXML(snapshotsXML, true);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load snapshots: " + e.getMessage());
                }

                releases = tempReleases;
                snapshots = tempSnapshots;
                versionsLoaded = true;

                return null;
            }

            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    statusLabel.setText(message);
                }
            }

            protected void done() {
                onVersionsLoaded();
            }
        };

        worker.execute();
    }

    private void onVersionsLoaded() {
        if (releases.isEmpty() && snapshots.isEmpty()) {
            statusLabel.setText("Error: Failed to load versions from repository");
            JOptionPane.showMessageDialog(this,
                    "Error: Could not connect to the MeldMC repository.\n\n" +
                    "Please check your internet connection and try again.\n" +
                    "If the problem persists, the repository may be temporarily unavailable.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            installButton.setEnabled(false);
        } else {
            statusLabel.setText("Versions loaded successfully");
            versionTypeChoice.setEnabled(true);
            versionChoice.setEnabled(true);
            installButton.setEnabled(true);
        }

        updateVersionChoice();
    }

    private void updateVersionChoice() {
        versionChoice.removeAllItems();

        List<Version> currentVersions = (versionTypeChoice.getSelectedIndex() == 0) ? releases : snapshots;

        for (Version version : currentVersions) {
            versionChoice.addItem(version);
        }

        if (!currentVersions.isEmpty()) {
            versionChoice.setSelectedIndex(0);
        }
    }

    private void browseForDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Minecraft Directory");

        String currentDir = minecraftDirInput.getText();
        if (!currentDir.isEmpty()) {
            chooser.setCurrentDirectory(new File(currentDir));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            minecraftDirInput.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void performInstall() {
        String minecraftDir = minecraftDirInput.getText().trim();
        if (minecraftDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Minecraft directory", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Version selectedVersion = (Version) versionChoice.getSelectedItem();
        if (selectedVersion == null) {
            JOptionPane.showMessageDialog(this, "Please select a version", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable UI during installation
        installButton.setEnabled(false);
        versionTypeChoice.setEnabled(false);
        versionChoice.setEnabled(false);

        SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
            protected Boolean doInBackground() throws Exception {
                publish(10);

                String version = selectedVersion.getVersion();
                String osString = getOSString();

                // Update status
                SwingUtilities.invokeLater(() -> statusLabel.setText("Installing MeldMC..."));

                // Create directories
                Path versionDir = Paths.get(minecraftDir, "versions", "meldmc-" + version);
                try {
                    Files.createDirectories(versionDir);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MeldInstaller.this, "Failed to create version directory", "Error", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Installation failed");
                    });
                    return false;
                }

                publish(30);

                // Download client JSON
                String clientUrl = "https://repo.coosanta.net/releases/net/coosanta/meldmc/" + version + "/meldmc-" +
                                   version + "-client-" + osString + ".json";
                Path clientPath = versionDir.resolve("meldmc-" + version + ".json");

                SwingUtilities.invokeLater(() -> statusLabel.setText("Downloading client configuration..."));
                publish(50);

                try {
                    if (!downloadFile(clientUrl, clientPath.toString())) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MeldInstaller.this, "Failed to download client configuration from repository", "Error", JOptionPane.ERROR_MESSAGE);
                            statusLabel.setText("Installation failed");
                        });
                        return false;
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MeldInstaller.this, "Failed to download client configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Installation failed");
                    });
                    return false;
                }

                publish(70);

                // Create or update launcher profile
                SwingUtilities.invokeLater(() -> statusLabel.setText("Creating launcher profile..."));
                publish(90);

                try {
                    if (!createProfile(minecraftDir, version)) {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                JOptionPane.showMessageDialog(MeldInstaller.this, "Failed to create launcher profile", "Error", JOptionPane.ERROR_MESSAGE);
                                statusLabel.setText("Installation failed");
                            }
                        });
                        return false;
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MeldInstaller.this, "Failed to create launcher profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Installation failed");
                    });
                    return false;
                }

                publish(100);
                SwingUtilities.invokeLater(() -> statusLabel.setText("MeldMC installed successfully!"));

                return true;
            }

            protected void process(List<Integer> chunks) {
                for (Integer progress : chunks) {
                    progressBar.setValue(progress);
                }
            }

            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        Version selectedVersion = (Version) versionChoice.getSelectedItem();
                        JOptionPane.showMessageDialog(MeldInstaller.this,
                                "MeldMC " + selectedVersion.getVersion() + " has been installed successfully!\n\n" +
                                "You can now select the MeldMC profile in your Minecraft launcher.",
                                "Installation Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        progressBar.setValue(0);
                    }
                } catch (Exception e) {
                    progressBar.setValue(0);
                    statusLabel.setText("Installation failed");
                    JOptionPane.showMessageDialog(MeldInstaller.this, "Installation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }

                // Re-enable UI
                installButton.setEnabled(true);
                versionTypeChoice.setEnabled(true);
                versionChoice.setEnabled(true);
            }
        };

        worker.execute();
    }

    private String getOSString() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("win")) {
            return "win";
        } else if (os.contains("mac")) {
            if (arch.contains("aarch64") || arch.contains("arm")) {
                return "mac-aarch64";
            } else {
                return "mac";
            }
        } else {
            return "linux";
        }
    }

    private String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
            return response.toString();
        }
    }

    private List<Version> parseVersionsFromXML(String xmlData, boolean isSnapshot) throws Exception {
        List<Version> versions = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));

        Element metadata = doc.getDocumentElement();
        if (!"metadata".equals(metadata.getNodeName())) {
            return versions;
        }

        NodeList versioningList = metadata.getElementsByTagName("versioning");
        if (versioningList.getLength() == 0) {
            return versions;
        }

        Element versioning = (Element) versioningList.item(0);
        NodeList versionsList = versioning.getElementsByTagName("versions");
        if (versionsList.getLength() == 0) {
            return versions;
        }

        Element versionsElement = (Element) versionsList.item(0);
        NodeList versionNodes = versionsElement.getElementsByTagName("version");

        for (int i = 0; i < versionNodes.getLength(); i++) {
            Element versionElement = (Element) versionNodes.item(i);
            String versionText = versionElement.getTextContent();
            if (versionText != null && !versionText.trim().isEmpty()) {
                versions.add(new Version(versionText.trim(), isSnapshot));
            }
        }

        // Sort versions (latest first)
        versions.sort(Collections.reverseOrder());

        return versions;
    }

    private boolean downloadFile(String urlString, String filePath) throws Exception {
        String content = httpGet(urlString);
        if (content.isEmpty()) {
            return false;
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
            return true;
        }
    }

    private boolean createProfile(String minecraftDir, String version) throws Exception {
        Path profilesPath = Paths.get(minecraftDir, "launcher_profiles.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject profiles;

        // Load existing profiles if they exist
        if (Files.exists(profilesPath)) {
            try {
                String existingContent = new String(Files.readAllBytes(profilesPath), StandardCharsets.UTF_8);
                JsonElement existingProfiles = JsonParser.parseString(existingContent);
                profiles = existingProfiles.getAsJsonObject();
            } catch (Exception e) {
                // If parsing fails, start with empty profiles
                profiles = new JsonObject();
            }
        } else {
            profiles = new JsonObject();
        }

        // Ensure basic structure exists
        if (!profiles.has("profiles")) {
            profiles.add("profiles", new JsonObject());
        }

        // Create MeldMC profile
        String profileName = "MeldMC " + version;
        JsonObject profilesNode = profiles.getAsJsonObject("profiles");
        JsonObject profile = new JsonObject();

        profile.addProperty("name", profileName);
        profile.addProperty("type", "custom");
        profile.addProperty("created", "1970-01-01T00:00:00.000Z");
        profile.addProperty("lastUsed", "1970-01-01T00:00:00.000Z");
        profile.addProperty("icon", "Grass");
        profile.addProperty("lastVersionId", "meldmc-" + version);

        profilesNode.add(profileName, profile);

        // Save profiles
        try (FileWriter writer = new FileWriter(profilesPath.toFile())) {
            gson.toJson(profiles, writer);
            return true;
        }
    }
}
