package chat.server;

import java.io.BufferedWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户管理器
 * 维护在线用户ID -> ClientHandler映射，支持消息路由
 */
public class OnlineManager {
    // userId -> ChatClientHandler的写通道
    private static final ConcurrentHashMap<Integer, BufferedWriter> onlineMap = new ConcurrentHashMap<>();
    // userId -> 用户名
    private static final ConcurrentHashMap<Integer, String> nameMap = new ConcurrentHashMap<>();

    public static void addUser(int userId, String username, BufferedWriter writer) {
        onlineMap.put(userId, writer);
        nameMap.put(userId, username);
    }

    public static void removeUser(int userId) {
        onlineMap.remove(userId);
        nameMap.remove(userId);
    }

    public static boolean isOnline(int userId) {
        return onlineMap.containsKey(userId);
    }

    public static String getUsername(int userId) {
        return nameMap.getOrDefault(userId, "未知用户");
    }

    public static BufferedWriter getWriter(int userId) {
        return onlineMap.get(userId);
    }

    public static java.util.Set<Integer> getOnlineUsers() {
        return onlineMap.keySet();
    }

    public static int getOnlineCount() {
        return onlineMap.size();
    }

    /**
     * 向在线用户推送消息
     */
    public static synchronized boolean sendToUser(int userId, String json) {
        BufferedWriter writer = onlineMap.get(userId);
        if (writer == null) return false;
        try {
            writer.write(json);
            writer.newLine();
            writer.flush();
            return true;
        } catch (Exception e) {
            onlineMap.remove(userId);
            return false;
        }
    }

    /**
     * 广播给所有在线用户（用于系统通知）
     */
    public static void broadcast(String json) {
        onlineMap.forEach((id, writer) -> {
            try {
                writer.write(json);
                writer.newLine();
                writer.flush();
            } catch (Exception ignored) {}
        });
    }
}
