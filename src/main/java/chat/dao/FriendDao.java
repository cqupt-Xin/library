package chat.dao;

import chat.model.FriendInfo;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友关系数据访问层
 * 基于现有表：friend_relations / friend_groups / reader_info / admin / reader_card
 * 不新建任何表
 */
public class FriendDao {

    /**
     * 确保用户有默认好友分组（不存在则创建）
     */
    public int ensureDefaultGroup(int userId) {
        String sql = "SELECT group_id FROM friend_groups WHERE user_id=? AND sort_order=0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("group_id");
        } catch (SQLException ignored) {}
        // 不存在则插入
        String insert = "INSERT INTO friend_groups(user_id, group_name, sort_order) VALUES(?, '我的好友', 0)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 1;
    }

    /**
     * 获取用户好友列表
     * 管理员：自动将所有活跃读者添加为好友
     * 读者：自动展示所有管理员为好友（不走 friend_relations，管理员不在 reader_info 中）
     */
    public List<FriendInfo> getFriends(int userId) {
        List<FriendInfo> list = new ArrayList<>();
        if (isAdmin(userId)) {
            // 管理员 — 自动同步全部活跃读者
            syncAllReadersToAdmin(userId);
        }
        // 查询常规好友（来自 reader_info 的读者）
        String sql = "SELECT fr.friend_id, ri.name, fr.group_id, fr.add_time " +
                     "FROM friend_relations fr " +
                     "JOIN reader_info ri ON fr.friend_id = ri.reader_id " +
                     "WHERE fr.user_id=? ORDER BY ri.name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FriendInfo(
                    rs.getInt("friend_id"),
                    rs.getString("name"),
                    rs.getInt("group_id"),
                    rs.getString("add_time")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        // 读者：自动追加所有管理员到好友列表（不依赖 friend_relations，管理员不在 reader_info 中）
        if (!isAdmin(userId)) {
            String adminSql = "SELECT admin_id FROM admin ORDER BY admin_id";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(adminSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int adminId = rs.getInt("admin_id");
                    list.add(new FriendInfo(adminId, "管理员(" + adminId + ")", 0, null));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return list;
    }

    /**
     * 判断是否为管理员
     */
    private boolean isAdmin(int userId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM admin WHERE admin_id=?")) {
            ps.setInt(1, userId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /**
     * 将全部活跃读者同步为管理员好友
     */
    private void syncAllReadersToAdmin(int adminId) {
        int groupId = ensureDefaultGroup(adminId);
        String sql = "SELECT rc.reader_id FROM reader_card rc " +
                     "WHERE rc.card_state=1 AND rc.reader_id NOT IN " +
                     "(SELECT friend_id FROM friend_relations WHERE user_id=?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int readerId = rs.getInt("reader_id");
                addSingleRelation(conn, adminId, readerId, groupId);
            }
        } catch (SQLException ignored) {}
    }

    /**
     * 读者添加好友（双向：A加B，B也加A）
     * 验证目标读者存在且card_state=1
     */
    public String addFriend(int userId, int targetId) {
        // 验证目标存在：活跃读者 或 管理员
        boolean targetExists = false;
        String checkSql = "SELECT 1 FROM reader_info ri JOIN reader_card rc ON ri.reader_id=rc.reader_id WHERE ri.reader_id=? AND rc.card_state=1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, targetId);
            if (ps.executeQuery().next()) targetExists = true;
        } catch (SQLException e) { return "验证失败: " + e.getMessage(); }
        if (!targetExists) {
            // 管理员也可能成为好友目标
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM admin WHERE admin_id=?")) {
                ps.setInt(1, targetId);
                if (ps.executeQuery().next()) targetExists = true;
            } catch (SQLException e) { return "验证失败: " + e.getMessage(); }
        }
        if (!targetExists) return "目标不存在或已被禁用";

        // 不能添加自己
        if (userId == targetId) return "不能添加自己为好友";

        // 检查是否已是好友
        String exist = "SELECT 1 FROM friend_relations WHERE user_id=? AND friend_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(exist)) {
            ps.setInt(1, userId); ps.setInt(2, targetId);
            if (ps.executeQuery().next()) return "已是好友";
        } catch (SQLException e) { return "查询失败: " + e.getMessage(); }

        // 双向添加
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int groupA = ensureDefaultGroup(userId);
                int groupB = ensureDefaultGroup(targetId);
                addSingleRelation(conn, userId, targetId, groupA);
                addSingleRelation(conn, targetId, userId, groupB);
                conn.commit();
                return "ok";
            } catch (SQLException e) {
                conn.rollback();
                return "添加失败: " + e.getMessage();
            }
        } catch (SQLException e) { return "连接失败: " + e.getMessage(); }
    }

    private void addSingleRelation(Connection conn, int userId, int friendId, int groupId) throws SQLException {
        String sql = "INSERT IGNORE INTO friend_relations(user_id, friend_id, group_id) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, friendId); ps.setInt(3, groupId);
            ps.executeUpdate();
        }
    }

    /**
     * 删除好友（双向）
     */
    public boolean removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM friend_relations WHERE (user_id=? AND friend_id=?) OR (user_id=? AND friend_id=?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, friendId);
            ps.setInt(3, friendId); ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int createGroup(String groupName, int ownerId, String ownerName) {
        String sql = "INSERT INTO chat_groups(group_name, owner_id, owner_name, create_time) VALUES(?,?,?,NOW())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, groupName); ps.setInt(2, ownerId); ps.setString(3, ownerName);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) { addGroupMember(rs.getInt(1), ownerId); return rs.getInt(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public boolean addGroupMember(int groupId, int userId) {
        String sql = "INSERT IGNORE INTO group_members(group_id, user_id) VALUES(?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean removeGroupMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id=? AND user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Integer> getGroupMembers(int groupId) {
        List<Integer> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM group_members WHERE group_id=?")) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getInt("user_id"));
        } catch (SQLException ignored) {}
        return list;
    }

    public List<chat.model.GroupInfo> getUserGroups(int userId) {
        List<chat.model.GroupInfo> list = new ArrayList<>();
        String sql = "SELECT g.*, (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id=g.group_id) AS member_count " +
                     "FROM chat_groups g JOIN group_members gm ON g.group_id=gm.group_id WHERE gm.user_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chat.model.GroupInfo g = new chat.model.GroupInfo();
                g.setGroupId(rs.getInt("group_id")); g.setGroupName(rs.getString("group_name"));
                g.setOwnerId(rs.getInt("owner_id")); g.setOwnerName(rs.getString("owner_name"));
                g.setMemberCount(rs.getInt("member_count"));
                list.add(g);
            }
        } catch (SQLException ignored) {}
        return list;
    }
}
