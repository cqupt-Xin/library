package chat.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * 聊天客户端处理器（长连接 + 心跳 + 断线处理）
 * 每个客户端连接一个独立线程，保持TCP长连接
 */
public class ChatClientHandler implements Runnable {
    private final Socket socket;
    private final ChatDispatcher dispatcher;
    private BufferedWriter writer;
    private BufferedReader reader;
    private int userId;
    private String username;
    private volatile boolean running = true;
    private long lastHeartbeat;

    public ChatClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = new ChatDispatcher();
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            lastHeartbeat = System.currentTimeMillis();

            String line;
            while (running && (line = reader.readLine()) != null) {
                lastHeartbeat = System.currentTimeMillis();

                // 心跳处理
                if (line.trim().equals("PING")) {
                    writer.write("PONG\n");
                    writer.flush();
                    continue;
                }

                // 登录绑定（提取userId用于在线管理）
                if (line.contains("\"chatLogin\"")) {
                    String resp = dispatcher.dispatch(line);
                    writer.write(resp);
                    writer.newLine();
                    writer.flush();

                    // 登录成功后注册在线
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(resp).getAsJsonObject();
                    if (json.get("success").getAsBoolean() && json.has("data")) {
                        com.google.gson.JsonObject data = json.getAsJsonObject("data");
                        this.userId = data.get("userId").getAsInt();
                        this.username = data.get("username").getAsString();
                        OnlineManager.addUser(userId, username, writer);
                        System.out.println("[IM] 用户上线: " + username + " (id=" + userId + ")");
                        // 推送离线消息
                        pushOfflineMessages();
                        // 广播上线通知
                        OnlineManager.broadcast("{\"command\":\"userOnline\",\"data\":{\"userId\":" + userId + ",\"username\":\"" +
                                OnlineManager.getUsername(userId) + "\"}}");
                    }
                    continue;
                }

                // 其他命令
                String resp = dispatcher.dispatch(line);
                writer.write(resp);
                writer.newLine();
                writer.flush();
            }
        } catch (SocketException e) {
            // 客户端正常断开
        } catch (IOException e) {
            if (running) e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void pushOfflineMessages() {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String req = "{\"command\":\"getOffline\",\"data\":{\"userId\":" + userId + "}}";
            String resp = dispatcher.dispatch(req);
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(resp).getAsJsonObject();
            if (json.get("success").getAsBoolean() && json.has("data")) {
                // ChatMainFrame 期望: {"command":"offlineMessages","data":[...]}
                com.google.gson.JsonObject push = new com.google.gson.JsonObject();
                push.addProperty("command", "offlineMessages");
                push.add("data", json.get("data"));
                writer.write(gson.toJson(push));
                writer.newLine(); writer.flush();
            }
        } catch (Exception ignored) {}
    }

    private void cleanup() {
        running = false;
        if (userId > 0) {
            OnlineManager.removeUser(userId);
            System.out.println("[IM] 用户下线: " + username + " (id=" + userId + ")");
            OnlineManager.broadcast("{\"command\":\"userOffline\",\"data\":{\"userId\":" + userId + "}}");
        }
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (Exception ignored) {}
    }

    public long getLastHeartbeat() { return lastHeartbeat; }

    public void stop() {
        running = false;
        cleanup();
    }
}
