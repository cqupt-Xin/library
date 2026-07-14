package ui;

import server.BookCommandServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 服务器监视弹窗
 * 包含：端口配置、启停控制、状态显示、实时日志
 */
public class ServerMonitorDialog extends JDialog {

    private final JFrame owner;
    private BookCommandServer server;

    private JButton toggleBtn;
    private JLabel statusLabel;
    private JSpinner portSpinner;
    private JTextArea logArea;

    public ServerMonitorDialog(JFrame owner) {
        super(owner, "服务器监视", false); // 非模态
        this.owner = owner;
        initUI();

        // 关闭窗口时仅隐藏，不销毁
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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

    // ==================== 顶部控制区 ====================

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // — 控制行 —
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));

        JLabel portLabel = new JLabel("监听端口:");
        portLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        controlRow.add(portLabel);

        SpinnerNumberModel portModel = new SpinnerNumberModel(8888, 1024, 65535, 1);
        portSpinner = new JSpinner(portModel);
        portSpinner.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        portSpinner.setPreferredSize(new Dimension(75, 28));
        controlRow.add(portSpinner);

        toggleBtn = new JButton("启动服务器");
        toggleBtn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        toggleBtn.setBackground(new Color(46, 139, 87));
        toggleBtn.setForeground(Color.BLACK);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.setMargin(new Insets(4, 16, 4, 16));
        controlRow.add(toggleBtn);

        statusLabel = new JLabel("● 已停止");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statusLabel.setForeground(Color.GRAY);
        controlRow.add(statusLabel);

        JButton clearBtn = new JButton("清空日志");
        clearBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        clearBtn.setMargin(new Insets(2, 8, 2, 8));
        controlRow.add(clearBtn);

        panel.add(controlRow, BorderLayout.CENTER);

        // — 分隔线 —
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(200, 200, 200));
        sep.setPreferredSize(new Dimension(490, 1));
        panel.add(sep, BorderLayout.SOUTH);

        // 事件
        toggleBtn.addActionListener(e -> toggleServer());
        clearBtn.addActionListener(e -> logArea.setText(""));

        return panel;
    }

    // ==================== 日志区 ====================

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("运行日志"));

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

    // ==================== 服务器操作 ====================

    private void toggleServer() {
        if (server != null && server.isRunning()) {
            server.stop();
            server = null;
            updateUIState(false, 0);
        } else {
            int port = (int) portSpinner.getValue();
            toggleBtn.setEnabled(false);
            toggleBtn.setText("启动中...");
            statusLabel.setText("◉ 绑定端口...");
            statusLabel.setForeground(Color.ORANGE);
            portSpinner.setEnabled(false);

            server = new BookCommandServer(port);
            server.start(logArea, success -> {
                toggleBtn.setEnabled(true);
                if (success) {
                    updateUIState(true, port);
                } else {
                    server = null;
                    updateUIState(false, 0);
                }
            });
        }
    }

    private void updateUIState(boolean running, int port) {
        if (running) {
            toggleBtn.setText("停止服务器");
            toggleBtn.setBackground(new Color(220, 80, 60));
            statusLabel.setText("● 运行中 (端口 " + port + ")");
            statusLabel.setForeground(new Color(46, 139, 87));
            portSpinner.setEnabled(false);
        } else {
            toggleBtn.setText("启动服务器");
            toggleBtn.setBackground(new Color(46, 139, 87));
            statusLabel.setText("● 已停止");
            statusLabel.setForeground(Color.GRAY);
            portSpinner.setEnabled(true);
        }
    }

    /**
     * 可由外部调用以在关闭主窗口时停止服务器
     */
    public void shutdownServer() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
