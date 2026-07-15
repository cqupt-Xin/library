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

/**
 * 图书信息导入/导出对话框
 * 仅支持图书信息的 JSON 导入导出，重复数据自动更新123
 */
public class ImportExportDialog extends JDialog {

    private final JFrame parent;
    private final ImportExportService service = new ImportExportService();

    private JProgressBar exportBar;
    private JTextArea exportArea;
    private JButton exportBtn;

    private JTextField importFileField;
    private JProgressBar importBar;
    private JTextArea importArea;
    private JButton importBtn;
    private JButton chooseFileBtn;

    private String selectedFile;

    public ImportExportDialog(JFrame parent) {
        super(parent, "图书信息导入 / 导出", true);
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setSize(600, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        tabs.addTab("导出图书", createExportPanel());
        tabs.addTab("导入图书", createImportPanel());
        setContentPane(tabs);
    }

    // ========================= 导出面板 =========================

    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel label = new JLabel("导出全部图书信息为 JSON 文件");
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        top.add(label);

        exportBtn = styleBtn("导出到文件", new Color(46, 139, 87), 140, 35);
        top.add(exportBtn);
        panel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new TitledBorder("导出状态"));

        exportBar = new JProgressBar(0, 100);
        exportBar.setStringPainted(true);
        exportBar.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        center.add(exportBar, BorderLayout.NORTH);

        exportArea = createStatusArea();
        center.add(new JScrollPane(exportArea), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        exportBtn.addActionListener(e -> doExport());
        return panel;
    }

    private void doExport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择导出位置");
        fc.setFileFilter(new FileNameExtensionFilter("JSON 文件 (*.json)", "json"));
        fc.setSelectedFile(new File("图书信息_" + java.time.LocalDate.now() + ".json"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".json")) path += ".json";
        final String filePath = path;

        exportBtn.setEnabled(false);
        exportBar.setValue(0);
        exportArea.setText("");

        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try {
                    return service.exportBooks(filePath, new ProgressCallback() {
                        public void onStart(String msg, int total) {
                            publish("[开始] " + msg);
                            SwingUtilities.invokeLater(() -> {
                                exportBar.setMaximum(total > 0 ? total : 1);
                                exportBar.setValue(0);
                            });
                        }
                        public void onProgress(int cur, int total) {
                            publish("[进度] " + cur + " / " + total);
                            SwingUtilities.invokeLater(() -> exportBar.setValue(cur));
                        }
                        public void onComplete(String msg) { publish("[完成] " + msg); }
                    });
                } catch (IOException e) { publish("[错误] " + e.getMessage()); return -1; }
            }
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) exportArea.append(s + "\n");
            }
            @Override
            protected void done() {
                try {
                    if (get() >= 0) {
                        exportBar.setValue(exportBar.getMaximum());
                        exportArea.append("------ 导出成功: " + filePath + " ------\n");
                    }
                } catch (Exception e) { exportArea.append("[错误] " + e.getMessage() + "\n"); }
                exportBtn.setEnabled(true);
            }
        }.execute();
    }

    // ========================= 导入面板 =========================

    private JPanel createImportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部：两行独立布局，避免按钮被底部容器裁切
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // 行1：文件选择
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JLabel fileLabel = new JLabel("选择文件:");
        fileLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        row1.add(fileLabel);

        importFileField = new JTextField(28);
        importFileField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        importFileField.setEditable(false);
        row1.add(importFileField);

        chooseFileBtn = styleBtn("浏览...", new Color(70, 130, 180), 90, 30);
        row1.add(chooseFileBtn);

        // 行2：导入按钮 + 提示
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));

        importBtn = styleBtn("开始导入", new Color(220, 80, 60), 120, 35);
        row2.add(importBtn);

        JLabel hint = new JLabel("重复图书自动更新");
        hint.setFont(new Font("微软雅黑", Font.ITALIC, 12));
        hint.setForeground(Color.GRAY);
        row2.add(hint);

        top.add(row1);
        top.add(Box.createVerticalStrut(2));
        top.add(row2);
        panel.add(top, BorderLayout.NORTH);

        // 状态区
        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new TitledBorder("导入状态"));

        importBar = new JProgressBar(0, 100);
        importBar.setStringPainted(true);
        importBar.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        center.add(importBar, BorderLayout.NORTH);

        importArea = createStatusArea();
        center.add(new JScrollPane(importArea), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        chooseFileBtn.addActionListener(e -> chooseFile());
        importBtn.addActionListener(e -> doImport());
        return panel;
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择 JSON 文件");
        fc.setFileFilter(new FileNameExtensionFilter("JSON 文件 (*.json)", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.exists() || !f.canRead()) {
            JOptionPane.showMessageDialog(this, "无法读取文件: " + f.getAbsolutePath(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedFile = f.getAbsolutePath();
        importFileField.setText(selectedFile);
    }

    private void doImport() {
        if (selectedFile == null || selectedFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择 JSON 文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!new File(selectedFile).canRead()) {
            JOptionPane.showMessageDialog(this, "文件无法读取", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "即将导入: " + new File(selectedFile).getName() +
                "\n重复图书将自动更新。\n\n确定开始？",
                "确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        importBtn.setEnabled(false);
        chooseFileBtn.setEnabled(false);
        importBar.setValue(0);
        importArea.setText("");

        new SwingWorker<ImportResult, String>() {
            @Override
            protected ImportResult doInBackground() throws Exception {
                ProgressCallback cb = new ProgressCallback() {
                    public void onStart(String msg, int total) {
                        publish("[开始] " + msg);
                        SwingUtilities.invokeLater(() -> {
                            importBar.setMaximum(total > 0 ? total : 1);
                            importBar.setValue(0);
                        });
                    }
                    public void onProgress(int cur, int total) {
                        if (cur % 5 == 0 || cur == total) {
                            publish("[进度] " + cur + " / " + total);
                            SwingUtilities.invokeLater(() -> importBar.setValue(cur));
                        }
                    }
                    public void onComplete(String msg) { publish("[完成] " + msg); }
                };
                return service.importBooks(selectedFile, cb);
            }
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) importArea.append(s + "\n");
            }
            @Override
            protected void done() {
                try {
                    ImportResult r = get();
                    importBar.setValue(importBar.getMaximum());
                    importArea.append("\n" + r.getSummary() + "\n");
                    JOptionPane.showMessageDialog(ImportExportDialog.this,
                            "导入完成!\n\n新增: " + (r.getSuccessCount() - r.getUpdateCount()) +
                            " 条\n更新: " + r.getUpdateCount() + " 条\n失败: " + r.getFailCount() + " 条",
                            "完成", r.getFailCount() == 0
                                    ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.WARNING_MESSAGE);
                } catch (Exception e) {
                    importArea.append("[错误] " + e.getMessage() + "\n");
                    JOptionPane.showMessageDialog(ImportExportDialog.this,
                            "导入失败:\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
                importBtn.setEnabled(true);
                chooseFileBtn.setEnabled(true);
            }
        }.execute();
    }

    // ========================= 工具 =========================

    private JTextArea createStatusArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JButton styleBtn(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(4, 12, 4, 12));
        return btn;
    }
}