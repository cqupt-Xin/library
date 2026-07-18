package chat.dao;

import chat.model.ChatMessage;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天消息数据访问层
 * 管理 chat_messages 表的 CRUD
 * 支持离线消息缓存、历史查询、已读状态更新
 */
public class MessageDao {

    /**
     * 保存消息到数据库（发送时调用）
     */
    public boolean save(ChatMessage msg) {
        String sql = "INSERT INTO chat_messages(sender_id, sender_name, target_id, msg_type, content, send_time, is_read) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getSenderId());
            ps.setString(2, msg.getSenderName());
            ps.setInt(3, msg.getTargetId());
            ps.setString(4, msg.getMsgType());
            ps.setString(5, msg.getContent());
            ps.setString(6, msg.getSendTime());
            ps.setBoolean(7, msg.isRead());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) msg.setMsgId(rs.getLong(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取两个用户之间的历史消息（私聊）
     */
    public List<ChatMessage> getPrivateHistory(int userId1, int userId2, int offset, int limit) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE msg_type='private' " +
                "AND ((sender_id=? AND target_id=?) OR (sender_id=? AND target_id=?)) " +
                "ORDER BY send_time DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1); ps.setInt(2, userId2);
            ps.setInt(3, userId2); ps.setInt(4, userId1);
            ps.setInt(5, limit); ps.setInt(6, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 获取群聊历史消息
     */
    public List<ChatMessage> getGroupHistory(int groupId, int offset, int limit) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE msg_type='group' AND target_id=? ORDER BY send_time DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId); ps.setInt(2, limit); ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 获取用户的离线消息（is_read=false 且 msg_type=private）
     */
    public List<ChatMessage> getOfflineMessages(int userId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE msg_type='private' AND target_id=? AND is_read=false ORDER BY send_time ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 标记消息为已读（收到已读回执时调用）
     */
    public boolean markAsRead(long msgId) {
        String sql = "UPDATE chat_messages SET is_read=true WHERE msg_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, msgId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * 获取群聊消息的已读人数
     */
    public int getGroupMsgReadCount(long msgId) {
        String sql = "SELECT COUNT(*) FROM chat_msg_reads WHERE msg_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, msgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * 群聊消息标记已读
     */
    public boolean markGroupMsgRead(long msgId, int userId) {
        String sql = "INSERT IGNORE INTO chat_msg_reads(msg_id, reader_id) VALUES(?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, msgId); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private ChatMessage mapRow(ResultSet rs) throws SQLException {
        ChatMessage msg = new ChatMessage();
        msg.setMsgId(rs.getLong("msg_id"));
        msg.setSenderId(rs.getInt("sender_id"));
        msg.setSenderName(rs.getString("sender_name"));
        msg.setTargetId(rs.getInt("target_id"));
        msg.setMsgType(rs.getString("msg_type"));
        msg.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("send_time");
        if (ts != null) msg.setSendTime(ts.toLocalDateTime().format(ChatMessage.TIME_FMT));
        msg.setRead(rs.getBoolean("is_read"));
        return msg;
    }
}
