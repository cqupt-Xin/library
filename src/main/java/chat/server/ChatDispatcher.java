package chat.server;

import chat.dao.FriendDao;
import chat.dao.MessageDao;
import chat.model.ChatMessage;
import chat.model.FriendInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 聊天命令分发器
 */
public class ChatDispatcher {
    private final Gson gson = new Gson();
    private final MessageDao messageDao = new MessageDao();
    private final FriendDao friendDao = new FriendDao();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    public String dispatch(String rawJson) {
        try {
            // 心跳包
            if (rawJson.trim().equals("PING")) {
                return "{\"success\":true,\"message\":\"PONG\"}";
            }
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            String command = json.get("command").getAsString();
            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();

            switch (command) {
                case "chatLogin":    return handleLogin(data);
                case "chatRegister": return handleRegister(data);
                case "sendPrivate":  return handlePrivateMsg(data);
                case "sendGroup":    return handleGroupMsg(data);
                case "getHistory":   return handleHistory(data);
                case "getOffline":   return handleOffline(data);
                case "markRead":     return handleMarkRead(data);
                case "addFriend":    return handleAddFriend(data);
                case "removeFriend": return handleRemoveFriend(data);
                case "getFriends":   return handleGetFriends(data);
                case "getGroups":    return handleGetGroups(data);
                case "createGroup":  return handleCreateGroup(data);
                case "addMember":    return handleAddMember(data);
                case "removeMember": return handleRemoveMember(data);
                case "getOnlineUsers": return handleOnlineUsers();
                default: return error("未知聊天命令");
            }
        } catch (Exception e) {
            return error("命令解析错误: " + e.getMessage());
        }
    }

    // ==================== 登录/注册 ====================

    private String handleLogin(JsonObject data) {
        int userId = data.get("userId").getAsInt();
        String username = data.get("username").getAsString();
        // 封装登录成功信息，客户端收到后切换主界面
        return success("登录成功", gson.toJsonTree(new LoginResult(userId, username,
                friendDao.getFriends(userId),
                friendDao.getUserGroups(userId))));
    }

    // ==================== 私聊消息 ====================

    private String handlePrivateMsg(JsonObject data) {
        int senderId = data.get("senderId").getAsInt();
        String senderName = data.get("senderName").getAsString();
        int targetId = data.get("targetId").getAsInt();
        String content = data.get("content").getAsString();

        ChatMessage msg = new ChatMessage(senderId, senderName, targetId, "private", content);
        messageDao.save(msg);

        // 目标在线则实时推送
        if (OnlineManager.isOnline(targetId)) {
            msg.setRead(true);
            messageDao.markAsRead(msg.getMsgId());
            JsonObject push = new JsonObject();
            push.addProperty("command", "newMessage");
            push.add("data", gson.toJsonTree(msg));
            OnlineManager.sendToUser(targetId, gson.toJson(push));
        }
        // 无论在线与否，发送方始终收到统一成功响应
        return success("发送成功");
    }

    // ==================== 群聊消息 ====================

    private String handleGroupMsg(JsonObject data) {
        int senderId = data.get("senderId").getAsInt();
        String senderName = data.get("senderName").getAsString();
        int groupId = data.get("groupId").getAsInt();
        String content = data.get("content").getAsString();

        ChatMessage msg = new ChatMessage(senderId, senderName, groupId, "group", content);
        messageDao.save(msg);

        JsonObject msgJson = new JsonObject();
        msgJson.addProperty("success", true);
        msgJson.addProperty("command", "newMessage");
        msgJson.add("data", gson.toJsonTree(msg));

        // 推送给群内所有在线成员
        int delivered = 0;
        for (int memberId : friendDao.getGroupMembers(groupId)) {
            if (memberId != senderId && OnlineManager.isOnline(memberId)) {
                OnlineManager.sendToUser(memberId, gson.toJson(msgJson));
                delivered++;
            }
        }
        return success("群消息已发送 (在线" + delivered + "人)");
    }

    // ==================== 离线消息获取 ====================

    private String handleOffline(JsonObject data) {
        int userId = data.get("userId").getAsInt();
        java.util.List<ChatMessage> msgs = messageDao.getOfflineMessages(userId);
        return success("离线消息", gson.toJsonTree(msgs));
    }

