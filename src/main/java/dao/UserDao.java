package dao;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import util.DBUtil;

public class UserDao {

    public User loginAdmin(int adminId, String password) {
        String sql = "SELECT admin_id, password FROM admin WHERE admin_id = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("admin_id"), String.valueOf(rs.getInt("admin_id")), "admin");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User loginReader(int readerId, String passwd) {
        String sql = "SELECT rc.reader_id, rc.name, rc.passwd FROM reader_card rc WHERE rc.reader_id = ? AND rc.passwd = ? AND rc.card_state = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            ps.setString(2, passwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("reader_id"));
                    user.setUsername(rs.getString("name"));
                    user.setPassword(rs.getString("passwd"));
                    user.setRole("reader");
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllReaders() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT rc.reader_id, rc.name, rc.passwd, rc.card_state, ri.sex, ri.birth, ri.address, ri.telcode FROM reader_card rc LEFT JOIN reader_info ri ON rc.reader_id = ri.reader_id ORDER BY rc.reader_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("reader_id"));
                user.setUsername(rs.getString("name"));
                user.setPassword(rs.getString("passwd"));
                user.setRole("reader");
                user.setStatus(rs.getInt("card_state") == 1 ? "正常" : "已禁用");
                list.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int register(String name, String passwd, String sex, String birth, String address, String telcode) {
        String sql1 = "SELECT COALESCE(MAX(reader_id), 10000000) + 1 AS next_id FROM reader_info";
        String sql2 = "INSERT INTO reader_info (reader_id, name, sex, birth, address, telcode) VALUES (?, ?, ?, ?, ?, ?)";
        String sql3 = "INSERT INTO reader_card (reader_id, name, passwd, card_state) VALUES (?, ?, ?, 1)";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int newId;
            try (PreparedStatement ps1 = conn.prepareStatement(sql1);
                 ResultSet rs = ps1.executeQuery()) {
                rs.next();
                newId = rs.getInt("next_id");
            }

            try (PreparedStatement ps2 = conn.prepareStatement(sql2);
                 PreparedStatement ps3 = conn.prepareStatement(sql3)) {
                ps2.setInt(1, newId);
                ps2.setString(2, name);
                ps2.setString(3, sex);
                ps2.setString(4, birth);
                ps2.setString(5, address);
                ps2.setString(6, telcode);
                ps2.executeUpdate();

                ps3.setInt(1, newId);
                ps3.setString(2, name);
                ps3.setString(3, passwd);
                ps3.executeUpdate();
            }

            conn.commit();
            return newId;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return -1;
    }

    public boolean updateCardState(int readerId, int state) {
        String sql = "UPDATE reader_card SET card_state = ? WHERE reader_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, state);
            ps.setInt(2, readerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
