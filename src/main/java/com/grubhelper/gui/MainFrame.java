// Package declaration for the GUI package of Grub Helper
package com.grubhelper.gui;

// Import GrubConfigParser model for loading and saving GRUB settings
import com.grubhelper.model.GrubConfigParser;
// Import GrubEnvironment model for system path and tool detection
import com.grubhelper.model.GrubEnvironment;
// Import GrubTheme data model representing GRUB themes
import com.grubhelper.model.GrubTheme;
// Import ThemeManager model for discovering and installing themes
import com.grubhelper.model.ThemeManager;
// Import CommandResult utility for process output execution results
import com.grubhelper.util.CommandResult;
// Import RootExecutor utility for executing root shell commands
import com.grubhelper.util.RootExecutor;
// Import UpdateChecker utility for auto-update checks
import com.grubhelper.util.UpdateChecker;
// Import Version utility for storing application version constants
import com.grubhelper.util.Version;

// Import Swing GUI components
import javax.swing.*;
// Import Swing border classes for padding and outlines
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
// Import Swing event listener interfaces for tab switching
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
// Import Swing table renderers and models for Easy Mode table
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
// Import AWT layout, color, and graphics classes
import java.awt.*;
// Import File class for filesystem references
import java.io.File;
// Import IOException for input/output error handling
import java.io.IOException;
// Import LinkedHashMap for preserving configuration key ordering
import java.util.LinkedHashMap;
// Import Map interface for key-value pair operations
import java.util.Map;

/**
 * Main application window (JFrame) for Grub Helper.
 */
public class MainFrame extends JFrame {

