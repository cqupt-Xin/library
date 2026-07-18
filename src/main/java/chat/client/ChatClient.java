package chat.client;

import chat.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 聊天客户端网络服务
 * 长连接 + 心跳 + 自动重连 + 消息监听
 */
public class ChatClient {
    private static ChatClient instance;
    private final Gson gson = new Gson();
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private volatile boolean connected;
    private String host = "127.0.0.1";
    private int port = 9000;
    private MessageListener listener;
    private ScheduledExecutorService heartbeat;
    private int reconnectAttempts;
    
    // 本地消息暂存队列：离线/断连时暂存待发送消息，重连后自动批量发送
    private final Queue<String> pendingQueue = new ConcurrentLinkedQueue<>();

    private ChatClient() {}

    public static ChatClient getInstance() {
        if (instance == null) instance = new ChatClient();
        return instance;
    }

    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }

    /**
     * 建立长连接并启动心跳
     */
    public boolean connect(String username, int userId) throws IOException {
        disconnect();
        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;

        // 发送登录
        String loginCmd = "{\"command\":\"chatLogin\",\"data\":{\"userId\":" + userId + ",\"username\":\"" + username + "\"}}";
        writer.write(loginCmd);
        writer.newLine(); writer.flush();
        String resp = reader.readLine();
        JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
        if (!json.get("success").getAsBoolean()) {
            disconnect();
            return false;
        }

        // 启动心跳（每20秒）
        heartbeat = Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(this::sendHeartbeat, 20, 20, TimeUnit.SECONDS);

        // 启动消息接收线程
        Thread recvThread = new Thread(this::receiveLoop, "ChatClient-Recv");
        recvThread.setDaemon(true);
        recvThread.start();

        // 重连后自动发送暂存队列中的离线消息
        flushPendingQueue();

        return true;
    }

    private void sendHeartbeat() {
        try {
            if (writer != null) { writer.write("PING\n"); writer.flush(); }
        } catch (Exception e) { handleDisconnect(); }
    }

    private void receiveLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                if (line.equals("PONG")) continue;
                if (listener != null) listener.onMessage(line);
            }
        } catch (Exception e) {
            if (connected) handleDisconnect();
        }
    }

    private void handleDisconnect() {
        connected = false;
        if (listener != null) listener.onDisconnect();
        if (reconnectAttempts < 5) {
            reconnectAttempts++;
            System.out.println("[IM] 断线重连 第" + reconnectAttempts + "次...");
            if (listener != null) listener.onReconnecting(reconnectAttempts);
        }
    }

    /**
     * 发送聊天消息 — 解除与在线状态的强制绑定
     * 若当前已连接，直接发送；若未连接，将消息暂存到本地队列，连接恢复后自动发送
     * @return true 表示已直接发送，false 表示已加入暂存队列
     */
    public boolean sendCommand(String json) throws IOException {
        if (!connected || writer == null) {
            pendingQueue.add(json);
            if (listener != null) listener.onMessageQueued(json, pendingQueue.size());
            return false;
        }
        writer.write(json);
        writer.newLine(); writer.flush();
        return true;
    }

    /**
     * 获取当前暂存消息数量
     */
    public int getPendingCount() {
        return pendingQueue.size();
    }

    /**
     * 清空暂存队列（取消所有待发消息）
     */
    public void clearPendingQueue() {
        pendingQueue.clear();
    }

    /**
     * 批量发送暂存队列中的所有消息
     */
    private void flushPendingQueue() {
        if (pendingQueue.isEmpty()) return;
        int sent = 0;
        while (!pendingQueue.isEmpty()) {
            String json = pendingQueue.poll();
            try {
                writer.write(json);
                writer.newLine();
                sent++;
            } catch (Exception e) {
                // 发送失败则放回队列前端
                pendingQueue.add(json);
                break;
            }
        }
        try { writer.flush(); } catch (Exception ignored) {}
        final int remaining = pendingQueue.size();
        if (listener != null) {
            listener.onQueueFlushed(sent, remaining);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connected = false;
        if (heartbeat != null) { heartbeat.shutdownNow(); heartbeat = null; }
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() { return connected; }

    public void setMessageListener(MessageListener listener) { this.listener = listener; }

    public interface MessageListener {
        void onMessage(String rawJson);
        void onDisconnect();
        void onReconnecting(int attempt);
        /** 消息已加入本地暂存队列（离线/断连时触发） */
        default void onMessageQueued(String rawJson, int queueSize) {}
        /** 暂存队列已批量发送（重连后触发） */
        default void onQueueFlushed(int sent, int remaining) {}
    }
}
