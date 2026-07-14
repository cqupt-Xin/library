package ui;

import model.User;

import javax.swing.*;
import java.awt.*;

public class ReaderMainFrame extends JFrame {

    private final User reader;
    private ServerMonitorDialog monitorDialog;

    public ReaderMainFrame(User reader) {
        this.reader = reader;

        setTitle("图书管理系统 - 读者:" + reader.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        JLabel titleLabel = new JLabel("读者服务面板");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        // Row 1: 借阅图书 | 我的借阅
        gbc.gridy = 1; gbc.gridx = 0;
        JButton borrowBtn = new JButton("借阅图书");
        styleButton(borrowBtn);
        panel.add(borrowBtn, gbc);

        gbc.gridx = 1;
        JButton myBorrowBtn = new JButton("我的借阅");
        styleButton(myBorrowBtn);
        panel.add(myBorrowBtn, gbc);

        // Row 2: 启动服务器 | 退出登录
        gbc.gridy = 2; gbc.gridx = 0;
        JButton serverBtn = new JButton("启动服务器");
        styleButton(serverBtn);
        serverBtn.setToolTipText("启动本地 TCP 服务器，等待 NetAssist 客户端连接");
        panel.add(serverBtn, gbc);

        gbc.gridx = 1;
        JButton logoutBtn = new JButton("退出登录");
        styleButton(logoutBtn);
        panel.add(logoutBtn, gbc);

        // — 事件绑定 —
        borrowBtn.addActionListener(e -> new BookBrowseDialog(this, reader).setVisible(true));
        myBorrowBtn.addActionListener(e -> new MyBorrowDialog(this, reader).setVisible(true));
        serverBtn.addActionListener(e -> openServerMonitor());
        logoutBtn.addActionListener(e -> {
            if (monitorDialog != null) {
                monitorDialog.shutdownServer();
                monitorDialog.dispose();
            }
            dispose();
            new LoginFrame().setVisible(true);
        });

        setContentPane(panel);
    }

    private void openServerMonitor() {
        if (monitorDialog == null) {
            monitorDialog = new ServerMonitorDialog(this);
        }
        monitorDialog.setVisible(true);
        monitorDialog.toFront();
    }

    private void styleButton(JButton btn) {
        btn.setPreferredSize(new Dimension(160, 60));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
    }
}