    // Define primary blue accent color for buttons and highlights
    private static final Color COLOR_PRIMARY = new Color(41, 128, 185);
    // Define dark charcoal color for the header background
    private static final Color COLOR_DARK_HEADER = new Color(30, 39, 46);
    // Define light grey background color for main content panels
    private static final Color COLOR_BG_LIGHT = new Color(245, 246, 250);
    // Define emerald green color for update success notification buttons
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113);
    // Define muted grey color for secondary text and subtitles
    private static final Color COLOR_TEXT_MUTED = new Color(127, 140, 141);

    // Instance variable for managing GRUB system environment information
    private final GrubEnvironment environment;
    // Instance variable for parsing and saving /etc/default/grub settings
    private final GrubConfigParser configParser;
    // Instance variable for discovering and installing GRUB themes
    private final ThemeManager themeManager;

    // Tabbed pane component for switching between Easy, Advanced, and Theme tabs
    private JTabbedPane tabbedPane;

    // Table component for displaying GRUB key-value pairs in Easy Mode
    private JTable configTable;
    // Table model backing the configTable data
    private DefaultTableModel tableModel;

    // Text area component for raw /etc/default/grub text in Advanced Mode
    private JTextArea rawTextArea;

    // JList component for displaying installed GRUB themes
    private JList<GrubTheme> themeJList;
    // DefaultListModel backing the themeJList data
    private DefaultListModel<GrubTheme> themeListModel;
    // JLabel component for rendering the theme preview image
    private JLabel previewImageLabel;
    // JLabel component for showing selected theme name
    private JLabel selectedThemeNameLabel;
    // JLabel component for showing selected theme directory path
    private JLabel selectedThemePathLabel;

    // Header label for displaying version and update status
    private JLabel updateStatusLabel;
    // Header button for triggering auto-update when available
    private JButton updateNowBtn;

    // Tracking variable to keep track of previous tab index for sync on tab switch
    private int previousTabIndex = 0;

    /**
     * Constructor initializing the main window, components, and loading system settings.
     */
    public MainFrame() {
        // Call super constructor setting frame title with current version number
        super("Grub Helper v" + Version.CURRENT_VERSION + " - Linux Bootloader Manager");
        // Instantiate GrubEnvironment to detect system GRUB paths and tools
        this.environment = new GrubEnvironment();
        // Instantiate GrubConfigParser for reading /etc/default/grub
        this.configParser = new GrubConfigParser();
        // Instantiate ThemeManager passing environment instance
        this.themeManager = new ThemeManager(environment);

        // Set native system look and feel for native GUI styling
        initSystemLookAndFeel();
        // Initialize GUI layout and Swing widgets
        initUI();
        // Load GRUB configuration settings into UI
        loadConfig();
        // Discover and load installed GRUB themes into UI
        loadThemes();
        // Start background worker thread to check for application updates
        checkUpdatesInBackground();
    }

    /**
     * Applies system look and feel theme.
     */
    private void initSystemLookAndFeel() {
        // Try setting look and feel to system native class name
        try {
            // Set LookAndFeel using UIManager
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        // Catch any exception if system look and feel is unavailable
        } catch (Exception ignored) {}
    }

    /**
     * Sets up window properties, header, tabbed pane, and footer action bar.
     */
    private void initUI() {
        // Configure frame to exit process when user closes window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Set default window size to 960x680 pixels
        setSize(960, 680);
        // Center window on screen
        setLocationRelativeTo(null);

        // Instantiate main tabbed pane
        tabbedPane = new JTabbedPane();
        // Set tab title font
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Add Tab 0: Easy Mode panel
        tabbedPane.addTab("  Dashboard (Easy Mode)  ", createEasyModePanel());
        // Add Tab 1: Advanced Mode panel
        tabbedPane.addTab("  Advanced Config Editor  ", createAdvancedModePanel());
        // Add Tab 2: Theme Manager panel
        tabbedPane.addTab("  Theme Manager  ", createThemeManagerPanel());

        // Register change listener to trigger synchronization when user switches tabs
        tabbedPane.addChangeListener(e -> onTabChanged());

        // Create top header panel
        JPanel headerPanel = createHeaderPanel();

        // Create bottom action panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // Add padding around bottom panel
        bottomPanel.setBorder(new EmptyBorder(10, 20, 12, 20));
        // Set background color of bottom panel
        bottomPanel.setBackground(COLOR_BG_LIGHT);

        // Create status footer label
        JLabel statusFooterLabel = new JLabel("Status: Ready");
        // Set status footer font
        statusFooterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Set status footer text color
        statusFooterLabel.setForeground(COLOR_TEXT_MUTED);

        // Create right-aligned panel for action buttons
        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        // Make panel transparent
        actionBtnPanel.setOpaque(false);

        // Create reload button
        JButton reloadButton = createStyledButton("Reload Config", new Color(149, 165, 166), Color.WHITE);
        // Add click listener to reload button
        reloadButton.addActionListener(e -> {
            // Reload GRUB configuration from disk
            loadConfig();
            // Reload installed themes from disk
            loadThemes();
            // Update status footer text
            statusFooterLabel.setText("Status: Configuration reloaded.");
        });

        // Create apply and save button
        JButton saveButton = createStyledButton("Apply Changes & Update GRUB", COLOR_PRIMARY, Color.WHITE);
        // Set bold font on apply button
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        // Add click listener to save button
        saveButton.addActionListener(e -> applyAndSave());

        // Add reload button to action panel
        actionBtnPanel.add(reloadButton);
        // Add save button to action panel
        actionBtnPanel.add(saveButton);

        // Position status text on left of bottom panel
        bottomPanel.add(statusFooterLabel, BorderLayout.WEST);
        // Position action buttons on right of bottom panel
        bottomPanel.add(actionBtnPanel, BorderLayout.EAST);

        // Set window content pane background color
        getContentPane().setBackground(COLOR_BG_LIGHT);
        // Use BorderLayout for main frame content pane
        getContentPane().setLayout(new BorderLayout());
        // Add top header to NORTH region
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        // Add tabbed pane to CENTER region
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        // Add bottom bar to SOUTH region
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Builds header panel showing app title, detected GRUB environment, and update status.
     */
    private JPanel createHeaderPanel() {
        // Create header panel with BorderLayout
        JPanel headerPanel = new JPanel(new BorderLayout(15, 10));
        // Set padding inside header panel
        headerPanel.setBorder(new EmptyBorder(14, 20, 14, 20));
        // Set dark background color for header
        headerPanel.setBackground(COLOR_DARK_HEADER);

        // Create left sub-panel for title and environment details
        JPanel leftHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        // Set panel transparent
        leftHeader.setOpaque(false);

        // Create title label
        JLabel titleLabel = new JLabel("Grub Helper");
        // Set font for title label
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        // Set text color to white
        titleLabel.setForeground(Color.WHITE);

        // Create environment info label displaying detected directories
        JLabel envInfoLabel = new JLabel("Boot Directory: " + environment.getGrubBootDir() + "  |  Themes: " + environment.getGrubThemesDir());
        // Set font for environment info
        envInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Set text color for environment info
        envInfoLabel.setForeground(new Color(189, 195, 199));

        // Add title label to left header panel
        leftHeader.add(titleLabel);
        // Add environment info label to left header panel
        leftHeader.add(envInfoLabel);

        // Create right sub-panel for version label and update button
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        // Set panel transparent
        rightHeader.setOpaque(false);

        // Create version label
        updateStatusLabel = new JLabel("Version v" + Version.CURRENT_VERSION);
        // Set font for version label
        updateStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Set text color for version label
        updateStatusLabel.setForeground(new Color(236, 240, 241));

        // Create update button (hidden by default)
        updateNowBtn = createStyledButton("Update Available", COLOR_SUCCESS, Color.WHITE);
        // Hide button until an update is found
        updateNowBtn.setVisible(false);
        // Add click listener to trigger app update
        updateNowBtn.addActionListener(e -> performAppUpdate());

        // Add update status label to right header
        rightHeader.add(updateStatusLabel);
        // Add update button to right header
        rightHeader.add(updateNowBtn);

        // Add left header to header panel WEST
        headerPanel.add(leftHeader, BorderLayout.WEST);
        // Add right header to header panel EAST
        headerPanel.add(rightHeader, BorderLayout.EAST);

        // Return constructed header panel
        return headerPanel;
    }

    /**
     * Builds Easy Mode dashboard panel containing key-value config table and controls.
     */
    private JPanel createEasyModePanel() {
        // Create panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        // Set outer border padding
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        // Set panel background color
        panel.setBackground(COLOR_BG_LIGHT);

        // Create description label
        JLabel infoLabel = new JLabel("Edit your GRUB parameters visually in the table below:");
        // Set font for description label
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        // Add description label to NORTH region
        panel.add(infoLabel, BorderLayout.NORTH);

        // Define table column headers
        String[] columnNames = {"Setting Key", "Value"};
        // Instantiate DefaultTableModel enabling cell editing
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing on all table cells
                return true;
            }
        };

        // Instantiate JTable with tableModel
        configTable = new JTable(tableModel);
        // Set row height for better touch/click targets
        configTable.setRowHeight(28);
        // Set font for table cells
        configTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
        // Set grid line color
        configTable.setGridColor(new Color(220, 221, 225));

        // Wrap table in JScrollPane
        JScrollPane scrollPane = new JScrollPane(configTable);
        // Set border outline around scroll pane
        scrollPane.setBorder(new LineBorder(new Color(220, 221, 225)));
        // Add scroll pane to CENTER region
        panel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel for table row controls
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        // Set panel transparent
        btnPanel.setOpaque(false);

        // Create add row button
        JButton addRowBtn = createStyledButton("+ Add Parameter", new Color(52, 152, 219), Color.WHITE);
        // Add click listener to insert new setting row into table
        addRowBtn.addActionListener(e -> tableModel.addRow(new String[]{"GRUB_NEW_SETTING", "value"}));

        // Create remove row button
        JButton removeRowBtn = createStyledButton("- Remove Selected", new Color(231, 76, 60), Color.WHITE);
        // Add click listener to delete selected row
        removeRowBtn.addActionListener(e -> {
            // Get index of selected row
            int selectedRow = configTable.getSelectedRow();
            // If a row is selected, remove it from table model
            if (selectedRow != -1) {
                // Remove row from model
                tableModel.removeRow(selectedRow);
            }
        });

        // Add add button to button panel
        btnPanel.add(addRowBtn);
        // Add remove button to button panel
        btnPanel.add(removeRowBtn);
        // Add button panel to SOUTH region
        panel.add(btnPanel, BorderLayout.SOUTH);

        // Return completed Easy Mode panel
        return panel;
    }

    /**
     * Builds Advanced Mode raw text editor panel.
     */
    private JPanel createAdvancedModePanel() {
        // Create panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        // Set outer padding
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        // Set background color
        panel.setBackground(COLOR_BG_LIGHT);

        // Create info description label
        JLabel infoLabel = new JLabel("Directly modify /etc/default/grub raw configuration lines:");
        // Set font for info label
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        // Add info label to NORTH region
        panel.add(infoLabel, BorderLayout.NORTH);

        // Instantiate raw text area
        rawTextArea = new JTextArea();
        // Set monospaced font for code/config text
        rawTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        // Set internal margins inside text area
        rawTextArea.setMargin(new Insets(10, 10, 10, 10));

        // Wrap text area in JScrollPane
        JScrollPane scrollPane = new JScrollPane(rawTextArea);
        // Set outline border
        scrollPane.setBorder(new LineBorder(new Color(220, 221, 225)));
        // Add scroll pane to CENTER region
        panel.add(scrollPane, BorderLayout.CENTER);

        // Return completed Advanced Mode panel
        return panel;
    }

    /**
     * Builds Theme Manager panel for viewing, applying, and installing themes.
     */
    private JPanel createThemeManagerPanel() {
        // Create panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        // Set outer border padding
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        // Set background color
        panel.setBackground(COLOR_BG_LIGHT);

        // Instantiate theme list model
        themeListModel = new DefaultListModel<>();
        // Instantiate theme JList with list model
        themeJList = new JList<>(themeListModel);
        // Set list selection mode to single selection
        themeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Set fixed row height for list items
        themeJList.setFixedCellHeight(32);
        // Set custom cell renderer for formatting theme items
        themeJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // Call super renderer logic
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                // Check if item value is a GrubTheme instance
                if (value instanceof GrubTheme) {
                    // Set item display text to theme name
                    lbl.setText("  " + ((GrubTheme) value).getName());
                    // Set item font
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
                // Return rendered label
                return lbl;
            }
        });

        // Add selection listener to update right preview panel when user selects a theme
        themeJList.addListSelectionListener(e -> {
            // Check if selection change is final
            if (!e.getValueIsAdjusting()) {
                // Update theme details panel with selected theme
                updateThemeDetails(themeJList.getSelectedValue());
            }
        });

        // Wrap theme list in JScrollPane
        JScrollPane listScroll = new JScrollPane(themeJList);
        // Set preferred size width for theme list
        listScroll.setPreferredSize(new Dimension(260, 0));
        // Set outline border on list scroll pane
        listScroll.setBorder(new LineBorder(new Color(220, 221, 225)));

        // Create left side sub-panel for theme list and install button
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        // Set panel transparent
        leftPanel.setOpaque(false);
        // Create header label for installed themes
        JLabel installedHeader = new JLabel("Installed Themes:");
        // Set font for header label
        installedHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        // Add header to left panel NORTH
        leftPanel.add(installedHeader, BorderLayout.NORTH);
        // Add list scroll pane to left panel CENTER
        leftPanel.add(listScroll, BorderLayout.CENTER);

        // Create install theme archive button
        JButton installThemeBtn = createStyledButton("Install Theme Archive", new Color(52, 152, 219), Color.WHITE);
        // Add click listener to open theme archive file chooser dialog
        installThemeBtn.addActionListener(e -> installThemeDialog());
        // Add install button to left panel SOUTH
        leftPanel.add(installThemeBtn, BorderLayout.SOUTH);

        // Create right side sub-panel for theme details and preview image
        JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        // Set border style with outline and padding
        rightPanel.setBorder(new CompoundBorder(new LineBorder(new Color(220, 221, 225)), new EmptyBorder(12, 12, 12, 12)));
        // Set panel transparent
        rightPanel.setOpaque(false);

        // Create sub-panel for theme metadata info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        // Set panel transparent
        infoPanel.setOpaque(false);
        // Create selected theme name label
        selectedThemeNameLabel = new JLabel("Selected: None");
        // Set font for theme name label
        selectedThemeNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        // Create selected theme path label
        selectedThemePathLabel = new JLabel("Path: -");
        // Set font for theme path label
        selectedThemePathLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Set muted text color for theme path
        selectedThemePathLabel.setForeground(COLOR_TEXT_MUTED);
        // Add theme name label to info panel
        infoPanel.add(selectedThemeNameLabel);
        // Add theme path label to info panel
        infoPanel.add(selectedThemePathLabel);

        // Create preview image label
        previewImageLabel = new JLabel("No Preview Available", SwingConstants.CENTER);
        // Set dark background color for preview area
        previewImageLabel.setBackground(new Color(45, 52, 54));
        // Make preview label opaque
        previewImageLabel.setOpaque(true);
        // Set preview text color
        previewImageLabel.setForeground(Color.LIGHT_GRAY);

        // Create set active theme button
        JButton applyThemeBtn = createStyledButton("Set as Active GRUB Theme", COLOR_PRIMARY, Color.WHITE);
        // Set bold font for active theme button
        applyThemeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        // Add click listener to set GRUB_THEME in parser
        applyThemeBtn.addActionListener(e -> {
            // Get currently selected theme from JList
            GrubTheme theme = themeJList.getSelectedValue();
            // Check that theme and theme.txt file exist
            if (theme != null && theme.getThemeTxtFile() != null) {
                // Set GRUB_THEME setting in config parser
                configParser.setValue("GRUB_THEME", theme.getThemeTxtFile().getAbsolutePath());
                // Synchronize parser values to UI
                syncParserToUI();
                // Show notification dialog
                JOptionPane.showMessageDialog(this, "Theme set to: " + theme.getName() + ".\nClick 'Apply Changes & Update GRUB' to write to system.", "Theme Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Add info panel to right panel NORTH
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        // Add preview scroll pane to right panel CENTER
        rightPanel.add(new JScrollPane(previewImageLabel), BorderLayout.CENTER);
        // Add apply theme button to right panel SOUTH
        rightPanel.add(applyThemeBtn, BorderLayout.SOUTH);

        // Position left panel in Theme Manager WEST region
        panel.add(leftPanel, BorderLayout.WEST);
        // Position right panel in Theme Manager CENTER region
        panel.add(rightPanel, BorderLayout.CENTER);

        // Return completed Theme Manager panel
        return panel;
    }

    /**
     * Utility method to create styled JButton instances.
     */
    private JButton createStyledButton(String text, Color bg, Color fg) {
        // Instantiate JButton with text
        JButton btn = new JButton(text);
        // Set font for button
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        // Set background color
        btn.setBackground(bg);
        // Set text foreground color
        btn.setForeground(fg);
        // Disable focus ring outline
        btn.setFocusPainted(false);
        // Set internal button padding
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        // Return styled button instance
        return btn;
    }

    /**
     * Executes asynchronous background task to check for remote application updates.
     */
    private void checkUpdatesInBackground() {
        // Create SwingWorker to perform update check off the EDT thread
        SwingWorker<UpdateChecker.UpdateInfo, Void> worker = new SwingWorker<>() {
            @Override
            protected UpdateChecker.UpdateInfo doInBackground() {
                // Call UpdateChecker to check remote version manifest
                return UpdateChecker.checkForUpdates();
            }

            @Override
            protected void done() {
                try {
                    // Get result from background task
                    UpdateChecker.UpdateInfo info = get();
                    // If update is available, show update button in header
                    if (info.updateAvailable) {
                        // Update status label text
                        updateStatusLabel.setText("New Update Available: v" + info.latestVersion);
                        // Update button label text
                        updateNowBtn.setText("Update to v" + info.latestVersion);
                        // Make update button visible
                        updateNowBtn.setVisible(true);
                    } else {
                        // Update status label text showing app is up to date
                        updateStatusLabel.setText("Version v" + Version.CURRENT_VERSION + " (Up to date)");
                    }
                } catch (Exception ignored) {}
            }
        };
        // Execute SwingWorker thread
        worker.execute();
    }

    /**
     * Triggers auto-update process when user clicks update button and offers option to restart app.
     */
    private void performAppUpdate() {
        // Prompt user for confirmation before performing update
        int confirm = JOptionPane.showConfirmDialog(this,
                "A new version of Grub Helper is available.\nWould you like to download and install the update now?",
                "Update Grub Helper", JOptionPane.YES_NO_OPTION);

        // If user confirmed update
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Execute auto update shell commands as root
                CommandResult result = UpdateChecker.performAutoUpdate();
                // If update completed successfully
                if (result.isSuccess()) {
                    // Prompt user to restart application immediately or later
                    int restartChoice = JOptionPane.showOptionDialog(this,
                            "Grub Helper updated successfully!\n\nWould you like to restart the application now?",
                            "Update Complete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[]{"Restart Now", "Later"},
                            "Restart Now");

                    // If user selected Restart Now
                    if (restartChoice == JOptionPane.YES_OPTION) {
                        // Relaunch app and close current process
                        UpdateChecker.restartApplication();
                    }
                } else {
                    // Show error message dialog with output
                    JOptionPane.showMessageDialog(this, "Failed to update:\n" + result.stderr, "Update Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                // Show exception error dialog
                JOptionPane.showMessageDialog(this, "Error during update: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Stops cell editing in config table to ensure last edited cell value is committed before saving.
     */
    private void stopActiveTableEditing() {
        // Check if table exists and cell editing is currently active
        if (configTable != null && configTable.isEditing()) {
            // Stop cell editing to commit text
            configTable.getCellEditor().stopCellEditing();
        }
    }

    /**
     * Handles synchronizing settings when switching between tabs.
     */
    private void onTabChanged() {
        // Get index of newly selected tab
        int currentTab = tabbedPane.getSelectedIndex();
        // If user was previously on Tab 0 (Easy Mode)
        if (previousTabIndex == 0) {
            // Stop any active table cell editor
            stopActiveTableEditing();
            // Sync table rows into config parser
            syncTableToParser();
        // Else if user was previously on Tab 1 (Advanced Mode)
        } else if (previousTabIndex == 1) {
            // Sync raw text area content into config parser
            configParser.loadFromString(rawTextArea.getText());
        }

        // Update previous tab index tracker
        previousTabIndex = currentTab;
        // Refresh UI components with updated parser content
        syncParserToUI();
    }

    /**
     * Reads values from Easy Mode table and updates config parser model.
     */
    private void syncTableToParser() {
        // Instantiate LinkedHashMap to preserve order
        Map<String, String> tableData = new LinkedHashMap<>();
        // Iterate through each row in table model
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            // Read key from column 0
            String k = (String) tableModel.getValueAt(i, 0);
            // Read value from column 1
            String v = (String) tableModel.getValueAt(i, 1);
            // If key is non-null and non-empty, add to map
            if (k != null && !k.trim().isEmpty()) {
                // Put trimmed key and value into tableData map
                tableData.put(k.trim(), v != null ? v : "");
            }
        }
        // Update parser lines and map from tableData
        configParser.updateFromMap(tableData);
    }

    /**
     * Loads /etc/default/grub file into configParser and updates UI.
     */
    private void loadConfig() {
        // Create File object for /etc/default/grub
        File configFile = new File(environment.getGrubConfigPath());
        // Check if configuration file exists
        if (configFile.exists()) {
            try {
                // Load configuration from file
                configParser.loadFromFile(configFile);
                // Synchronize parser values to UI
                syncParserToUI();
            } catch (IOException e) {
                // Show error message dialog if reading fails
                JOptionPane.showMessageDialog(this, "Error reading " + configFile.getAbsolutePath() + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Load default fallback sample config if file does not exist
            configParser.loadFromString("GRUB_DEFAULT=0\nGRUB_TIMEOUT=5\nGRUB_DISTRIBUTOR=\"Linux\"\n");
            // Synchronize fallback values to UI
            syncParserToUI();
        }
    }

    /**
     * Populates Easy Mode table and Advanced Mode text area from configParser.
     */
    private void syncParserToUI() {
        // Clear existing table rows
        tableModel.setRowCount(0);
        // Iterate through entries in parser config map
        for (Map.Entry<String, String> entry : configParser.getConfigMap().entrySet()) {
            // Add row to table model
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        // Update raw text area text with generated config string
        rawTextArea.setText(configParser.generateConfigString());
    }

    /**
     * Synchronizes current active UI tab inputs into configParser model.
     */
    private void syncUIToParser() {
        // Get index of currently selected tab
        int currentTab = tabbedPane.getSelectedIndex();
        // If on Tab 0 (Easy Mode)
        if (currentTab == 0) {
            // Stop cell editor
            stopActiveTableEditing();
            // Sync table to parser
            syncTableToParser();
        // Else if on Tab 1 (Advanced Mode)
        } else if (currentTab == 1) {
            // Parse text area content into config parser
            configParser.loadFromString(rawTextArea.getText());
        }
    }

    /**
     * Scans system theme directory and populates Theme Manager JList.
     */
    private void loadThemes() {
        // Clear existing theme list model items
        themeListModel.clear();
        // Iterate through discovered themes from ThemeManager
        for (GrubTheme theme : themeManager.getInstalledThemes()) {
            // Add theme to list model
            themeListModel.addElement(theme);
        }
        // If theme list is not empty, select first theme by default
        if (!themeListModel.isEmpty()) {
            // Set selected index to 0
            themeJList.setSelectedIndex(0);
        }
    }

    /**
     * Updates preview image label and metadata labels for selected theme.
     */
    private void updateThemeDetails(GrubTheme theme) {
        // If theme is null, reset labels
        if (theme == null) {
            // Set name label to None
            selectedThemeNameLabel.setText("Selected: None");
            // Set path label to dash
            selectedThemePathLabel.setText("Path: -");
            // Clear preview icon
            previewImageLabel.setIcon(null);
            // Set preview text
            previewImageLabel.setText("No Preview Available");
            // Return early
            return;
        }

        // Set name label text to selected theme name
        selectedThemeNameLabel.setText("Selected: " + theme.getName());
        // Set path label text to theme folder absolute path
        selectedThemePathLabel.setText("Path: " + theme.getFolder().getAbsolutePath());

        // Get preview image file reference
        File imgFile = theme.getPreviewImage();
        // Check if preview image exists
        if (imgFile != null && imgFile.exists()) {
            // Create ImageIcon from image file path
            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
            // Get Image object from icon
            Image img = icon.getImage();
            // Scale image smooth to fit preview label width
            Image scaled = img.getScaledInstance(450, -1, Image.SCALE_SMOOTH);
            // Set scaled image icon on preview label
            previewImageLabel.setIcon(new ImageIcon(scaled));
            // Clear text overlay
            previewImageLabel.setText("");
        } else {
            // Clear icon if no image found
            previewImageLabel.setIcon(null);
            // Display notice text
            previewImageLabel.setText("No Preview Image Found");
        }
    }

    /**
     * Displays JFileChooser dialog to pick theme archive and installs it.
     */
    private void installThemeDialog() {
        // Instantiate JFileChooser instance
        JFileChooser chooser = new JFileChooser();
        // Set dialog title
        chooser.setDialogTitle("Select Theme Archive (.zip, .tar.gz, .tar.xz)");
        // Add file filter for theme archives
        chooser.setFileFilter(new FileNameExtensionFilter("Theme Archives (*.zip, *.tar.gz, *.tar.xz, *.tar)", "zip", "gz", "xz", "tar", "tgz"));

        // Show open dialog and store result code
        int res = chooser.showOpenDialog(this);
        // Check if user approved file selection
        if (res == JFileChooser.APPROVE_OPTION) {
            // Get selected archive File object
            File selectedFile = chooser.getSelectedFile();
            try {
                // Get reference to system themes directory
                File targetThemesDir = new File(environment.getGrubThemesDir());
                // Check if user has write access without root
                if (!targetThemesDir.canWrite()) {
                    // Create temporary staging directory
                    File tempStaging = new File("/tmp/grub_themes_staging");
                    // Make staging directories
                    tempStaging.mkdirs();
                    // Extract theme into staging directory
                    GrubTheme extractedTheme = themeManager.installThemeArchive(selectedFile, tempStaging);

                    // Formulate shell copy command with quoted paths
                    String copyCmd = String.format("mkdir -p \"%s\" && cp -r \"%s\" \"%s/\"",
                            environment.getGrubThemesDir(),
                            extractedTheme.getFolder().getAbsolutePath(),
                            environment.getGrubThemesDir());

                    // Execute copy command as root via pkexec/sudo
                    CommandResult result = RootExecutor.runAsRoot(copyCmd);
                    // Check if command execution succeeded
                    if (!result.isSuccess()) {
                        // Throw exception if copy failed
                        throw new IOException("Failed to install theme as root: " + result.stderr);
                    }
                } else {
                    // Extract directly if writable
                    themeManager.installThemeArchive(selectedFile, targetThemesDir);
                }

                // Show success dialog
                JOptionPane.showMessageDialog(this, "Theme installed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                // Reload theme list in UI
                loadThemes();

            } catch (Exception e) {
                // Print stack trace for debugging
                e.printStackTrace();
                // Show error dialog
                JOptionPane.showMessageDialog(this, "Failed to install theme: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves GRUB configuration changes to disk and executes update-grub command as root.
     */
    private void applyAndSave() {
        // Synchronize current UI active tab inputs into config parser
        syncUIToParser();

        try {
            // Create temporary staging config file
            File tempConfigFile = File.createTempFile("grub_config_", ".tmp");
            // Save config parser contents to temporary file
            configParser.saveToFile(tempConfigFile);

            // Formulate root apply command to overwrite /etc/default/grub and run update command
            String applyCmd = String.format("cp \"%s\" \"%s\" && %s",
                    tempConfigFile.getAbsolutePath(),
                    environment.getGrubConfigPath(),
                    environment.getUpdateGrubCommand());

            // Confirm action with user
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to write changes to " + environment.getGrubConfigPath() + " and execute:\n" + environment.getUpdateGrubCommand() + "?",
                    "Confirm GRUB Update", JOptionPane.YES_NO_OPTION);

            // If user clicked YES
            if (confirm == JOptionPane.YES_OPTION) {
                // Execute command as root via pkexec/sudo
                CommandResult result = RootExecutor.runAsRoot(applyCmd);
                // Check if command succeeded
                if (result.isSuccess()) {
                    // Show success dialog with process output
                    JOptionPane.showMessageDialog(this, "GRUB Configuration updated successfully!\n\nOutput:\n" + result.stdout, "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Show error dialog with stderr output
                    JOptionPane.showMessageDialog(this, "Error updating GRUB:\n" + result.stderr, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            // Delete temporary staging file
            tempConfigFile.delete();

        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();
            // Show error dialog
            JOptionPane.showMessageDialog(this, "Error saving GRUB config: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Application entry point.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Check if --version argument was passed
        if (args.length > 0 && "--version".equals(args[0])) {
            // Print version string to console
            System.out.println("Grub Helper v" + Version.CURRENT_VERSION);
            // Exit program
            return;
        }

        // Schedule GUI instantiation on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Instantiate MainFrame
            MainFrame frame = new MainFrame();
            // Make main frame visible
            frame.setVisible(true);
        });
    }
}
