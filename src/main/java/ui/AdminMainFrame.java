package ui;

import model.User;

import javax.swing.*;
import java.awt.*;

public class AdminMainFrame extends JFrame {

    private final User admin;

    public AdminMainFrame(User admin) {
        this.admin = admin;

        setTitle("图书管理系统 - 管理员:" + admin.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        JLabel titleLabel = new JLabel("管理员管理面板");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;

        JButton bookBtn = new JButton("图书管理");
        styleButton(bookBtn);
        panel.add(bookBtn, gbc);

        gbc.gridx = 1;
        JButton readerBtn = new JButton("读者管理");
        styleButton(readerBtn);
        panel.add(readerBtn, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JButton borrowBtn = new JButton("借阅管理");
        styleButton(borrowBtn);
        panel.add(borrowBtn, gbc);

        gbc.gridx = 1;
        JButton logoutBtn = new JButton("退出登录");
        styleButton(logoutBtn);
        panel.add(logoutBtn, gbc);

        bookBtn.addActionListener(e -> new BookManageDialog(this).setVisible(true));
        readerBtn.addActionListener(e -> new ReaderManageDialog(this).setVisible(true));
        borrowBtn.addActionListener(e -> new BorrowDialog(this, null).setVisible(true));
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        setContentPane(panel);
    }

    private void styleButton(JButton btn) {
        btn.setPreferredSize(new Dimension(150, 60));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
    }
}