    // ==================== 历史消息 ====================

    private String handleHistory(JsonObject data) {
        String msgType = data.get("msgType").getAsString();
        int targetId = data.get("targetId").getAsInt();
        int userId = data.has("userId") ? data.get("userId").getAsInt() : 0;
        int offset = data.has("offset") ? data.get("offset").getAsInt() : 0;
        int limit = data.has("limit") ? data.get("limit").getAsInt() : 50;

        java.util.List<ChatMessage> msgs;
        if ("private".equals(msgType)) {
            msgs = messageDao.getPrivateHistory(userId, targetId, offset, limit);
        } else {
            msgs = messageDao.getGroupHistory(targetId, offset, limit);
        }
        return success("历史消息", gson.toJsonTree(msgs));
    }

    private String handleMarkRead(JsonObject data) {
        long msgId = data.get("msgId").getAsLong();
        int userId = data.has("userId") ? data.get("userId").getAsInt() : 0;
        if (messageDao.markAsRead(msgId)) {
            return success("已读确认");
        }
        return error("标记已读失败");
    }

    private String handleAddFriend(JsonObject data) {
        int userId = data.get("userId").getAsInt();
        int targetId = data.get("targetId").getAsInt();
        String result = friendDao.addFriend(userId, targetId);
        if ("ok".equals(result)) return success("添加好友成功");
        return error(result);
    }

    private String handleRemoveFriend(JsonObject data) {
        if (friendDao.removeFriend(data.get("userId").getAsInt(), data.get("friendId").getAsInt())) {
            return success("删除好友成功");
        }
        return error("删除好友失败");
    }

    private String handleGetFriends(JsonObject data) {
        int userId = data.get("userId").getAsInt();
        java.util.List<FriendInfo> friends = friendDao.getFriends(userId);
        JsonObject wrapper = new JsonObject();
        wrapper.add("friends", gson.toJsonTree(friends));
        return success("好友列表", wrapper);
    }

    private String handleGetGroups(JsonObject data) {
        return success("群组列表", gson.toJsonTree(friendDao.getUserGroups(data.get("userId").getAsInt())));
    }

    private String handleCreateGroup(JsonObject data) {
        int groupId = friendDao.createGroup(data.get("groupName").getAsString(),
                data.get("ownerId").getAsInt(), data.get("ownerName").getAsString());
        if (groupId > 0) return success("群组创建成功", gson.toJsonTree(new GroupResult(groupId)));
        return error("群组创建失败");
    }

    private String handleAddMember(JsonObject data) {
        if (friendDao.addGroupMember(data.get("groupId").getAsInt(), data.get("userId").getAsInt()))
            return success("添加成员成功");
        return error("添加成员失败");
    }

    private String handleRemoveMember(JsonObject data) {
        if (friendDao.removeGroupMember(data.get("groupId").getAsInt(), data.get("userId").getAsInt()))
            return success("移除成员成功");
        return error("移除成员失败");
    }

    private String handleOnlineUsers() {
        return success("在线用户", gson.toJsonTree(OnlineManager.getOnlineUsers()));
    }

    // 忽略的注册（共用图书馆读者注册）
    private String handleRegister(JsonObject data) {
        return success("请使用图书馆管理系统注册读者账号");
    }

    private String success(String message) {
        return "{\"success\":true,\"message\":\"" + escapeJson(message) + "\"}";
    }

    private String success(String message, com.google.gson.JsonElement data) {
        JsonObject r = new JsonObject();
        r.addProperty("success", true);
        r.addProperty("message", message);
        r.add("data", data);
        return gson.toJson(r);
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // 内部结果类
    static class LoginResult {
        int userId; String username; java.util.List<FriendInfo> friends; java.util.List<chat.model.GroupInfo> groups;
        LoginResult(int userId, String username, java.util.List<FriendInfo> friends, java.util.List<chat.model.GroupInfo> groups) {
            this.userId=userId; this.username=username; this.friends=friends; this.groups=groups;
        }
    }
    static class GroupResult {
        int groupId;
        GroupResult(int groupId) { this.groupId = groupId; }
    }
}
