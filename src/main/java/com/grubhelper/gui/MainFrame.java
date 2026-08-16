package com.grubhelper.gui;

import com.grubhelper.model.GrubConfigParser;
import com.grubhelper.model.GrubEnvironment;
import com.grubhelper.model.GrubTheme;
import com.grubhelper.model.ThemeManager;
import com.grubhelper.util.RootExecutor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

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

    private int previousTabIndex = 0;

    public MainFrame() {
        super("Grub Helper - Linux Bootloader Manager");
        this.environment = new GrubEnvironment();
        this.configParser = new GrubConfigParser();
        this.themeManager = new ThemeManager(environment);

        initSystemLookAndFeel();
        initUI();
        loadConfig();
        loadThemes();
    }

    private void initSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Dashboard (Easy Mode)", createEasyModePanel());
        tabbedPane.addTab("Advanced Config Editor", createAdvancedModePanel());
        tabbedPane.addTab("Theme Manager", createThemeManagerPanel());

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                onTabChanged();
            }
        });

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        headerPanel.setBackground(new Color(45, 52, 54));

        JLabel titleLabel = new JLabel("Grub Helper");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel envInfoLabel = new JLabel("Detected GRUB Boot Dir: " + environment.getGrubBootDir() + " | Themes: " + environment.getGrubThemesDir());
        envInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        envInfoLabel.setForeground(new Color(223, 230, 233));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(envInfoLabel, BorderLayout.EAST);

        // Bottom Action Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton saveButton = new JButton("Apply Changes & Update GRUB");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveButton.setBackground(new Color(9, 132, 227));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> applyAndSave());

        JButton reloadButton = new JButton("Reload Config");
        reloadButton.addActionListener(e -> {
            loadConfig();
            loadThemes();
        });

        bottomPanel.add(reloadButton);
        bottomPanel.add(saveButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createEasyModePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("Easy Mode: Edit GRUB settings key-value pairs directly in the table below.");
        infoLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        panel.add(infoLabel, BorderLayout.NORTH);

        String[] columnNames = {"Setting Key", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        configTable = new JTable(tableModel);
        configTable.setRowHeight(25);
        configTable.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(configTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton addRowBtn = new JButton("Add Setting");
        addRowBtn.addActionListener(e -> tableModel.addRow(new String[]{"GRUB_NEW_SETTING", "value"}));

        JButton removeRowBtn = new JButton("Remove Selected");
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
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("Advanced Mode: Directly edit raw /etc/default/grub file text.");
        infoLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        panel.add(infoLabel, BorderLayout.NORTH);

        rawTextArea = new JTextArea();
        rawTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(rawTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createThemeManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        themeListModel = new DefaultListModel<>();
        themeJList = new JList<>(themeListModel);
        themeJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GrubTheme) {
                    lbl.setText("  " + ((GrubTheme) value).getName() + "  ");
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
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
        listScroll.setPreferredSize(new Dimension(250, 0));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(new JLabel("Installed Themes:"), BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton installThemeBtn = new JButton("Install Theme (.zip, .tar.gz)");
        installThemeBtn.addActionListener(e -> installThemeDialog());
        leftPanel.add(installThemeBtn, BorderLayout.SOUTH);

        // Right details & preview panel
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Theme Details & Preview"));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        selectedThemeNameLabel = new JLabel("Selected: None");
        selectedThemeNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectedThemePathLabel = new JLabel("Path: -");
        selectedThemePathLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPanel.add(selectedThemeNameLabel);
        infoPanel.add(selectedThemePathLabel);

        previewImageLabel = new JLabel("No Preview Available", SwingConstants.CENTER);
        previewImageLabel.setBackground(Color.DARK_GRAY);
        previewImageLabel.setOpaque(true);
        previewImageLabel.setForeground(Color.LIGHT_GRAY);

        JButton applyThemeBtn = new JButton("Set Active Theme");
        applyThemeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        applyThemeBtn.addActionListener(e -> {
            GrubTheme theme = themeJList.getSelectedValue();
            if (theme != null && theme.getThemeTxtFile() != null) {
                configParser.setValue("GRUB_THEME", theme.getThemeTxtFile().getAbsolutePath());
                syncParserToUI();
                JOptionPane.showMessageDialog(this, "Theme set to: " + theme.getName() + ".\nClick 'Apply Changes & Update GRUB' to apply.", "Theme Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(previewImageLabel), BorderLayout.CENTER);
        rightPanel.add(applyThemeBtn, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
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
            // Load sample if not exists
            configParser.loadFromString("GRUB_DEFAULT=0\nGRUB_TIMEOUT=5\nGRUB_DISTRIBUTOR=\"Linux\"\n");
            syncParserToUI();
        }
    }

    private void syncParserToUI() {
        // Table sync
        tableModel.setRowCount(0);
        for (Map.Entry<String, String> entry : configParser.getConfigMap().entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        // Raw text sync
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
            // Scale icon for preview
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
                    // Staging directory
                    File tempStaging = new File("/tmp/grub_themes_staging");
                    tempStaging.mkdirs();
                    GrubTheme extractedTheme = themeManager.installThemeArchive(selectedFile, tempStaging);

                    // Move using root privileges with safe quoted paths
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
            // Write temporary file
            File tempConfigFile = File.createTempFile("grub_config_", ".tmp");
            configParser.saveToFile(tempConfigFile);

            // Command to overwrite /etc/default/grub and run update-grub with safely quoted path
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
            System.out.println("Grub Helper v1.0.0");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
