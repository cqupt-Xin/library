package ui;

import model.User;
import server.BookCommandServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 管理员主界面
 * 包含：功能导航按钮 + TCP 远程命令服务器控制面板
 */
public class AdminMainFrame extends JFrame {

    private final User admin;
    private BookCommandServer server;
    private JButton serverToggleBtn;
    private JLabel serverStatusLabel;
    private JSpinner portSpinner;
    private JTextArea serverLogArea;

    public AdminMainFrame(User admin) {
        this.admin = admin;

        setTitle("图书管理系统 - 管理员:" + admin.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(createFunctionPanel(), BorderLayout.NORTH);
        mainPanel.add(createServerPanel(), BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    // ==================== 功能导航按钮区 ====================

    private JPanel createFunctionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("功能导航"));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton bookBtn = new JButton("图书管理");
        JButton readerBtn = new JButton("读者管理");
        JButton borrowBtn = new JButton("借阅管理");
        JButton importExportBtn = new JButton("导入 / 导出");
        JButton logoutBtn = new JButton("退出登录");

        Dimension btnSize = new Dimension(140, 50);
        Font btnFont = new Font("微软雅黑", Font.PLAIN, 14);
        for (JButton btn : new JButton[]{bookBtn, readerBtn, borrowBtn, importExportBtn, logoutBtn}) {
            btn.setPreferredSize(btnSize);
            btn.setFont(btnFont);
        }

        btnPanel.add(bookBtn);
        btnPanel.add(readerBtn);
        btnPanel.add(borrowBtn);
        btnPanel.add(importExportBtn);
        btnPanel.add(logoutBtn);
        panel.add(btnPanel, BorderLayout.CENTER);

        bookBtn.addActionListener(e -> new BookManageDialog(this).setVisible(true));
        readerBtn.addActionListener(e -> new ReaderManageDialog(this).setVisible(true));
        borrowBtn.addActionListener(e -> new BorrowDialog(this, null).setVisible(true));
        importExportBtn.addActionListener(e -> new ImportExportDialog(this).setVisible(true));
        logoutBtn.addActionListener(e -> {
            if (server != null && server.isRunning()) server.stop();
            dispose();
            new LoginFrame().setVisible(true);
        });

        return panel;
    }

    // ==================== TCP 远程服务器面板 ====================

    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("TCP 远程命令服务器（支持 NetAssist 连接）"));

        // —— 控制栏 ——
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        controlBar.add(new JLabel("监听端口:"));

        SpinnerNumberModel portModel = new SpinnerNumberModel(8888, 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        portSpinner.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        portSpinner.setPreferredSize(new Dimension(80, 28));
        controlBar.add(portSpinner);

        serverToggleBtn = new JButton("启动服务器");
        serverToggleBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        serverToggleBtn.setBackground(new Color(46, 139, 87));
        serverToggleBtn.setForeground(Color.WHITE);
        serverToggleBtn.setFocusPainted(false);
        serverToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        controlBar.add(serverToggleBtn);

        serverStatusLabel = new JLabel("● 已停止");
        serverStatusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        serverStatusLabel.setForeground(Color.GRAY);
        controlBar.add(serverStatusLabel);

        JButton clearLogBtn = new JButton("清空日志");
        clearLogBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        controlBar.add(clearLogBtn);

        panel.add(controlBar, BorderLayout.NORTH);

        // —— 日志区 ——
        serverLogArea = new JTextArea();
        serverLogArea.setEditable(false);
        serverLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        serverLogArea.setBackground(new Color(30, 30, 30));
        serverLogArea.setForeground(new Color(0, 255, 0));
        JScrollPane scrollPane = new JScrollPane(serverLogArea);
        scrollPane.setPreferredSize(new Dimension(560, 250));
        panel.add(scrollPane, BorderLayout.CENTER);

        // —— 事件绑定 ——
        serverToggleBtn.addActionListener(e -> toggleServer());
        clearLogBtn.addActionListener(e -> serverLogArea.setText(""));

        return panel;
    }

    /**
     * 切换服务器启停状态（等待端口绑定成功后才更新 UI）
     */
    private void toggleServer() {
        if (server != null && server.isRunning()) {
            // ===== 停止服务器 =====
            server.stop();
            server = null;
            updateUIState(false, 0);
        } else {
            // ===== 启动服务器 =====
            int port = (int) portSpinner.getValue();

            // 先置为"启动中"状态
            serverToggleBtn.setEnabled(false);
            serverToggleBtn.setText("启动中...");
            serverStatusLabel.setText("◉ 正在绑定端口...");
            serverStatusLabel.setForeground(Color.ORANGE);
            portSpinner.setEnabled(false);
            serverLogArea.append("========================================\n");

            server = new BookCommandServer(port);
            server.start(serverLogArea, success -> {
                // 绑定完成后回调（在 EDT 执行）
                serverToggleBtn.setEnabled(true);
                if (success) {
                    updateUIState(true, port);
                } else {
                    // 绑定失败，恢复初始状态
                    server = null;
                    updateUIState(false, 0);
                }
            });
        }
    }

    /**
     * 更新 UI 状态显示
     */
    private void updateUIState(boolean running, int port) {
        if (running) {
            serverToggleBtn.setText("停止服务器");
            serverToggleBtn.setBackground(new Color(220, 80, 60));
            serverStatusLabel.setText("● 运行中 (端口 " + port + ")");
            serverStatusLabel.setForeground(new Color(46, 139, 87));
            portSpinner.setEnabled(false);
        } else {
            serverToggleBtn.setText("启动服务器");
            serverToggleBtn.setBackground(new Color(46, 139, 87));
            serverStatusLabel.setText("● 已停止");
            serverStatusLabel.setForeground(Color.GRAY);
            portSpinner.setEnabled(true);
        }
    }
}
