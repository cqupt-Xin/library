package util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库工具类
 * 作用：获取数据库连接、关闭资源
 */
public class DBUtil {

    private static String url;
    private static String user;
    private static String password;

    // 静态代码块：类加载时自动执行，读取 db.properties 配置
    static {
        try {
            // 1. 读取配置文件 db.properties
            InputStream is = DBUtil.class.getClassLoader()
                    .getResourceAsStream("db.properties");
            Properties props = new Properties();
            props.load(is);

            // 2. 获取配置信息
            url = props.getProperty("jdbc.url");
            user = props.getProperty("jdbc.user");
            password = props.getProperty("jdbc.password");

            // 3. 加载 MySQL 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库配置加载失败，请检查 db.properties 文件");
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 关闭所有资源（Connection, Statement, ResultSet）
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭 Connection 和 Statement（用于不需要 ResultSet 的情况）
     */
    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }
}