package ui;

import client.ClientNetworkService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 服务器监视对话框（客户端版本）
 * 客户端模式下仅显示连接状态和日志，不控制服务器启停
 * 如需完整的服务器控制功能，请在服务端进程中运行
 */
public class ServerMonitorDialog extends JDialog {

    private final JFrame owner;

    private JLabel statusLabel;
    private JTextArea logArea;
    private Timer pingTimer;

    public ServerMonitorDialog(JFrame owner) {
        super(owner, "服务器连接状态", false);
        this.owner = owner;
        initUI();

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopPingTimer();
                setVisible(false);
            }
        });
    }

    private void initUI() {
        setSize(520, 380);
        setLocationRelativeTo(owner);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);
        mainPanel.add(createLogPanel(), BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));

        JLabel titleLabel = new JLabel("服务器状态监视");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        controlRow.add(titleLabel);

        statusLabel = new JLabel("检测中...");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statusLabel.setForeground(Color.GRAY);
        controlRow.add(statusLabel);

        JButton refreshBtn = new JButton("刷新状态");
        refreshBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        refreshBtn.setMargin(new Insets(2, 8, 2, 8));
        controlRow.add(refreshBtn);

        JButton clearBtn = new JButton("清空日志");
        clearBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        clearBtn.setMargin(new Insets(2, 8, 2, 8));
        controlRow.add(clearBtn);

        panel.add(controlRow, BorderLayout.CENTER);

        // 提示信息
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2));
        JLabel infoLabel = new JLabel("客户端模式 | 服务器需独立运行 (java server.ServerMain)");
        infoLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoRow.add(infoLabel);
        panel.add(infoRow, BorderLayout.SOUTH);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(200, 200, 200));
        sep.setPreferredSize(new Dimension(490, 1));
        panel.add(sep, BorderLayout.NORTH);

        refreshBtn.addActionListener(e -> checkConnection());
        clearBtn.addActionListener(e -> logArea.setText(""));

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("连接日志"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            checkConnection();
            startPingTimer();
        } else {
            stopPingTimer();
        }
        super.setVisible(visible);
    }

    private void checkConnection() {
        ClientNetworkService cns = ClientNetworkService.getInstance();
        String addr = cns.getHost() + ":" + cns.getPort();

        new Thread(() -> {
            long start = System.currentTimeMillis();
            boolean ok = cns.ping();
            long elapsed = System.currentTimeMillis() - start;

            SwingUtilities.invokeLater(() -> {
                String time = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                if (ok) {
                    statusLabel.setText("\u25CF 已连接 (" + elapsed + "ms)");
                    statusLabel.setForeground(new Color(46, 139, 87));
                    logArea.append("[" + time + "] \u2713 服务器 " + addr + " 响应正常 ("
                            + elapsed + "ms)\n");
                } else {
                    statusLabel.setText("\u2717 未连接");
                    statusLabel.setForeground(new Color(220, 80, 60));
                    logArea.append("[" + time + "] \u2717 无法连接服务器 " + addr + "\n");
                }
            });
        }, "ServerPing").start();
    }

    private void startPingTimer() {
        stopPingTimer();
        pingTimer = new Timer(10000, e -> checkConnection()); // 每10秒检测一次
        pingTimer.start();
    }

    private void stopPingTimer() {
        if (pingTimer != null) {
            pingTimer.stop();
            pingTimer = null;
        }
    }

    /**
     * 客户端模式下无需关闭服务器
     */
    public void shutdownServer() {
        stopPingTimer();
    }
}
