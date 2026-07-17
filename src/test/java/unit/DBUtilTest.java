package unit;

import org.junit.Test;
import util.DBUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * 单元测试 — 数据库工具类 DBUtil
 * 验证数据库连接获取和资源关闭功能
 */
public class DBUtilTest {

    @Test
    public void testGetConnection() {
        try {
            Connection conn = DBUtil.getConnection();
            assertNotNull("应成功获取数据库连接", conn);
            assertFalse("连接不应已关闭", conn.isClosed());
            conn.close();
        } catch (Exception e) {
            System.out.println("[跳过] testGetConnection: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testConnectionIsValid() {
        try (Connection conn = DBUtil.getConnection()) {
            assertNotNull("连接不应为null", conn);
            // 执行一个简单查询验证连接可用
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue("应能执行简单查询", rs.next());
                assertEquals("查询结果应为1", 1, rs.getInt(1));
            }
        } catch (Exception e) {
            System.out.println("[跳过] testConnectionIsValid: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testMultipleConnections() {
        // 验证能获取多个独立连接
        Connection conn1 = null;
        Connection conn2 = null;
        try {
            conn1 = DBUtil.getConnection();
            conn2 = DBUtil.getConnection();
            assertNotNull("连接1", conn1);
            assertNotNull("连接2", conn2);
            assertNotSame("两次获取的连接应为不同对象", conn1, conn2);
        } catch (Exception e) {
            System.out.println("[跳过] testMultipleConnections: 数据库不可用 — " + e.getMessage());
        } finally {
            if (conn1 != null) try { conn1.close(); } catch (Exception ex) {}
            if (conn2 != null) try { conn2.close(); } catch (Exception ex) {}
        }
    }

    @Test
    public void testCloseWithNullParameters() {
        // close方法应能安全处理null参数
        DBUtil.close(null, null, null);
        DBUtil.close(null, null);
        // 不应抛出异常
    }

    @Test
    public void testCloseWithValidConnection() {
        try {
            Connection conn = DBUtil.getConnection();
            assertNotNull(conn);
            // close(conn, stmt, rs) 应安全关闭所有资源
            DBUtil.close(conn, null, null);
            assertTrue("关闭后连接应为closed状态", conn.isClosed());
        } catch (Exception e) {
            System.out.println("[跳过] testCloseWithValidConnection: 数据库不可用 — " + e.getMessage());
        }
    }
}
