package ui;

import client.ClientNetworkService;
import com.google.gson.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 导入导出对话框 — 实验九 C/S 模式
 * 导出：客户端请求服务器获取 JSON → 客户端写文件
 * 导入：客户端读文件 → 发送 JSON 到服务器 → 服务器入库
 */
public class ImportExportDialog extends JDialog {

    private final JFrame parent;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        super(parent, "图书信息导入 / 导出 (C/S模式)", true);
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

    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel label = new JLabel("从服务器导出全部图书信息为 JSON 文件");
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
            protected Integer doInBackground() {
                try {
                    publish("[开始] 正在从服务器获取图书数据...");
                    SwingUtilities.invokeLater(() -> exportBar.setIndeterminate(true));

                    // 实验九：从服务器获取导出数据
                    String jsonData = ClientNetworkService.getInstance().exportBooksJson();
                    if (jsonData == null) {
                        publish("[错误] 从服务器获取数据失败");
                        return -1;
                    }

                    publish("[进度] 正在写入文件: " + filePath);
                    try (Writer w = new OutputStreamWriter(
                            new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
                        w.write(jsonData);
                    }

                    publish("[完成] 图书信息导出完成");
                    return 1;
                } catch (IOException e) {
                    publish("[错误] " + e.getMessage());
                    return -1;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) exportArea.append(s + "\n");
            }

            @Override
            protected void done() {
                exportBar.setIndeterminate(false);
                try {
                    if (get() >= 0) {
                        exportBar.setValue(100);
                        exportArea.append("------ 导出成功: " + filePath + " ------\n");
                    }
                } catch (Exception e) {
                    exportArea.append("[错误] " + e.getMessage() + "\n");
                }
                exportBtn.setEnabled(true);
            }
        }.execute();
    }

    private JPanel createImportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

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

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        importBtn = styleBtn("开始导入", new Color(220, 80, 60), 120, 35);
        row2.add(importBtn);

        JLabel hint = new JLabel("重复图书自动更新，通过服务器入库");
        hint.setFont(new Font("微软雅黑", Font.ITALIC, 12));
        hint.setForeground(Color.GRAY);
        row2.add(hint);

        top.add(row1);
        top.add(Box.createVerticalStrut(2));
        top.add(row2);
        panel.add(top, BorderLayout.NORTH);

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

        if (JOptionPane.showConfirmDialog(this,
                "即将导入: " + new File(selectedFile).getName() +
                "\n重复图书将自动更新。\n数据通过服务器入库。\n\n确定开始？",
                "确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        importBtn.setEnabled(false);
        chooseFileBtn.setEnabled(false);
        importBar.setValue(0);
        importArea.setText("");

        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() {
                try {
                    publish("[开始] 正在读取 JSON 文件...");
                    JsonArray books;
                    try (Reader r = new InputStreamReader(new FileInputStream(selectedFile), StandardCharsets.UTF_8)) {
                        JsonObject root = GSON.fromJson(r, JsonObject.class);
                        if (root == null || !root.has("data")) throw new IOException("无效的 JSON 格式");
                        JsonElement data = root.get("data");
                        books = data.isJsonArray() ? data.getAsJsonArray()
                                : data.getAsJsonObject().getAsJsonArray("books");
                        if (books == null || books.size() == 0) throw new IOException("文件中没有图书数据");
                    }

                    publish("[进度] 共 " + books.size() + " 条记录，发送到服务器...");
                    importBar.setMaximum(books.size());
                    importBar.setValue(0);

                    // 实验九：发送到服务器进行导入
                    JsonObject result = ClientNetworkService.getInstance().importBooks(books);
                    if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
                        JsonObject data = result.getAsJsonObject("data");
                        int newCount = data.get("newCount").getAsInt();
                        int updateCount = data.get("updateCount").getAsInt();
                        int failCount = data.get("failCount").getAsInt();
                        int total = data.get("total").getAsInt();

                        publish("[完成] 导入完成: 新增 " + newCount + " 条, 更新 " + updateCount
                                + " 条, 失败 " + failCount + " 条, 共 " + total + " 条");

                        if (data.has("errors") && data.get("errors").getAsJsonArray().size() > 0) {
                            publish("------ 错误详情 ------");
                            for (JsonElement e : data.getAsJsonArray("errors")) {
                                publish("  " + e.getAsString());
                            }
                        }
                        return "成功";
                    } else {
                        publish("[错误] 服务器导入失败");
                        return "失败";
                    }
                } catch (Exception e) {
                    publish("[错误] " + e.getMessage());
                    return "异常: " + e.getMessage();
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) importArea.append(s + "\n");
            }

            @Override
            protected void done() {
                importBar.setValue(importBar.getMaximum());
                importBtn.setEnabled(true);
                chooseFileBtn.setEnabled(true);
                try {
                    String result = get();
                    JOptionPane.showMessageDialog(ImportExportDialog.this,
                            "导入" + ("成功".equals(result) ? "完成!" : "失败: " + result),
                            "完成", "成功".equals(result)
                                    ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.WARNING_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ImportExportDialog.this,
                            "导入失败:\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

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
