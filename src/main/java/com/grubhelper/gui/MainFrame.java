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
// Import ArrayList for dynamic lists
import java.util.ArrayList;
// Import LinkedHashMap for preserving configuration key ordering
import java.util.LinkedHashMap;
// Import List interface
import java.util.List;
// Import Map interface for key-value pair operations
import java.util.Map;

/**
 * Main application window (JFrame) for Grub Helper designed with a clean Black & White theme.
 */
public class MainFrame extends JFrame {

    // Define crisp black and white palette constants
    private static final Color COLOR_BLACK = new Color(18, 18, 18);
    private static final Color COLOR_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_CARD_BG = new Color(250, 250, 250);
    private static final Color COLOR_BORDER = new Color(220, 220, 220);
    private static final Color COLOR_DARK_GREY = new Color(60, 60, 60);
    private static final Color COLOR_LIGHT_GREY = new Color(240, 240, 240);

    // Instance variable for managing GRUB system environment information
    private final GrubEnvironment environment;
    // Instance variable for parsing and saving /etc/default/grub settings
    private final GrubConfigParser configParser;
    // Instance variable for discovering and installing GRUB themes
    private final ThemeManager themeManager;

    // Tabbed pane component for switching between Dashboard, Easy Mode, Advanced Mode, and Theme Manager tabs
    private JTabbedPane tabbedPane;

    // Easy Mode components
    private JTable configTable;
    private DefaultTableModel tableModel;

    // Advanced Mode components
    private JTextArea rawTextArea;

    // Theme Manager components
    private JList<GrubTheme> themeJList;
    private DefaultListModel<GrubTheme> themeListModel;
    private JLabel previewImageLabel;
    private JLabel selectedThemeNameLabel;
    private JLabel selectedThemePathLabel;

    // Header label for displaying version and update status
    private JLabel updateStatusLabel;
    // Header button for triggering auto-update when available
    private JButton updateNowBtn;

    // Holds fetched UpdateInfo from background update check
    private UpdateChecker.UpdateInfo latestUpdateInfo;

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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    /**
     * Sets up window properties, header, four separated navigation tabs, and footer action bar.
     */
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Add Tab 0: Dashboard Overview
        tabbedPane.addTab("  Dashboard  ", createDashboardPanel());
        // Add Tab 1: GRUB Config Easy Mode
        tabbedPane.addTab("  GRUB Config (Easy)  ", createEasyModePanel());
        // Add Tab 2: GRUB Config Advanced Mode
        tabbedPane.addTab("  GRUB Config (Advanced)  ", createAdvancedModePanel());
        // Add Tab 3: Theme Manager
        tabbedPane.addTab("  Theme Manager  ", createThemeManagerPanel());

        // Register change listener to trigger synchronization when user switches tabs
        tabbedPane.addChangeListener(e -> onTabChanged());

        // Create top header panel
        JPanel headerPanel = createHeaderPanel();

        // Create bottom action panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(12, 20, 14, 20));
        bottomPanel.setBackground(COLOR_WHITE);

        JLabel statusFooterLabel = new JLabel("Status: Ready");
        statusFooterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusFooterLabel.setForeground(COLOR_DARK_GREY);

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionBtnPanel.setOpaque(false);

        JButton reloadButton = createStyledButton("Reload Config", COLOR_LIGHT_GREY, COLOR_BLACK);
        reloadButton.addActionListener(e -> {
            loadConfig();
            loadThemes();
            statusFooterLabel.setText("Status: Configuration reloaded.");
        });

        JButton saveButton = createStyledButton("Apply Changes & Update GRUB", COLOR_BLACK, COLOR_WHITE);
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveButton.addActionListener(e -> applyAndSave());

        actionBtnPanel.add(reloadButton);
        actionBtnPanel.add(saveButton);

        bottomPanel.add(statusFooterLabel, BorderLayout.WEST);
        bottomPanel.add(actionBtnPanel, BorderLayout.EAST);

