package com.grubhelper.gui;

import com.grubhelper.model.GrubConfigParser;
import com.grubhelper.model.GrubEnvironment;
import com.grubhelper.model.GrubTheme;
import com.grubhelper.model.ThemeManager;
import com.grubhelper.util.RootExecutor;
import com.grubhelper.util.UpdateChecker;
import com.grubhelper.util.Version;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private static final Color COLOR_PRIMARY = new Color(41, 128, 185);
    private static final Color COLOR_DARK_HEADER = new Color(30, 39, 46);
    private static final Color COLOR_BG_LIGHT = new Color(245, 246, 250);
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113);
    private static final Color COLOR_TEXT_MUTED = new Color(127, 140, 141);

    private final GrubEnvironment environment;
    private final GrubConfigParser configParser;
    private final ThemeManager themeManager;

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

    // Header update status label
    private JLabel updateStatusLabel;
    private JButton updateNowBtn;

    private int previousTabIndex = 0;

    public MainFrame() {
        super("Grub Helper v" + Version.CURRENT_VERSION + " - Linux Bootloader Manager");
        this.environment = new GrubEnvironment();
        this.configParser = new GrubConfigParser();
        this.themeManager = new ThemeManager(environment);

        initSystemLookAndFeel();
        initUI();
        loadConfig();
        loadThemes();
        checkUpdatesInBackground();
    }

    private void initSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        tabbedPane.addTab("  Dashboard (Easy Mode)  ", createEasyModePanel());
        tabbedPane.addTab("  Advanced Config Editor  ", createAdvancedModePanel());
        tabbedPane.addTab("  Theme Manager  ", createThemeManagerPanel());

        tabbedPane.addChangeListener(e -> onTabChanged());

        // Header Panel
        JPanel headerPanel = createHeaderPanel();

        // Bottom Action Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 20, 12, 20));
        bottomPanel.setBackground(COLOR_BG_LIGHT);

        JLabel statusFooterLabel = new JLabel("Status: Ready");
        statusFooterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusFooterLabel.setForeground(COLOR_TEXT_MUTED);

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionBtnPanel.setOpaque(false);

        JButton reloadButton = createStyledButton("Reload Config", new Color(149, 165, 166), Color.WHITE);
        reloadButton.addActionListener(e -> {
            loadConfig();
            loadThemes();
            statusFooterLabel.setText("Status: Configuration reloaded.");
        });

        JButton saveButton = createStyledButton("Apply Changes & Update GRUB", COLOR_PRIMARY, Color.WHITE);
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveButton.addActionListener(e -> applyAndSave());

        actionBtnPanel.add(reloadButton);
        actionBtnPanel.add(saveButton);

        bottomPanel.add(statusFooterLabel, BorderLayout.WEST);
        bottomPanel.add(actionBtnPanel, BorderLayout.EAST);

        getContentPane().setBackground(COLOR_BG_LIGHT);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 10));
        headerPanel.setBorder(new EmptyBorder(14, 20, 14, 20));
        headerPanel.setBackground(COLOR_DARK_HEADER);

        JPanel leftHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        leftHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Grub Helper");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel envInfoLabel = new JLabel("Boot Directory: " + environment.getGrubBootDir() + "  |  Themes: " + environment.getGrubThemesDir());
        envInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        envInfoLabel.setForeground(new Color(189, 195, 199));

        leftHeader.add(titleLabel);
        leftHeader.add(envInfoLabel);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);

        updateStatusLabel = new JLabel("Version v" + Version.CURRENT_VERSION);
        updateStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        updateStatusLabel.setForeground(new Color(236, 240, 241));

        updateNowBtn = createStyledButton("Update Available", COLOR_SUCCESS, Color.WHITE);
        updateNowBtn.setVisible(false);
        updateNowBtn.addActionListener(e -> performAppUpdate());

        rightHeader.add(updateStatusLabel);
        rightHeader.add(updateNowBtn);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createEasyModePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_BG_LIGHT);

        JLabel infoLabel = new JLabel("Edit your GRUB parameters visually in the table below:");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(infoLabel, BorderLayout.NORTH);

        String[] columnNames = {"Setting Key", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        configTable = new JTable(tableModel);
        configTable.setRowHeight(28);
        configTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
        configTable.setGridColor(new Color(220, 221, 225));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setBorder(new LineBorder(new Color(220, 221, 225)));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addRowBtn = createStyledButton("+ Add Parameter", new Color(52, 152, 219), Color.WHITE);
        addRowBtn.addActionListener(e -> tableModel.addRow(new String[]{"GRUB_NEW_SETTING", "value"}));

        JButton removeRowBtn = createStyledButton("- Remove Selected", new Color(231, 76, 60), Color.WHITE);
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

    private JPanel createAdvancedModePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_BG_LIGHT);

        JLabel infoLabel = new JLabel("Directly modify /etc/default/grub raw configuration lines:");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(infoLabel, BorderLayout.NORTH);

        rawTextArea = new JTextArea();
        rawTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        rawTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(rawTextArea);
        scrollPane.setBorder(new LineBorder(new Color(220, 221, 225)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createThemeManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_BG_LIGHT);

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
        listScroll.setBorder(new LineBorder(new Color(220, 221, 225)));

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);
        JLabel installedHeader = new JLabel("Installed Themes:");
        installedHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        leftPanel.add(installedHeader, BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton installThemeBtn = createStyledButton("Install Theme Archive", new Color(52, 152, 219), Color.WHITE);
        installThemeBtn.addActionListener(e -> installThemeDialog());
        leftPanel.add(installThemeBtn, BorderLayout.SOUTH);

        // Right details & preview panel
        JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        rightPanel.setBorder(new CompoundBorder(new LineBorder(new Color(220, 221, 225)), new EmptyBorder(12, 12, 12, 12)));
        rightPanel.setOpaque(false);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        infoPanel.setOpaque(false);
        selectedThemeNameLabel = new JLabel("Selected: None");
        selectedThemeNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectedThemePathLabel = new JLabel("Path: -");
        selectedThemePathLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        selectedThemePathLabel.setForeground(COLOR_TEXT_MUTED);
        infoPanel.add(selectedThemeNameLabel);
        infoPanel.add(selectedThemePathLabel);

        previewImageLabel = new JLabel("No Preview Available", SwingConstants.CENTER);
        previewImageLabel.setBackground(new Color(45, 52, 54));
        previewImageLabel.setOpaque(true);
        previewImageLabel.setForeground(Color.LIGHT_GRAY);

        JButton applyThemeBtn = createStyledButton("Set as Active GRUB Theme", COLOR_PRIMARY, Color.WHITE);
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

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    private void checkUpdatesInBackground() {
        SwingWorker<UpdateChecker.UpdateInfo, Void> worker = new SwingWorker<>() {
            @Override
            protected UpdateChecker.UpdateInfo doInBackground() {
                return UpdateChecker.checkForUpdates();
            }

            @Override
            protected void done() {
                try {
                    UpdateChecker.UpdateInfo info = get();
                    if (info.updateAvailable) {
                        updateStatusLabel.setText("New Update Available: v" + info.latestVersion);
                        updateNowBtn.setText("Update to v" + info.latestVersion);
                        updateNowBtn.setVisible(true);
                    } else {
                        updateStatusLabel.setText("Version v" + Version.CURRENT_VERSION + " (Up to date)");
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void performAppUpdate() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "A new version of Grub Helper is available.\nWould you like to pull updates and reinstall now?",
                "Update Grub Helper", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                RootExecutor.CommandResult result = UpdateChecker.performAutoUpdate();
                if (result.isSuccess()) {
                    JOptionPane.showMessageDialog(this, "Grub Helper updated successfully!\nPlease restart the application.", "Update Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update: " + result.stderr, "Update Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error during update: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void stopActiveTableEditing() {
        if (configTable != null && configTable.isEditing()) {
            configTable.getCellEditor().stopCellEditing();
        }
    }

    private void onTabChanged() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (previousTabIndex == 0) { // Leaving Easy Mode
            stopActiveTableEditing();
            syncTableToParser();
        } else if (previousTabIndex == 1) { // Leaving Advanced Mode
            configParser.loadFromString(rawTextArea.getText());
        }

        previousTabIndex = currentTab;
        syncParserToUI();
    }

    private void syncTableToParser() {
        Map<String, String> tableData = new LinkedHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String k = (String) tableModel.getValueAt(i, 0);
            String v = (String) tableModel.getValueAt(i, 1);
            if (k != null && !k.trim().isEmpty()) {
                tableData.put(k.trim(), v != null ? v : "");
            }
        }
        configParser.updateFromMap(tableData);
    }

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
            configParser.loadFromString("GRUB_DEFAULT=0\nGRUB_TIMEOUT=5\nGRUB_DISTRIBUTOR=\"Linux\"\n");
            syncParserToUI();
        }
    }

    private void syncParserToUI() {
        tableModel.setRowCount(0);
        for (Map.Entry<String, String> entry : configParser.getConfigMap().entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        rawTextArea.setText(configParser.generateConfigString());
    }

    private void syncUIToParser() {
        int currentTab = tabbedPane.getSelectedIndex();
        if (currentTab == 0) { // Easy Mode
            stopActiveTableEditing();
            syncTableToParser();
        } else if (currentTab == 1) { // Advanced Mode
            configParser.loadFromString(rawTextArea.getText());
        }
    }

    private void loadThemes() {
        themeListModel.clear();
        for (GrubTheme theme : themeManager.getInstalledThemes()) {
            themeListModel.addElement(theme);
        }
        if (!themeListModel.isEmpty()) {
            themeJList.setSelectedIndex(0);
        }
    }

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

                    RootExecutor.CommandResult result = RootExecutor.runAsRoot(copyCmd);
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
                RootExecutor.CommandResult result = RootExecutor.runAsRoot(applyCmd);
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
