package chat.ui;

import chat.client.ChatClient;
import chat.model.ChatMessage;
import chat.model.FriendInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * QQ风格聊天主界面 — 所有数据均从后端加载，无任何硬编码/Mock数据
 */
public class ChatMainFrame extends JFrame {
    private final int myUserId;
    private final String myUsername;
    private final Gson gson = new Gson();
    private final ChatClient client = ChatClient.getInstance();

    private JPanel chatPanel;
    private JLabel statusLabel;
    private JTextArea chatArea;
    private JTextField inputField;
    private DefaultListModel<String> contactModel;
    private JList<String> contactList;

    private int currentChatTarget = -1;
    private String currentTargetName = "";
    // friendId -> friendName 映射
    private final Map<Integer, String> friendMap = new LinkedHashMap<>();
    // 在线用户集合
    private final Set<Integer> onlineUsers = new HashSet<>();

    private boolean connected = false;

    public ChatMainFrame(int userId, String username) {
        this.myUserId = userId;
        this.myUsername = username;
        setTitle("即时通信 - " + username);
        setSize(860, 640);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(720, 480));

        initComponents();
        initNetworkListener();
    }

    // ==================== UI 初始化 ====================

    private void initComponents() {
        setLayout(new BorderLayout());

        // 左侧面板 — 仅联系人列表
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setBackground(new Color(235, 235, 235));
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, 1));

        // 联系人列表标题
        JLabel contactTitle = new JLabel("  联系人");
        contactTitle.setFont(new Font("微软雅黑", Font.BOLD, 13));
        contactTitle.setBorder(new EmptyBorder(8, 4, 8, 4));
        contactTitle.setOpaque(true);
        contactTitle.setBackground(new Color(235, 235, 235));
        leftPanel.add(contactTitle, BorderLayout.NORTH);

        // 联系人列表
        contactModel = new DefaultListModel<>();
        contactList = new JList<>(contactModel);
        contactList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        contactList.setCellRenderer(new ContactCellRenderer());
        contactList.setFixedCellHeight(42);
        contactList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !contactList.isSelectionEmpty()) {
                    String sel = contactList.getSelectedValue();
                    String cleanName = (sel.startsWith("● ") || sel.startsWith("○ ")) ? sel.substring(2) : sel;
                    for (Map.Entry<Integer, String> entry : friendMap.entrySet()) {
                        if (entry.getValue().equals(cleanName)) {
                            startPrivateChat(entry.getKey(), entry.getValue());
                            return;
                        }
                    }
                }
            }
        });
        JScrollPane contactScroll = new JScrollPane(contactList);
        contactScroll.setBorder(null);
        leftPanel.add(contactScroll, BorderLayout.CENTER);

        // 添加好友按钮
        JButton addFriendBtn = new JButton("+ 添加好友");
        addFriendBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        addFriendBtn.setFocusPainted(false);
        addFriendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addFriendBtn.addActionListener(e -> showAddFriendDialog());
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(new Color(235, 235, 235));
        btnPanel.add(addFriendBtn);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // 右侧聊天区
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(Color.WHITE);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(new EmptyBorder(10, 16, 10, 16));
        titleBar.setBackground(new Color(245, 245, 245));
        titleBar.add(new JLabel("正在连接IM服务器..."), BorderLayout.WEST);
        chatPanel.add(titleBar, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBorder(new EmptyBorder(8, 12, 12, 12));
        inputPanel.setBackground(Color.WHITE);

        inputField = new JTextField();
        inputField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("发送");
        sendBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        sendBtn.setBackground(new Color(24, 144, 255));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setPreferredSize(new Dimension(72, 36));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.CENTER);

        // 状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(240, 240, 240));
        statusBar.setBorder(new EmptyBorder(2, 12, 2, 12));
        statusLabel = new JLabel("未连接");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        statusLabel.setForeground(Color.RED);
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ==================== 网络监听 ====================

    private void initNetworkListener() {
        client.setMessageListener(new ChatClient.MessageListener() {
            @Override
            public void onMessage(String rawJson) {
                SwingUtilities.invokeLater(() -> handleMessage(rawJson));
            }
            @Override
            public void onDisconnect() {
                SwingUtilities.invokeLater(() -> {
                    connected = false;
                    statusLabel.setText("连接已断开 | 消息将暂存本地");
                    statusLabel.setForeground(Color.ORANGE);
                });
            }
            @Override
            public void onReconnecting(int attempt) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("重连中...(" + attempt + "/5) | 暂存消息:" + client.getPendingCount() + "条");
                    statusLabel.setForeground(Color.ORANGE);
                });
            }
            @Override
            public void onMessageQueued(String rawJson, int queueSize) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("离线模式 | 消息已暂存(" + queueSize + "条)，恢复连接后自动发送");
                    statusLabel.setForeground(Color.ORANGE);
                });
            }
            @Override
            public void onQueueFlushed(int sent, int remaining) {
                SwingUtilities.invokeLater(() -> {
                    if (remaining == 0) {
                        statusLabel.setText("在线 | 暂存消息已全部同步发送(" + sent + "条)");
                        statusLabel.setForeground(new Color(0, 128, 0));
                    } else {
                        statusLabel.setText("部分暂存消息发送失败 | 已发送:" + sent + " 剩余:" + remaining);
                        statusLabel.setForeground(Color.ORANGE);
                    }
                });
            }
        });
    }

    // ==================== 数据连接后由外部调用 ====================

    public void onConnected() {
        connected = true;
        SwingUtilities.invokeLater(() -> {
            int pending = client.getPendingCount();
            if (pending > 0) {
                statusLabel.setText("在线 | 正在同步" + pending + "条暂存消息...");
                statusLabel.setForeground(Color.ORANGE);
            } else {
                statusLabel.setText("在线 | 已连接IM服务器");
                statusLabel.setForeground(new Color(0, 128, 0));
            }
            updateTitle("选择一个联系人开始聊天");
            requestFriends();
            requestOnlineUsers();
        });
    }

    private void requestOnlineUsers() {
        try {
            client.sendCommand("{\"command\":\"getOnlineUsers\"}");
        } catch (IOException ignored) {}
    }

    private void requestFriends() {
        try {
            client.sendCommand("{\"command\":\"getFriends\",\"data\":{\"userId\":" + myUserId + "}}");
        } catch (IOException ignored) {}
    }

    // ==================== 消息处理 ====================

    private void handleMessage(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            String command = json.has("command") ? json.get("command").getAsString() : "";

            switch (command) {
                case "chatLoginResp":
                    break;
                case "newMessage":
                    ChatMessage msg = gson.fromJson(json.get("data"), ChatMessage.class);
                    appendMessage(msg);
                    break;
                case "offlineMessages":
                    java.lang.reflect.Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
                    List<ChatMessage> offline = gson.fromJson(json.get("data"), listType);
                    for (ChatMessage m : offline) appendMessage(m);
                    break;
                case "userOnline":
                    int oid = json.getAsJsonObject("data").get("userId").getAsInt();
                    onlineUsers.add(oid);
                    refreshContactList();
                    statusLabel.setText(json.getAsJsonObject("data").get("username").getAsString() + " 上线了");
                    statusLabel.setForeground(new Color(0, 128, 0));
                    break;
                case "userOffline":
                    int fid = json.getAsJsonObject("data").get("userId").getAsInt();
                    onlineUsers.remove(fid);
                    refreshContactList();
                    break;
                default:
                    // 处理 getOnlineUsers 响应: data是数组 [1,2,3...]
                    if (json.has("data") && json.get("data").isJsonArray()) {
                        JsonArray arr = json.getAsJsonArray("data");
                        // 区分：数字数组 = 在线用户ID列表，对象数组 = 历史/离线消息列表
                        if (arr.size() > 0 && arr.get(0).isJsonPrimitive()) {
                            onlineUsers.clear();
                            for (int i = 0; i < arr.size(); i++) onlineUsers.add(arr.get(i).getAsInt());
                            refreshContactList();
                        } else if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                            // getHistory 响应：data 是 ChatMessage 数组
                            java.lang.reflect.Type histType = new TypeToken<List<ChatMessage>>() {}.getType();
                            List<ChatMessage> history = gson.fromJson(arr, histType);
                            for (ChatMessage m : history) appendMessage(m);
                        }
                        break;
                    }
                    if (json.get("success") != null && json.get("success").getAsBoolean()) {
                        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : null;
                        if (data != null && data.has("friends")) {
                            friendMap.clear();
                            java.lang.reflect.Type friendListType = new TypeToken<List<FriendInfo>>(){}.getType();
                            List<FriendInfo> flist = gson.fromJson(data.get("friends"), friendListType);
                            for (FriendInfo fi : flist) {
                                friendMap.put(fi.getFriendId(), fi.getFriendName());
                            }
                            refreshContactList();
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("消息解析错误: " + e.getMessage());
        }
    }

    private void refreshContactList() {
        contactModel.clear();
        if (friendMap.isEmpty()) {
            contactModel.addElement("暂无好友，请先添加");
            return;
        }
        for (Map.Entry<Integer, String> entry : friendMap.entrySet()) {
            boolean online = onlineUsers.contains(entry.getKey());
            String icon = online ? "● " : "○ ";
            contactModel.addElement(icon + entry.getValue());
        }
    }

    private void appendMessage(ChatMessage msg) {
        boolean isRelevant = (msg.getSenderId() == currentChatTarget || msg.getTargetId() == currentChatTarget);

        if (isRelevant) {
            String prefix = (msg.getSenderId() == myUserId) ? "我" : msg.getSenderName();
            String time = msg.getSendTime();
            if (time != null && time.length() >= 19) time = time.substring(11, 19);
            else time = "";
            chatArea.append("[" + time + "] " + prefix + ": " + msg.getContent() + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }

        // 已读回执
        if (!msg.isRead() && msg.getSenderId() != myUserId) {
            try {
                client.sendCommand("{\"command\":\"markRead\",\"data\":{\"msgId\":" + msg.getMsgId() + "}}");
            } catch (IOException ignored) {}
        }
    }

    // ==================== 聊天操作 ====================

    private void startPrivateChat(int friendId, String friendName) {
        currentChatTarget = friendId;
        currentTargetName = friendName;
        chatArea.setText("");
        updateTitle("与 " + friendName + " 聊天中");
        loadHistory();
    }

    private void updateTitle(String t) {
        Component[] comps = chatPanel.getComponents();
        if (comps.length > 0 && comps[0] instanceof JPanel) {
            JPanel bar = (JPanel) comps[0];
            bar.removeAll();
            JLabel titleLabel = new JLabel(t);
            titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
            bar.add(titleLabel, BorderLayout.WEST);
            bar.revalidate();
            bar.repaint();
        }
    }

    private void loadHistory() {
        try {
            String cmd = "{\"command\":\"getHistory\",\"data\":{\"msgType\":\"private\""
                    + ",\"targetId\":" + currentChatTarget + ",\"userId\":" + myUserId + "}}";
            client.sendCommand(cmd);
        } catch (Exception ignored) {}
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || currentChatTarget < 0) return;

        try {
            String cmd = "{\"command\":\"sendPrivate\",\"data\":{\"senderId\":" + myUserId
                    + ",\"senderName\":\"" + escapeJson(myUsername) + "\",\"targetId\":" + currentChatTarget
                    + ",\"content\":\"" + escapeJson(text) + "\"}}";
            boolean sent = client.sendCommand(cmd);
            // 消息已在本地聊天区展示（离线时也正常展示，仅状态栏提示暂存信息）
            chatArea.append("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + "] 我: " + text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            inputField.setText("");
            
            if (!sent) {
                // 消息已暂存到本地队列，等待连接恢复后自动发送
                statusLabel.setText("离线模式 | 消息已暂存(" + client.getPendingCount() + "条)，恢复连接后自动发送");
                statusLabel.setForeground(Color.ORANGE);
            }
        } catch (IOException e) {
            // 即使是IOException，也加入暂存队列并展示给用户
            chatArea.append("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + "] 我: " + text + " [暂存]\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            inputField.setText("");
            statusLabel.setText("离线模式 | 消息已暂存(" + client.getPendingCount() + "条)，恢复连接后自动发送");
            statusLabel.setForeground(Color.ORANGE);
        }
    }

    private void showAddFriendDialog() {
        JTextField field = new JTextField(15);
        field.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        int result = JOptionPane.showConfirmDialog(this, field,
                "输入要添加的读者ID", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        String input = field.getText().trim();
        if (input.isEmpty()) return;

        try {
            int targetId = Integer.parseInt(input);
            if (!connected) {
                JOptionPane.showMessageDialog(this, "未连接IM服务器", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String cmd = "{\"command\":\"addFriend\",\"data\":{\"userId\":" + myUserId + ",\"targetId\":" + targetId + "}}";
            client.sendCommand(cmd);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字ID", "错误", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void dispose() {
        client.disconnect();
        super.dispose();
    }

    // ==================== 渲染器 ====================

    static class ContactCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = value.toString();
            if (text.startsWith("●")) {
                label.setForeground(new Color(0, 150, 0));
                label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            } else if (text.startsWith("○")) {
                label.setForeground(Color.GRAY);
                label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            } else {
                label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            }
            label.setBorder(new EmptyBorder(4, 12, 4, 4));
            return label;
        }
    }
}
