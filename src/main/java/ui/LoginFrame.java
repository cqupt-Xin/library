package ui;

import client.ClientNetworkService;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 客户端登录界面 — 实验九 C/S 模式
 * 纯客户端，连接外部 TCP 服务器，不含任何数据库/服务端逻辑
 *
 * 运行方式：java ui.LoginFrame [服务器地址] [端口]
 * 默认连接：127.0.0.1:8888
 */
public class LoginFrame extends JFrame {

    private JTextField idField;
    private JPasswordField passwordField;
    private JLabel serverStatusLabel;

    public LoginFrame() {
        initUI();
        // 启动异步检测服务器连通性
        checkServerAsync();
    }

    private void initUI() {
        setTitle("图书管理系统 - 客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(420, 420);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 25, 10, 25);

        // 标题
        JLabel titleLabel = new JLabel("图书管理系统");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 26));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 25, 15, 25);
        panel.add(titleLabel, gbc);

        // 服务器状态
        ClientNetworkService cns = ClientNetworkService.getInstance();
        serverStatusLabel = new JLabel("服务器: " + cns.getHost() + ":" + cns.getPort() + "  检测中...");
        serverStatusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        serverStatusLabel.setForeground(Color.GRAY);
        gbc.insets = new Insets(0, 25, 10, 25);
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(serverStatusLabel, gbc);

        // ID
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.gridwidth = 1;
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel idLabel = new JLabel("ID:");
        idLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        panel.add(idLabel, gbc);
        idField = new JTextField(18);
        idField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(idField, gbc);

        // 密码
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel pwLabel = new JLabel("密码:");
        pwLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        panel.add(pwLabel, gbc);
        passwordField = new JPasswordField(18);
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // 按钮行
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginBtn = new JButton("登录");
        loginBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        loginBtn.setPreferredSize(new Dimension(100, 40));

        JButton registerBtn = new JButton("注册");
        registerBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        registerBtn.setPreferredSize(new Dimension(100, 40));

        JButton configBtn = new JButton("服务器设置");
        configBtn.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        configBtn.setPreferredSize(new Dimension(120, 40));

        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 25, 5, 25);
        panel.add(btnPanel, gbc);

        // 服务器设置按钮（单独一行）
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 25, 10, 25);
        panel.add(configBtn, gbc);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        configBtn.addActionListener(e -> showConfigDialog());

        // 回车键登录
        passwordField.addActionListener(e -> doLogin());

        setContentPane(panel);
        setLocationRelativeTo(null);
    }

    /**
     * 检测服务器连通性
     */
    private void checkServerAsync() {
        new Thread(() -> {
            ClientNetworkService cns = ClientNetworkService.getInstance();
            boolean ok = cns.ping();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    serverStatusLabel.setText("服务器: " + cns.getHost() + ":" + cns.getPort()
                            + "  \u25CF 已连接");
                    serverStatusLabel.setForeground(new Color(46, 139, 87));
                } else {
                    serverStatusLabel.setText("服务器: " + cns.getHost() + ":" + cns.getPort()
                            + "  \u2717 未连接");
                    serverStatusLabel.setForeground(new Color(220, 80, 60));
                }
            });
        }, "ServerCheck").start();
    }

    /**
     * 服务器地址配置对话框
     */
    private void showConfigDialog() {
        JTextField hostField = new JTextField(ClientNetworkService.getInstance().getHost(), 15);
        JTextField portField = new JTextField(String.valueOf(ClientNetworkService.getInstance().getPort()), 6);
        Font f = new Font("微软雅黑", Font.PLAIN, 13);
        hostField.setFont(f);
        portField.setFont(f);

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("服务器地址:"));
        panel.add(hostField);
        panel.add(new JLabel("端口:"));
        panel.add(portField);

        int r = JOptionPane.showConfirmDialog(this, panel, "服务器设置",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "地址和端口不能为空");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            ClientNetworkService.getInstance().setHost(host);
            ClientNetworkService.getInstance().setPort(port);
            serverStatusLabel.setText("服务器: " + host + ":" + port + "  检测中...");
            serverStatusLabel.setForeground(Color.GRAY);
            checkServerAsync();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "端口号格式不正确");
        }
    }

    /**
     * 登录 — 通过 TCP 发送命令
     */
    private void doLogin() {
        String idStr = idField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (idStr.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID和密码不能为空");
            return;
        }

        try {
            User user = ClientNetworkService.getInstance().login(idStr, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "登录失败，请检查ID和密码");
                return;
            }

            dispose();
            if ("admin".equals(user.getRole())) {
                new AdminMainFrame(user).setVisible(true);
            } else {
                new ReaderMainFrame(user).setVisible(true);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "无法连接服务器: " + e.getMessage() + "\n\n请确认:\n"
                    + "1. 服务器程序已启动 (java server.ServerMain)\n"
                    + "2. 服务器地址和端口设置正确\n"
                    + "3. 防火墙未阻止连接");
            serverStatusLabel.setText("服务器: "
                    + ClientNetworkService.getInstance().getHost() + ":"
                    + ClientNetworkService.getInstance().getPort() + "  \u2717 未连接");
            serverStatusLabel.setForeground(new Color(220, 80, 60));
        }
    }

    /**
     * 注册 — 通过 TCP 发送命令
     */
    private void doRegister() {
        JTextField nameField = new JTextField(10);
        JComboBox<String> sexCombo = new JComboBox<>(new String[]{"男", "女"});
        JTextField birthField = new JTextField("1990-01-01", 10);
        JTextField addrField = new JTextField(10);
        JTextField telField = new JTextField(10);
        JPasswordField pwField = new JPasswordField(10);
        JPasswordField confirmField = new JPasswordField(10);

        Font f = new Font("微软雅黑", Font.PLAIN, 13);
        for (JComponent c : new JComponent[]{nameField, sexCombo, birthField, addrField,
                telField, pwField, confirmField}) {
            c.setFont(f);
        }

        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 8));
        panel.add(new JLabel("姓名:"));
        panel.add(nameField);
        panel.add(new JLabel("性别:"));
        panel.add(sexCombo);
        panel.add(new JLabel("出生日期:"));
        panel.add(birthField);
        panel.add(new JLabel("地址:"));
        panel.add(addrField);
        panel.add(new JLabel("电话:"));
        panel.add(telField);
        panel.add(new JLabel("密码:"));
        panel.add(pwField);
        panel.add(new JLabel("确认密码:"));
        panel.add(confirmField);

        int r = JOptionPane.showConfirmDialog(this, panel, "读者注册", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String pw = new String(pwField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();
        String sex = (String) sexCombo.getSelectedItem();
        String birth = birthField.getText().trim();
        String addr = addrField.getText().trim();
        String tel = telField.getText().trim();

        if (name.isEmpty() || pw.isEmpty() || tel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "姓名、密码、电话不能为空");
            return;
        }
        if (!pw.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "两次输入的密码不一致");
            return;
        }

        try {
            int newId = ClientNetworkService.getInstance().register(name, pw, sex, birth, addr, tel);
            if (newId > 0) {
                JTextField idField = new JTextField(String.valueOf(newId));
                idField.setFont(new Font("微软雅黑", Font.BOLD, 20));
                idField.setHorizontalAlignment(JTextField.CENTER);
                idField.setEditable(false);

                JPanel msgPanel = new JPanel(new BorderLayout(0, 10));
                msgPanel.add(new JLabel("注册成功！您的读者ID是：", JLabel.CENTER), BorderLayout.NORTH);
                msgPanel.add(idField, BorderLayout.CENTER);
                msgPanel.add(new JLabel("请复制并妥善保管，使用此ID登录", JLabel.CENTER), BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, msgPanel, "注册成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "注册失败，请重试");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "注册失败: " + e.getMessage() + "\n请确认服务器是否运行");
        }
    }

    /**
     * 客户端入口
     * 用法: java ui.LoginFrame [服务器地址] [端口]
     */
    public static void main(String[] args) {
        if (args.length >= 1) {
            ClientNetworkService.getInstance().setHost(args[0]);
        }
        if (args.length >= 2) {
            try {
                ClientNetworkService.getInstance().setPort(Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}