package gui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"

	"github.com/Coosanta17/MeldMC-Installer/internal/installer"
	"github.com/Coosanta17/MeldMC-Installer/internal/types"
)

// MainWindow represents the main application window
type MainWindow struct {
	app    fyne.App
	window fyne.Window
	
	// Widgets
	versionTypeSelect *widget.Select
	versionSelect     *widget.Select
	minecraftDirEntry *widget.Entry
	browseButton      *widget.Button
	installButton     *widget.Button
	progressBar       *widget.ProgressBar
	statusLabel       *widget.Label
	
	// Data
	installer *installer.Installer
	releases  []types.Version
	snapshots []types.Version
	versionsLoaded bool
}

// NewMainWindow creates and initializes the main window
func NewMainWindow() *MainWindow {
	a := app.New()
	a.SetIcon(nil) // TODO: Add application icon
	
	w := a.NewWindow("MeldMC Installer")
	w.Resize(fyne.NewSize(500, 400))
	w.SetFixedSize(true)
	
	mw := &MainWindow{
		app:       a,
		window:    w,
		installer: installer.New(),
	}
	
	mw.setupUI()
	mw.loadVersionsAsync()
	
	return mw
}

// setupUI creates and arranges the user interface
func (mw *MainWindow) setupUI() {
	// Version type selector
	mw.versionTypeSelect = widget.NewSelect([]string{"Release", "Snapshot"}, mw.onVersionTypeChanged)
	mw.versionTypeSelect.SetSelected("Release")
	mw.versionTypeSelect.Disable()
	
	// Version selector
	mw.versionSelect = widget.NewSelect([]string{}, nil)
	mw.versionSelect.Disable()
	
	// Minecraft directory
	mw.minecraftDirEntry = widget.NewEntry()
	mw.minecraftDirEntry.SetText(installer.GetDefaultMinecraftDir())
	
	mw.browseButton = widget.NewButton("Browse...", mw.onBrowseClicked)
	
	// Install button
	mw.installButton = widget.NewButton("Install", mw.onInstallClicked)
	mw.installButton.Disable()
	
	// Progress bar
	mw.progressBar = widget.NewProgressBar()
	mw.progressBar.SetValue(0)
	
	// Status label
	mw.statusLabel = widget.NewLabel("Loading versions...")
	mw.statusLabel.Wrapping = fyne.TextWrapWord
	
	// Layout
	versionTypeRow := container.NewBorder(nil, nil, 
		widget.NewLabel("Version Type:"), nil,
		mw.versionTypeSelect)
	
	versionRow := container.NewBorder(nil, nil,
		widget.NewLabel("Version:"), nil,
		mw.versionSelect)
	
	minecraftDirRow := container.NewBorder(nil, nil,
		widget.NewLabel("Minecraft Dir:"), mw.browseButton,
		mw.minecraftDirEntry)
	
	installRow := container.NewHBox(
		widget.NewLabel(""), // Spacer
		mw.installButton,
		widget.NewLabel(""), // Spacer
	)
	
	content := container.NewVBox(
		widget.NewCard("", "", container.NewVBox(
			versionTypeRow,
			widget.NewSeparator(),
			versionRow,
			widget.NewSeparator(),
			minecraftDirRow,
			widget.NewSeparator(),
			installRow,
			widget.NewSeparator(),
			mw.progressBar,
			widget.NewSeparator(),
			mw.statusLabel,
		)),
	)
	
	mw.window.SetContent(container.NewPadded(content))
}

// onVersionTypeChanged handles version type selection changes
func (mw *MainWindow) onVersionTypeChanged(selected string) {
	mw.updateVersionSelect()
}