        getContentPane().setBackground(COLOR_WHITE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Builds header panel showing app title, detected GRUB environment, and update status.
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 10));
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        headerPanel.setBackground(COLOR_BLACK);

        JPanel leftHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        leftHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Grub Helper");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_WHITE);

        JLabel envInfoLabel = new JLabel("Linux Bootloader Manager  |  Path: " + environment.getGrubBootDir());
        envInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        envInfoLabel.setForeground(new Color(200, 200, 200));

        leftHeader.add(titleLabel);
        leftHeader.add(envInfoLabel);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);

        updateStatusLabel = new JLabel("Version v" + Version.CURRENT_VERSION);
        updateStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        updateStatusLabel.setForeground(COLOR_WHITE);

        updateNowBtn = createStyledButton("Update Available", COLOR_WHITE, COLOR_BLACK);
        updateNowBtn.setVisible(false);
        updateNowBtn.addActionListener(e -> performAppUpdate());

        rightHeader.add(updateStatusLabel);
        rightHeader.add(updateNowBtn);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Builds Dashboard panel explaining what Grub Helper is and displaying system info metadata.
     */
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(COLOR_WHITE);

        // Welcome banner card
        JPanel welcomeCard = new JPanel(new BorderLayout(12, 12));
        welcomeCard.setBackground(COLOR_CARD_BG);
        welcomeCard.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER), new EmptyBorder(18, 18, 18, 18)));

        JLabel welcomeTitle = new JLabel("Welcome to Grub Helper");
        welcomeTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcomeTitle.setForeground(COLOR_BLACK);

        JTextArea welcomeText = new JTextArea(
                "Grub Helper is a modern GUI utility that simplifies managing your Linux GRUB bootloader.\n" +
                "Easily enable or disable configuration options, modify kernel command parameters, switch themes,\n" +
                "and install new GRUB theme archives safely without manually modifying system config files."
        );
        welcomeText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        welcomeText.setEditable(false);
        welcomeText.setOpaque(false);
        welcomeText.setLineWrap(true);
        welcomeText.setWrapStyleWord(true);

        welcomeCard.add(welcomeTitle, BorderLayout.NORTH);
        welcomeCard.add(welcomeText, BorderLayout.CENTER);

        // System Information Card
        JPanel infoCard = new JPanel(new BorderLayout(12, 12));
        infoCard.setBackground(COLOR_CARD_BG);
        infoCard.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER), new EmptyBorder(18, 18, 18, 18)));

        JLabel infoTitle = new JLabel("System & GRUB Information");
        infoTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        infoTitle.setForeground(COLOR_BLACK);

        JPanel gridPanel = new JPanel(new GridLayout(6, 2, 12, 10));
        gridPanel.setOpaque(false);

        addInfoRow(gridPanel, "Operating System:", environment.getOsName());
        addInfoRow(gridPanel, "Kernel Version:", environment.getKernelVersion());
        addInfoRow(gridPanel, "Architecture:", environment.getArchitecture());
        addInfoRow(gridPanel, "GRUB Version:", environment.getGrubVersion());
        addInfoRow(gridPanel, "GRUB Boot Directory:", environment.getGrubBootDir());
        addInfoRow(gridPanel, "GRUB Themes Directory:", environment.getGrubThemesDir());

        infoCard.add(infoTitle, BorderLayout.NORTH);
        infoCard.add(gridPanel, BorderLayout.CENTER);

        panel.add(welcomeCard, BorderLayout.NORTH);
        panel.add(infoCard, BorderLayout.CENTER);

        return panel;
    }

    private void addInfoRow(JPanel grid, String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(COLOR_DARK_GREY);

        JLabel val = new JLabel(value != null && !value.isEmpty() ? value : "N/A");
        val.setFont(new Font("SansSerif", Font.PLAIN, 13));
        val.setForeground(COLOR_BLACK);

        grid.add(lbl);
        grid.add(val);
    }

    /**
     * Builds Easy Mode panel containing key-value config table with an 'Enabled' checkbox column.
     */
    private JPanel createEasyModePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_WHITE);

        JLabel infoLabel = new JLabel("Visually manage GRUB options. Checkboxes enable or comment-out (#) specific settings:");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(infoLabel, BorderLayout.NORTH);

        String[] columnNames = {"Enabled", "Setting Key", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        configTable = new JTable(tableModel);
        configTable.setRowHeight(28);
        configTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
        configTable.setGridColor(COLOR_BORDER);
        configTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        configTable.getColumnModel().getColumn(0).setMaxWidth(90);

        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setBorder(new LineBorder(COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addRowBtn = createStyledButton("+ Add Parameter", COLOR_BLACK, COLOR_WHITE);
        addRowBtn.addActionListener(e -> tableModel.addRow(new Object[]{Boolean.TRUE, "GRUB_NEW_SETTING", "value"}));

        JButton removeRowBtn = createStyledButton("- Remove Selected", COLOR_LIGHT_GREY, COLOR_BLACK);
        removeRowBtn.addActionListener(e -> {
            int selectedRow = configTable.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
            }
        });

        btnPanel.add(addRowBtn);
        btnPanel.add(removeRowBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds Advanced Mode raw text editor panel.
     */
    private JPanel createAdvancedModePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_WHITE);

        JLabel infoLabel = new JLabel("Directly modify /etc/default/grub raw configuration lines:");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(infoLabel, BorderLayout.NORTH);

        rawTextArea = new JTextArea();
        rawTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        rawTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(rawTextArea);
        scrollPane.setBorder(new LineBorder(COLOR_BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds Theme Manager panel for viewing, applying, and installing themes.
     */
    private JPanel createThemeManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_WHITE);

        themeListModel = new DefaultListModel<>();
        themeJList = new JList<>(themeListModel);
        themeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeJList.setFixedCellHeight(32);
        themeJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GrubTheme) {
                    lbl.setText("  " + ((GrubTheme) value).getName());
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
                return lbl;
            }
        });

        themeJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateThemeDetails(themeJList.getSelectedValue());
            }
        });

        JScrollPane listScroll = new JScrollPane(themeJList);
        listScroll.setPreferredSize(new Dimension(260, 0));
        listScroll.setBorder(new LineBorder(COLOR_BORDER));

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);
        JLabel installedHeader = new JLabel("Installed Themes:");
        installedHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        leftPanel.add(installedHeader, BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton installThemeBtn = createStyledButton("Install Theme Archive", COLOR_LIGHT_GREY, COLOR_BLACK);
        installThemeBtn.addActionListener(e -> installThemeDialog());
        leftPanel.add(installThemeBtn, BorderLayout.SOUTH);

        // Right details & preview panel
        JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        rightPanel.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER), new EmptyBorder(12, 12, 12, 12)));
        rightPanel.setOpaque(false);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        infoPanel.setOpaque(false);
        selectedThemeNameLabel = new JLabel("Selected: None");
        selectedThemeNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectedThemePathLabel = new JLabel("Path: -");
        selectedThemePathLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        selectedThemePathLabel.setForeground(COLOR_DARK_GREY);
        infoPanel.add(selectedThemeNameLabel);
        infoPanel.add(selectedThemePathLabel);

        previewImageLabel = new JLabel("No Preview Available", SwingConstants.CENTER);
        previewImageLabel.setBackground(COLOR_BLACK);
        previewImageLabel.setOpaque(true);
        previewImageLabel.setForeground(COLOR_WHITE);

        JButton applyThemeBtn = createStyledButton("Set as Active GRUB Theme", COLOR_BLACK, COLOR_WHITE);
        applyThemeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        applyThemeBtn.addActionListener(e -> {
            GrubTheme theme = themeJList.getSelectedValue();
            if (theme != null && theme.getThemeTxtFile() != null) {
                configParser.setValue("GRUB_THEME", theme.getThemeTxtFile().getAbsolutePath());
                syncParserToUI();
                JOptionPane.showMessageDialog(this, "Theme set to: " + theme.getName() + ".\nClick 'Apply Changes & Update GRUB' to write to system.", "Theme Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(previewImageLabel), BorderLayout.CENTER);
        rightPanel.add(applyThemeBtn, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Utility method to create styled JButton instances in black/white palette.
     */
    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER), new EmptyBorder(8, 14, 8, 14)));
        return btn;
    }

    /**
     * Executes asynchronous background task to check for remote application updates.
     */
    private void checkUpdatesInBackground() {
        SwingWorker<UpdateChecker.UpdateInfo, Void> worker = new SwingWorker<>() {
            @Override
            protected UpdateChecker.UpdateInfo doInBackground() {
                return UpdateChecker.checkForUpdates();
            }

            @Override
            protected void done() {
                try {
                    latestUpdateInfo = get();
                    if (latestUpdateInfo != null && latestUpdateInfo.updateAvailable) {
                        updateStatusLabel.setText("New Update Available: v" + latestUpdateInfo.latestVersion);
                        updateNowBtn.setText("Update to v" + latestUpdateInfo.latestVersion);
                        updateNowBtn.setVisible(true);
                    } else {
                        updateStatusLabel.setText("Version v" + Version.CURRENT_VERSION + " (Up to date)");
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    /**
     * Triggers auto-update process when user clicks update button and offers option to restart app.
     */
    private void performAppUpdate() {
        String targetVer = (latestUpdateInfo != null && latestUpdateInfo.latestVersion != null) ? latestUpdateInfo.latestVersion : "";

        int confirm = JOptionPane.showConfirmDialog(this,
                "A new version of Grub Helper (v" + targetVer + ") is available.\nWould you like to download and install the update now?",
                "Update Grub Helper", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                CommandResult result = UpdateChecker.performAutoUpdate(targetVer);
                if (result.isSuccess()) {
                    int restartChoice = JOptionPane.showOptionDialog(this,
                            "Grub Helper updated successfully to v" + targetVer + "!\n\nWould you like to restart the application now to load the changes?",
                            "Update Complete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[]{"Restart Now", "Later"},
                            "Restart Now");

                    if (restartChoice == JOptionPane.YES_OPTION) {
                        UpdateChecker.restartApplication();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update:\n" + result.stderr, "Update Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error during update: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Stops cell editing in config table to ensure last edited cell value is committed before saving.
     */
    private void stopActiveTableEditing() {
        if (configTable != null && configTable.isEditing()) {
            configTable.getCellEditor().stopCellEditing();
        }
    }

    /**
     * Handles synchronizing settings when switching between tabs.
     */
    private void onTabChanged() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (previousTabIndex == 1) { // Leaving Easy Mode
            stopActiveTableEditing();
            syncTableToParser();
        } else if (previousTabIndex == 2) { // Leaving Advanced Mode
            configParser.loadFromString(rawTextArea.getText());
        }

        previousTabIndex = currentTab;
        syncParserToUI();
    }

    /**
     * Reads values from Easy Mode table and updates config parser model.
     */
    private void syncTableToParser() {
        List<GrubConfigParser.ConfigItem> items = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean enabled = (Boolean) tableModel.getValueAt(i, 0);
            String k = (String) tableModel.getValueAt(i, 1);
            String v = (String) tableModel.getValueAt(i, 2);
            if (k != null && !k.trim().isEmpty()) {
                items.add(new GrubConfigParser.ConfigItem(k.trim(), v != null ? v : "", enabled != null && enabled));
            }
        }
        configParser.updateFromItems(items);
    }

    /**
     * Loads /etc/default/grub file into configParser and updates UI.
     */
    private void loadConfig() {
        File configFile = new File(environment.getGrubConfigPath());
        if (configFile.exists()) {
            try {
                configParser.loadFromFile(configFile);
                syncParserToUI();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading " + configFile.getAbsolutePath() + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            configParser.loadFromString("GRUB_DEFAULT=0\nGRUB_TIMEOUT=5\n# GRUB_CMDLINE_LINUX_DEFAULT=\"quiet splash\"\n");
            syncParserToUI();
        }
    }

    /**
     * Populates Easy Mode table and Advanced Mode text area from configParser.
     */
    private void syncParserToUI() {
        tableModel.setRowCount(0);
        for (Map.Entry<String, GrubConfigParser.ConfigItem> entry : configParser.getAllConfigItems().entrySet()) {
            GrubConfigParser.ConfigItem item = entry.getValue();
            tableModel.addRow(new Object[]{item.isEnabled(), item.getKey(), item.getValue()});
        }
        rawTextArea.setText(configParser.generateConfigString());
    }

    /**
     * Synchronizes current active UI tab inputs into configParser model.
     */
    private void syncUIToParser() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab == 1) { // Easy Mode
            stopActiveTableEditing();
            syncTableToParser();
        } else if (currentTab == 2) { // Advanced Mode
            configParser.loadFromString(rawTextArea.getText());
        }
    }

    /**
     * Scans system theme directory and populates Theme Manager JList.
     */
    private void loadThemes() {
        themeListModel.clear();
        for (GrubTheme theme : themeManager.getInstalledThemes()) {
            themeListModel.addElement(theme);
        }
        if (!themeListModel.isEmpty()) {
            themeJList.setSelectedIndex(0);
        }
    }

    /**
     * Updates preview image label and metadata labels for selected theme.
     */
    private void updateThemeDetails(GrubTheme theme) {
        if (theme == null) {
            selectedThemeNameLabel.setText("Selected: None");
            selectedThemePathLabel.setText("Path: -");
            previewImageLabel.setIcon(null);
            previewImageLabel.setText("No Preview Available");
            return;
        }

        selectedThemeNameLabel.setText("Selected: " + theme.getName());
        selectedThemePathLabel.setText("Path: " + theme.getFolder().getAbsolutePath());

        File imgFile = theme.getPreviewImage();
        if (imgFile != null && imgFile.exists()) {
            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
            Image img = icon.getImage();
            Image scaled = img.getScaledInstance(450, -1, Image.SCALE_SMOOTH);
            previewImageLabel.setIcon(new ImageIcon(scaled));
            previewImageLabel.setText("");
        } else {
            previewImageLabel.setIcon(null);
            previewImageLabel.setText("No Preview Image Found");
        }
    }

    /**
     * Displays JFileChooser dialog to pick theme archive and installs it.
     */
    private void installThemeDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Theme Archive (.zip, .tar.gz, .tar.xz)");
        chooser.setFileFilter(new FileNameExtensionFilter("Theme Archives (*.zip, *.tar.gz, *.tar.xz, *.tar)", "zip", "gz", "xz", "tar", "tgz"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                File targetThemesDir = new File(environment.getGrubThemesDir());
                if (!targetThemesDir.canWrite()) {
                    File tempStaging = new File("/tmp/grub_themes_staging");
                    tempStaging.mkdirs();
                    GrubTheme extractedTheme = themeManager.installThemeArchive(selectedFile, tempStaging);

                    String copyCmd = String.format("mkdir -p \"%s\" && cp -r \"%s\" \"%s/\"",
                            environment.getGrubThemesDir(),
                            extractedTheme.getFolder().getAbsolutePath(),
                            environment.getGrubThemesDir());

                    CommandResult result = RootExecutor.runAsRoot(copyCmd);
                    if (!result.isSuccess()) {
                        throw new IOException("Failed to install theme as root: " + result.stderr);
                    }
                } else {
                    themeManager.installThemeArchive(selectedFile, targetThemesDir);
                }

                JOptionPane.showMessageDialog(this, "Theme installed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadThemes();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to install theme: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves GRUB configuration changes to disk and executes update-grub command as root.
     */
    private void applyAndSave() {
        syncUIToParser();

        try {
            File tempConfigFile = File.createTempFile("grub_config_", ".tmp");
            configParser.saveToFile(tempConfigFile);

            String applyCmd = String.format("cp \"%s\" \"%s\" && %s",
                    tempConfigFile.getAbsolutePath(),
                    environment.getGrubConfigPath(),
                    environment.getUpdateGrubCommand());

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to write changes to " + environment.getGrubConfigPath() + " and execute:\n" + environment.getUpdateGrubCommand() + "?",
                    "Confirm GRUB Update", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                CommandResult result = RootExecutor.runAsRoot(applyCmd);
                if (result.isSuccess()) {
                    JOptionPane.showMessageDialog(this, "GRUB Configuration updated successfully!\n\nOutput:\n" + result.stdout, "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error updating GRUB:\n" + result.stderr, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            tempConfigFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving GRUB config: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Application entry point.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && "--version".equals(args[0])) {
            System.out.println("Grub Helper v" + Version.CURRENT_VERSION);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
