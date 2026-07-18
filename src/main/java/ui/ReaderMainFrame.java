package ui;

import chat.client.ChatClient;
import chat.ui.ChatMainFrame;
import client.ClientNetworkService;
import model.User;

import javax.swing.*;
import java.awt.*;

/**
 * 读者主界面
 */
public class ReaderMainFrame extends JFrame {

    private final User reader;
    private ServerMonitorDialog monitorDialog;
    private ChatMainFrame chatFrame;

    public ReaderMainFrame(User reader) {
        this.reader = reader;

        setTitle("图书管理系统 - 读者:" + reader.getUsername() + " (V8.0)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 480);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        JLabel titleLabel = new JLabel("读者服务面板");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        gbc.gridy = 1; gbc.gridx = 0;
        JButton borrowBtn = new JButton("借阅图书");
        styleButton(borrowBtn);
        panel.add(borrowBtn, gbc);

        gbc.gridx = 1;
        JButton myBorrowBtn = new JButton("我的借阅");
        styleButton(myBorrowBtn);
        panel.add(myBorrowBtn, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JButton serverBtn = new JButton("服务器监视");
        styleButton(serverBtn);
        panel.add(serverBtn, gbc);

        gbc.gridx = 1;
        JButton chatBtn = new JButton("即时通信");
        styleButton(chatBtn);
        panel.add(chatBtn, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        JButton logoutBtn = new JButton("退出登录");
        styleButton(logoutBtn);
        panel.add(logoutBtn, gbc);

        borrowBtn.addActionListener(e -> new BookBrowseDialog(this, reader).setVisible(true));
        myBorrowBtn.addActionListener(e -> new MyBorrowDialog(this, reader).setVisible(true));
        serverBtn.addActionListener(e -> openServerMonitor());
        chatBtn.addActionListener(e -> openChat());
        logoutBtn.addActionListener(e -> {
            if (monitorDialog != null) { monitorDialog.shutdownServer(); monitorDialog.dispose(); }
            if (chatFrame != null) { chatFrame.dispose(); ChatClient.getInstance().disconnect(); }
            dispose();
            new LoginFrame().setVisible(true);
        });

        setContentPane(panel);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (monitorDialog != null) { monitorDialog.shutdownServer(); monitorDialog.dispose(); }
                if (chatFrame != null) { chatFrame.dispose(); ChatClient.getInstance().disconnect(); }
            }
        });
    }

    private void openServerMonitor() {
        if (monitorDialog == null) monitorDialog = new ServerMonitorDialog(this);
        monitorDialog.setVisible(true);
        monitorDialog.toFront();
    }

    private void openChat() {
        if (chatFrame == null || !chatFrame.isDisplayable()) {
            ChatClient.getInstance().setHost(ClientNetworkService.getInstance().getHost());
            ChatClient.getInstance().setPort(9000);
            chatFrame = new ChatMainFrame(reader.getId(), reader.getUsername());
        }
        if (!chatFrame.isVisible()) {
            new Thread(() -> {
                try {
                    ChatClient.getInstance().connect(reader.getUsername(), reader.getId());
                    chatFrame.onConnected();
                } catch (Exception ex) {
                    System.err.println("IM连接失败: " + ex.getMessage());
                }
                SwingUtilities.invokeLater(() -> chatFrame.setVisible(true));
            }).start();
        }
        chatFrame.toFront();
    }

    private void styleButton(JButton btn) {
        btn.setPreferredSize(new Dimension(160, 60));
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
    }
}