// updateVersionSelect updates the version selector based on the selected type
func (mw *MainWindow) updateVersionSelect() {
	if !mw.versionsLoaded {
		return
	}
	
	var versions []types.Version
	if mw.versionTypeSelect.Selected == "Release" {
		versions = mw.releases
	} else {
		versions = mw.snapshots
	}
	
	var versionStrings []string
	for _, v := range versions {
		versionStrings = append(versionStrings, v.Version)
	}
	
	mw.versionSelect.Options = versionStrings
	if len(versionStrings) > 0 {
		mw.versionSelect.SetSelected(versionStrings[0])
	}
	mw.versionSelect.Refresh()
}

// onBrowseClicked handles the browse button click
func (mw *MainWindow) onBrowseClicked() {
	dialog.ShowFolderOpen(func(folder fyne.ListableURI, err error) {
		if err == nil && folder != nil {
			mw.minecraftDirEntry.SetText(folder.Path())
		}
	}, mw.window)
}

// onInstallClicked handles the install button click
func (mw *MainWindow) onInstallClicked() {
	if mw.versionSelect.Selected == "" {
		dialog.ShowError(fmt.Errorf("please select a version"), mw.window)
		return
	}
	
	if mw.minecraftDirEntry.Text == "" {
		dialog.ShowError(fmt.Errorf("please select a Minecraft directory"), mw.window)
		return
	}
	
	// Find selected version
	var selectedVersion types.Version
	var versions []types.Version
	if mw.versionTypeSelect.Selected == "Release" {
		versions = mw.releases
	} else {
		versions = mw.snapshots
	}
	
	for _, v := range versions {
		if v.Version == mw.versionSelect.Selected {
			selectedVersion = v
			break
		}
	}
	
	// Disable UI during installation
	mw.installButton.Disable()
	mw.progressBar.SetValue(0)
	
	// Start installation
	progressCh := make(chan types.InstallProgress, 10)
	go func() {
		err := mw.installer.InstallVersion(selectedVersion, mw.minecraftDirEntry.Text, progressCh)
		if err != nil {
			// Error will be shown via progress channel
		}
	}()
	
	// Monitor progress
	go mw.monitorInstallProgress(progressCh, selectedVersion.Version)
}

// monitorInstallProgress monitors the installation progress
func (mw *MainWindow) monitorInstallProgress(progressCh <-chan types.InstallProgress, version string) {
	for progress := range progressCh {
		if progress.ErrorString != "" {
			mw.statusLabel.SetText("Installation failed")
			mw.progressBar.SetValue(0)
			mw.installButton.Enable()
			dialog.ShowError(fmt.Errorf(progress.ErrorString), mw.window)
			return
		}
		
		mw.statusLabel.SetText(progress.Step)
		mw.progressBar.SetValue(progress.Progress / 100.0)
		
		if progress.Progress >= 100 {
			// Installation complete
			mw.installButton.Enable()
			dialog.ShowInformation("Installation Complete", 
				fmt.Sprintf("MeldMC %s has been installed successfully!\n\nYou can now select the MeldMC profile in your Minecraft launcher.", version), 
				mw.window)
		}
	}
}

// loadVersionsAsync loads versions in a background goroutine
func (mw *MainWindow) loadVersionsAsync() {
	go func() {
		releases, snapshots, err := mw.installer.LoadVersions()
		
		// Update UI on main thread
		if err != nil {
			mw.statusLabel.SetText("Error: Failed to load versions from repository")
			dialog.ShowError(fmt.Errorf("could not connect to the MeldMC repository.\n\nPlease check your internet connection and try again.\nIf the problem persists, the repository may be temporarily unavailable"), mw.window)
			return
		}
		
		mw.releases = releases
		mw.snapshots = snapshots
		mw.versionsLoaded = true
		
		// Enable UI
		mw.statusLabel.SetText("Versions loaded successfully")
		mw.versionTypeSelect.Enable()
		mw.versionSelect.Enable()
		mw.installButton.Enable()
		
		// Update version selector
		mw.updateVersionSelect()
	}()
}

// Run starts the application
func (mw *MainWindow) Run() {
	mw.window.ShowAndRun()
}

// Close closes the application
func (mw *MainWindow) Close() {
	mw.app.Quit()
}