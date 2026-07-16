package dao;

import model.Book;
import model.ClassInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import util.DBUtil;

public class BookDao {

    public List<Book> findAll() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.*, c.class_name FROM book_info b LEFT JOIN class_info c ON b.class_id = c.class_id ORDER BY b.book_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rowToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Book findById(Long bookId) {
        String sql = "SELECT b.*, c.class_name FROM book_info b LEFT JOIN class_info c ON b.class_id = c.class_id WHERE b.book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToBook(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Book> search(String keyword) {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.*, c.class_name FROM book_info b LEFT JOIN class_info c ON b.class_id = c.class_id WHERE b.book_name LIKE ? OR b.author LIKE ? OR b.ISBN LIKE ? ORDER BY b.book_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowToBook(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Book> findByClassId(Integer classId) {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT b.*, c.class_name FROM book_info b LEFT JOIN class_info c ON b.class_id = c.class_id WHERE b.class_id = ? ORDER BY b.book_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowToBook(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addBook(Book book) {
        String sql = "INSERT INTO book_info (book_id, book_name, author, publish, ISBN, introduction, book_language, price, pubdate, class_id, pressmark, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setLongOrNull(ps, 1, book.getBookId());
            ps.setString(2, book.getBookName());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublish());
            ps.setString(5, book.getIsbn());
            ps.setString(6, book.getIntroduction());
            ps.setString(7, book.getBookLanguage());
            ps.setBigDecimal(8, book.getPrice());
            ps.setString(9, book.getPubdate());
            setIntOrNull(ps, 10, book.getClassId());
            setIntOrNull(ps, 11, book.getPressmark());
            setIntOrNull(ps, 12, book.getState());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE book_info SET book_name = ?, author = ?, publish = ?, ISBN = ?, introduction = ?, book_language = ?, price = ?, pubdate = ?, class_id = ?, pressmark = ?, state = ? WHERE book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getBookName());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getPublish());
            ps.setString(4, book.getIsbn());
            ps.setString(5, book.getIntroduction());
            ps.setString(6, book.getBookLanguage());
            ps.setBigDecimal(7, book.getPrice());
            ps.setString(8, book.getPubdate());
            setIntOrNull(ps, 9, book.getClassId());
            setIntOrNull(ps, 10, book.getPressmark());
            setIntOrNull(ps, 11, book.getState());
            ps.setLong(12, book.getBookId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteBook(Long bookId) {
        String sql = "DELETE FROM book_info WHERE book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateState(Long bookId, Integer state) {
        String sql = "UPDATE book_info SET state = ? WHERE book_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, state);
            ps.setLong(2, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    public List<ClassInfo> getAllClasses() {
        List<ClassInfo> list = new ArrayList<>();
        String sql = "SELECT * FROM class_info ORDER BY class_id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ClassInfo(rs.getInt("class_id"), rs.getString("class_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }



    private Book rowToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getLong("book_id"));
        book.setBookName(rs.getString("book_name"));
        book.setAuthor(rs.getString("author"));
        book.setPublish(rs.getString("publish"));
        book.setIsbn(rs.getString("ISBN"));
        book.setIntroduction(rs.getString("introduction"));
        book.setBookLanguage(rs.getString("book_language"));
        book.setPrice(rs.getBigDecimal("price"));
        book.setPubdate(rs.getString("pubdate"));
        book.setClassId(rs.getObject("class_id") != null ? rs.getInt("class_id") : null);
        book.setPressmark(rs.getObject("pressmark") != null ? rs.getInt("pressmark") : null);
        book.setState(rs.getObject("state") != null ? rs.getInt("state") : null);
        try {
            book.setClassName(rs.getString("class_name"));
        } catch (SQLException ignored) {}
        return book;
    }

    private void setIntOrNull(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, java.sql.Types.INTEGER);
        }
    }

    private void setLongOrNull(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, java.sql.Types.BIGINT);
        }
    }
}