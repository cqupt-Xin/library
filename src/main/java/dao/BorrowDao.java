package dao;

import model.Borrow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import util.DBUtil;

public class BorrowDao {

    public List<Borrow> findAll() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT l.*, b.book_name, ri.name AS reader_name FROM lend_list l LEFT JOIN book_info b ON l.book_id = b.book_id LEFT JOIN reader_info ri ON l.reader_id = ri.reader_id ORDER BY l.sernum";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowToBorrow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Borrow> findByReaderId(int readerId) {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT l.*, b.book_name, ri.name AS reader_name FROM lend_list l LEFT JOIN book_info b ON l.book_id = b.book_id LEFT JOIN reader_info ri ON l.reader_id = ri.reader_id WHERE l.reader_id = ? ORDER BY l.sernum";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowToBorrow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Borrow> findActive() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT l.*, b.book_name, ri.name AS reader_name FROM lend_list l LEFT JOIN book_info b ON l.book_id = b.book_id LEFT JOIN reader_info ri ON l.reader_id = ri.reader_id WHERE l.back_date IS NULL ORDER BY l.sernum";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowToBorrow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean borrowBook(long bookId, int readerId, String lendDate) {
        String sql1 = "INSERT INTO lend_list (book_id, reader_id, lend_date) VALUES (?, ?, ?)";
        String sql2 = "UPDATE book_info SET state = 1 WHERE book_id = ?";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sql1);
                 PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps1.setLong(1, bookId);
                ps1.setInt(2, readerId);
                ps1.setString(3, lendDate);
                ps1.executeUpdate();

                ps2.setLong(1, bookId);
                ps2.executeUpdate();
            }
            conn.commit();
            return true;
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
        return false;
    }

    public boolean returnBook(long sernum, long bookId) {
        String sql1 = "UPDATE lend_list SET back_date = CURDATE() WHERE sernum = ?";
        String sql2 = "UPDATE book_info SET state = 0 WHERE book_id = ?";
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(sql1);
                 PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps1.setLong(1, sernum);
                ps1.executeUpdate();

                ps2.setLong(1, bookId);
                ps2.executeUpdate();
            }
            conn.commit();
            return true;
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
        return false;
    }

    private Borrow rowToBorrow(ResultSet rs) throws SQLException {
        Borrow borrow = new Borrow();
        borrow.setSernum(rs.getLong("sernum"));
        borrow.setBookId(rs.getLong("book_id"));
        borrow.setReaderId(rs.getInt("reader_id"));
        borrow.setLendDate(rs.getString("lend_date"));
        borrow.setBackDate(rs.getString("back_date"));
        try { borrow.setBookName(rs.getString("book_name")); } catch (SQLException ignored) {}
        try { borrow.setReaderName(rs.getString("reader_name")); } catch (SQLException ignored) {}
        return borrow;
    }
}
