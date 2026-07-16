package util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


public class DBUtil {

    private static String url;
    private static String user;
    private static String password;

    
    static {
        try {
            
            InputStream is = DBUtil.class.getClassLoader()
                    .getResourceAsStream("db.properties");
            Properties props = new Properties();
            props.load(is);

            
            url = props.getProperty("jdbc.url");
            user = props.getProperty("jdbc.user");
            password = props.getProperty("jdbc.password");

            
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库配置加载失败，请检查 db.properties 文件");
        }
    }

    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    
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

    
    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }
}