package ui;

import service.ImportExportService;
import service.ImportExportService.ImportResult;
import service.ImportExportService.ProgressCallback;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 数据导入/导出对话框
 * 提供图书、借阅记录、读者信息、分类信息的 JSON 格式导入与导出功能
 */
public class ImportExportDialog extends JDialog {

    private final JFrame parent;
    private final ImportExportService service = new ImportExportService();

    // 导出组件
    private JComboBox<String> exportTypeCombo;
    private JProgressBar exportProgressBar;
    private JTextArea exportStatusArea;
    private JButton exportBtn;

    // 导入组件
    private JTextField importFileField;
    private JComboBox<String> duplicateStrategyCombo;
    private JProgressBar importProgressBar;
    private JTextArea importStatusArea;
    private JButton importBtn;
    private JButton chooseFileBtn;
    private JLabel importTypeLabel;

    private String selectedImportFilePath = null;

    public ImportExportDialog(JFrame parent) {
        super(parent, "数据导入 / 导出", true);
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setSize(650, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        tabbedPane.addTab("导出数据", createExportPanel());
        tabbedPane.addTab("导入数据", createImportPanel());

        setContentPane(tabbedPane);
    }

    // ========================= 导出面板 =========================

    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部选项区
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel typeLabel = new JLabel("选择导出数据类型:");
        typeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        topPanel.add(typeLabel);

        exportTypeCombo = new JComboBox<>(new String[]{"图书信息", "借阅记录", "读者信息", "图书分类"});
        exportTypeCombo.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        exportTypeCombo.setPreferredSize(new Dimension(150, 30));
        topPanel.add(exportTypeCombo);

        exportBtn = new JButton("导出到文件");
        styleButton(exportBtn, new Color(46, 139, 87), 150, 35);
        topPanel.add(exportBtn);

        panel.add(topPanel, BorderLayout.NORTH);

        // 中间进度区
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(new TitledBorder("导出状态"));

        exportProgressBar = new JProgressBar(0, 100);
        exportProgressBar.setStringPainted(true);
        exportProgressBar.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        centerPanel.add(exportProgressBar, BorderLayout.NORTH);

        exportStatusArea = new JTextArea();
        exportStatusArea.setEditable(false);
        exportStatusArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        exportStatusArea.setLineWrap(true);
        exportStatusArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(exportStatusArea);
        scrollPane.setPreferredSize(new Dimension(580, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // 事件绑定
        exportBtn.addActionListener(e -> performExport());

        return panel;
    }

    private void performExport() {
        // 选择保存路径
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择导出文件保存位置");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON 文件 (*.json)", "json"));
        fileChooser.setSelectedFile(new File(getDefaultExportFileName()));

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".json")) {
            filePath += ".json";
        }

        // 禁用按钮，防止重复操作
        exportBtn.setEnabled(false);
        exportProgressBar.setValue(0);
        exportStatusArea.setText("");
        String selectedType = (String) exportTypeCombo.getSelectedItem();

        // 使用 SwingWorker 后台执行
        final String finalFilePath = filePath;
        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try {
                    ProgressCallback callback = new ProgressCallback() {
                        @Override
                        public void onStart(String message, int total) {
                            publish("[开始] " + message);
                            SwingUtilities.invokeLater(() -> {
                                exportProgressBar.setMaximum(total > 0 ? total : 1);
                                exportProgressBar.setValue(0);
                            });
                        }

                        @Override
                        public void onProgress(int current, int total) {
                            publish("[进度] " + current + " / " + total);
                            SwingUtilities.invokeLater(() -> exportProgressBar.setValue(current));
                        }

                        @Override
                        public void onComplete(String message) {
                            publish("[完成] " + message);
                        }
                    };

                    switch (selectedType) {
                        case "图书信息":
                            return service.exportBooks(finalFilePath, callback);
                        case "借阅记录":
                            return service.exportBorrows(finalFilePath, callback);
                        case "读者信息":
                            return service.exportReaders(finalFilePath, callback);
                        case "图书分类":
                            return service.exportClasses(finalFilePath, callback);
                        default:
                            throw new IllegalArgumentException("未知的数据类型");
                    }
                } catch (IOException e) {
                    publish("[错误] " + e.getMessage());
                    return -1;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    exportStatusArea.append(msg + "\n");
                }
                exportStatusArea.setCaretPosition(exportStatusArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    if (count >= 0) {
                        exportProgressBar.setValue(exportProgressBar.getMaximum());
                        exportStatusArea.append("------ 导出成功，文件保存至: " + finalFilePath + " ------\n");
                    }
                } catch (Exception e) {
                    exportStatusArea.append("[错误] 导出失败: " + e.getMessage() + "\n");
                }
                exportBtn.setEnabled(true);
            }
        }.execute();
    }

    private String getDefaultExportFileName() {
        String type = (String) exportTypeCombo.getSelectedItem();
        String prefix;
        switch (type) {
            case "图书信息": prefix = "图书信息"; break;
            case "借阅记录": prefix = "借阅记录"; break;
            case "读者信息": prefix = "读者信息"; break;
            case "图书分类": prefix = "图书分类"; break;
            default: prefix = "导出数据";
        }
        return prefix + "_" + java.time.LocalDate.now() + ".json";
    }

    // ========================= 导入面板 =========================

    private JPanel createImportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部文件选择区
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 文件选择行
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel fileLabel = new JLabel("选择导入文件:");
        fileLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        filePanel.add(fileLabel);

        importFileField = new JTextField(30);
        importFileField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        importFileField.setEditable(false);
        filePanel.add(importFileField);

        chooseFileBtn = new JButton("浏览...");
        styleButton(chooseFileBtn, new Color(70, 130, 180), 90, 30);
        filePanel.add(chooseFileBtn);

        topPanel.add(filePanel);

        // 选项行
        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel typeLabel = new JLabel("检测类型:");
        typeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        optionPanel.add(typeLabel);

        importTypeLabel = new JLabel("(请先选择文件)");
        importTypeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        importTypeLabel.setForeground(Color.GRAY);
        optionPanel.add(importTypeLabel);

        optionPanel.add(Box.createHorizontalStrut(20));

        JLabel strategyLabel = new JLabel("重复数据处理:");
        strategyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        optionPanel.add(strategyLabel);

        duplicateStrategyCombo = new JComboBox<>(new String[]{"跳过已存在的记录", "更新已存在的记录"});
        duplicateStrategyCombo.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        duplicateStrategyCombo.setPreferredSize(new Dimension(180, 30));
        optionPanel.add(duplicateStrategyCombo);

        importBtn = new JButton("开始导入");
        styleButton(importBtn, new Color(220, 80, 60), 120, 35);
        optionPanel.add(importBtn);

        topPanel.add(optionPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // 中间进度区
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(new TitledBorder("导入状态"));

        importProgressBar = new JProgressBar(0, 100);
        importProgressBar.setStringPainted(true);
        importProgressBar.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        centerPanel.add(importProgressBar, BorderLayout.NORTH);

        importStatusArea = new JTextArea();
        importStatusArea.setEditable(false);
        importStatusArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        importStatusArea.setLineWrap(true);
        importStatusArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(importStatusArea);
        scrollPane.setPreferredSize(new Dimension(580, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // 事件绑定
        chooseFileBtn.addActionListener(e -> chooseImportFile());
        importBtn.addActionListener(e -> performImport());

        return panel;
    }

    private void chooseImportFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要导入的 JSON 文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON 文件 (*.json)", "json"));

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(this,
                    "无法读取文件: " + file.getAbsolutePath() + "\n请检查文件是否存在且有读取权限。",
                    "文件读取失败", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedImportFilePath = file.getAbsolutePath();
        importFileField.setText(selectedImportFilePath);

        // 尝试检测文件类型
        detectFileType(selectedImportFilePath);
    }

    private void detectFileType(String filePath) {
        try {
            java.io.Reader reader = new java.io.InputStreamReader(
                    new java.io.FileInputStream(filePath), StandardCharsets.UTF_8);
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseReader(reader);
            reader.close();

            if (element.isJsonObject()) {
                com.google.gson.JsonObject root = element.getAsJsonObject();
                if (root.has("type")) {
                    String type = root.get("type").getAsString();
                    String typeLabel;
                    switch (type) {
                        case "books": typeLabel = "图书信息"; break;
                        case "borrows": typeLabel = "借阅记录"; break;
                        case "readers": typeLabel = "读者信息"; break;
                        case "classes": typeLabel = "图书分类"; break;
                        default: typeLabel = "未知类型: " + type;
                    }
                    importTypeLabel.setText(typeLabel);
                    importTypeLabel.setForeground(new Color(46, 139, 87));
                }
            }
        } catch (Exception e) {
            importTypeLabel.setText("无法识别文件类型");
            importTypeLabel.setForeground(Color.RED);
        }
    }

    private void performImport() {
        if (selectedImportFilePath == null || selectedImportFilePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先选择一个要导入的 JSON 文件。",
                    "未选择文件", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File file = new File(selectedImportFilePath);
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(this,
                    "无法读取文件，请检查文件是否存在且有读取权限。",
                    "文件读取失败", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 确认对话框
        String strategyText = (String) duplicateStrategyCombo.getSelectedItem();
        int confirm = JOptionPane.showConfirmDialog(this,
                "即将导入文件: " + file.getName() + "\n\n重复数据处理策略: " + strategyText +
                "\n\n确定开始导入吗？",
                "确认导入", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 禁用按钮
        importBtn.setEnabled(false);
        chooseFileBtn.setEnabled(false);
        importProgressBar.setValue(0);
        importStatusArea.setText("");

        String duplicateStrategy = strategyText.contains("更新") ? "update" : "skip";

        new SwingWorker<ImportResult, String>() {
            @Override
            protected ImportResult doInBackground() throws Exception {
                ProgressCallback callback = new ProgressCallback() {
                    @Override
                    public void onStart(String message, int total) {
                        publish("[开始] " + message);
                        SwingUtilities.invokeLater(() -> {
                            importProgressBar.setMaximum(total > 0 ? total : 1);
                            importProgressBar.setValue(0);
                        });
                    }

                    @Override
                    public void onProgress(int current, int total) {
                        if (current % 5 == 0 || current == total) {
                            publish("[进度] " + current + " / " + total);
                            SwingUtilities.invokeLater(() -> importProgressBar.setValue(current));
                        }
                    }

                    @Override
                    public void onComplete(String message) {
                        publish("[完成] " + message);
                    }
                };

                return service.importData(selectedImportFilePath, duplicateStrategy, callback);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    importStatusArea.append(msg + "\n");
                }
                importStatusArea.setCaretPosition(importStatusArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                try {
                    ImportResult result = get();
                    importProgressBar.setValue(importProgressBar.getMaximum());
                    importStatusArea.append("\n" + result.getSummary() + "\n");

                    // 根据结果显示不同图标
                    if (result.getFailCount() == 0) {
                        JOptionPane.showMessageDialog(ImportExportDialog.this,
                                "导入完成!\n\n" +
                                "成功: " + result.getSuccessCount() + " 条\n" +
                                "跳过(重复): " + result.getSkipCount() + " 条\n" +
                                "失败: " + result.getFailCount() + " 条",
                                "导入完成", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ImportExportDialog.this,
                                "导入完成，但有部分记录处理失败。\n\n" +
                                "成功: " + result.getSuccessCount() + " 条\n" +
                                "跳过(重复): " + result.getSkipCount() + " 条\n" +
                                "失败: " + result.getFailCount() + " 条\n\n" +
                                "请查看状态区域获取详细信息。",
                                "导入完成(有错误)", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    importStatusArea.append("[错误] 导入失败: " + e.getMessage() + "\n");
                    JOptionPane.showMessageDialog(ImportExportDialog.this,
                            "导入过程发生异常:\n" + e.getMessage(),
                            "导入失败", JOptionPane.ERROR_MESSAGE);
                }
                importBtn.setEnabled(true);
                chooseFileBtn.setEnabled(true);
            }
        }.execute();
    }

    // ========================= 样式工具方法 =========================

    private void styleButton(JButton btn, Color bgColor, int width, int height) {
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
