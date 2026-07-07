package ui;

import model.User;
import service.UserService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final UserService userService = new UserService();

    private JTextField idField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("图书管理系统 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(420, 350);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 25, 12, 25);

        JLabel titleLabel = new JLabel("图书管理系统");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 26));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 25, 25, 25);
        panel.add(titleLabel, gbc);

        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel idLabel = new JLabel("ID:");
        idLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        panel.add(idLabel, gbc);
        idField = new JTextField(18);
        idField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        JLabel pwLabel = new JLabel("密码:");
        pwLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        panel.add(pwLabel, gbc);
        passwordField = new JPasswordField(18);
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton loginBtn = new JButton("登录");
        loginBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        loginBtn.setPreferredSize(new Dimension(120, 40));
        JButton registerBtn = new JButton("注册");
        registerBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        registerBtn.setPreferredSize(new Dimension(120, 40));
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 25, 10, 25);
        panel.add(btnPanel, gbc);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());

        setContentPane(panel);
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String idStr = idField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (idStr.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID和密码不能为空");
            return;
        }

        User user = userService.login(idStr, password);
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
    }

    private void doRegister() {
        JTextField nameField = new JTextField(10);
        JComboBox<String> sexCombo = new JComboBox<>(new String[]{"男", "女"});
        JTextField birthField = new JTextField("1990-01-01", 10);
        JTextField addrField = new JTextField(10);
        JTextField telField = new JTextField(10);
        JPasswordField pwField = new JPasswordField(10);
        JPasswordField confirmField = new JPasswordField(10);

        Font f = new Font("微软雅黑", Font.PLAIN, 13);
        for (JComponent c : new JComponent[]{nameField, sexCombo, birthField, addrField, telField, pwField, confirmField}) {
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

        int newId = userService.register(name, pw, sex, birth, addr, tel);
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
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
