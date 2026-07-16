package ui;

import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 管理员主界面 — 实验九 C/S 模式
 * 所有操作通过 ClientNetworkService 与后台服务器通信
 */
public class AdminMainFrame extends JFrame {

    private final User admin;
    private ServerMonitorDialog monitorDialog;

    public AdminMainFrame(User admin) {
        this.admin = admin;

        setTitle("图书管理系统 - 管理员:" + admin.getUsername() + " (C/S模式 V8.0)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 460);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        mainPanel.add(createTitleLabel(), BorderLayout.NORTH);
        mainPanel.add(createButtonGrid(), BorderLayout.CENTER);

        setContentPane(mainPanel);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (monitorDialog != null) {
                    monitorDialog.shutdownServer();
                    monitorDialog.dispose();
                }
            }
        });
    }

    private JLabel createTitleLabel() {
        JLabel label = new JLabel("管理员操作面板", JLabel.CENTER);
        label.setFont(new Font("微软雅黑", Font.BOLD, 22));
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }

    private JPanel createButtonGrid() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.fill = GridBagConstraints.BOTH;

        JButton bookBtn       = makeButton("图书管理");
        JButton readerBtn     = makeButton("读者管理");
        JButton borrowBtn     = makeButton("借阅管理");
        JButton importExportBtn = makeButton("导入 / 导出");
        JButton monitorBtn    = makeButton("服务器监视");
        JButton logoutBtn     = makeButton("退出登录");

        Dimension btnSize = new Dimension(180, 75);
        Font btnFont = new Font("微软雅黑", Font.BOLD, 18);
        for (JButton btn : new JButton[]{bookBtn, readerBtn, borrowBtn,
                                          importExportBtn, monitorBtn, logoutBtn}) {
            btn.setPreferredSize(btnSize);
            btn.setFont(btnFont);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        gbc.gridy = 0;
        gbc.gridx = 0; panel.add(bookBtn, gbc);
        gbc.gridx = 1; panel.add(readerBtn, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; panel.add(borrowBtn, gbc);
        gbc.gridx = 1; panel.add(importExportBtn, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0; panel.add(monitorBtn, gbc);
        gbc.gridx = 1; panel.add(logoutBtn, gbc);

        bookBtn.addActionListener(e -> new BookManageDialog(this).setVisible(true));
        readerBtn.addActionListener(e -> new ReaderManageDialog(this).setVisible(true));
        borrowBtn.addActionListener(e -> new BorrowDialog(this, null).setVisible(true));
        importExportBtn.addActionListener(e -> new ImportExportDialog(this).setVisible(true));
        monitorBtn.addActionListener(e -> openMonitorDialog());
        logoutBtn.addActionListener(e -> {
            if (monitorDialog != null) {
                monitorDialog.shutdownServer();
                monitorDialog.dispose();
            }
            dispose();
            new LoginFrame().setVisible(true);
        });

        return panel;
    }

    private JButton makeButton(String text) {
        return new JButton(text);
    }

    private void openMonitorDialog() {
        if (monitorDialog == null) {
            monitorDialog = new ServerMonitorDialog(this);
        }
        monitorDialog.setVisible(true);
        monitorDialog.toFront();
    }
}
