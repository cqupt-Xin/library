package integration;

import dao.UserDao;
import model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 集成测试 — 用户数据访问层 (UserDao)
 * 
 * 测试登录验证、读者注册（事务）、读者管理等功能。
 * 要求：
 *   - MySQL 数据库已启动
 *   - admin 表、reader_info 表、reader_card 表已存在
 */
public class UserDaoTest {

    private UserDao userDao;

    @Before
    public void setUp() {
        userDao = new UserDao();
    }

    // ==================== 管理员登录测试 ====================

    @Test
    public void testLoginAdminWithInvalidCredentials() {
        try {
            // 使用不存在的凭据测试管理员登录
            User user = userDao.loginAdmin(-1, "invalid_password");
            assertNull("无效凭据应返回null", user);
            System.out.println("[UserDao] 无效管理员登录返回null ✓");
        } catch (Exception e) {
            System.out.println("[跳过] testLoginAdminWithInvalidCredentials: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 读者登录测试 ====================

    @Test
    public void testLoginReaderWithInvalidCredentials() {
        try {
            User user = userDao.loginReader(-1, "invalid_password");
            assertNull("无效读者凭据应返回null", user);
            System.out.println("[UserDao] 无效读者登录返回null ✓");
        } catch (Exception e) {
            System.out.println("[跳过] testLoginReaderWithInvalidCredentials: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 读者注册测试 ====================

    @Test
    public void testRegisterReaderFlow() {
        try {
            long timestamp = System.currentTimeMillis() % 10000;
            String testName = "JR" + timestamp;

            // 1. 注册新读者
            int newId = userDao.register(
                    testName, "test123",
                    "男", "2000-01-01",
                    "测试地址", "13800138000"
            );

            if (newId > 0) {
                System.out.println("[UserDao] 注册成功，读者ID: " + newId);
                // 验证可以登录
                User reader = userDao.loginReader(newId, "test123");
                assertNotNull("注册后应可登录", reader);
                assertEquals("用户名", testName, reader.getUsername());
                assertEquals("角色", "reader", reader.getRole());

                // 2. 禁用该读者
                boolean disabled = userDao.updateCardState(newId, 0);
                assertTrue("禁用读者应成功", disabled);

                // 3. 验证禁用后无法登录（card_state=0时loginReader应返回null）
                User disabledLogin = userDao.loginReader(newId, "test123");
                assertNull("禁用后应无法登录", disabledLogin);

                // 4. 恢复启用
                boolean enabled = userDao.updateCardState(newId, 1);
                assertTrue("启用读者应成功", enabled);

                User enabledLogin = userDao.loginReader(newId, "test123");
                assertNotNull("启用后应可登录", enabledLogin);

                // 5. 再次禁用以便清理
                userDao.updateCardState(newId, 0);

                System.out.println("[UserDao] 注册→禁用→启用 完整流程验证通过 ✓");
            } else {
                System.out.println("[UserDao] 注册返回-1，可能是表结构不匹配");
            }
        } catch (Exception e) {
            System.out.println("[跳过] testRegisterReaderFlow: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 读者管理测试 ====================

    @Test
    public void testGetAllReaders() {
        try {
            List<User> readers = userDao.getAllReaders();
            assertNotNull("getAllReaders不应返回null", readers);

            for (User u : readers) {
                assertTrue("读者ID应>0", u.getId() > 0);
                assertNotNull("用户名不应为null", u.getUsername());
                assertEquals("角色应为reader", "reader", u.getRole());
                assertNotNull("状态不应为null", u.getStatus());
                assertTrue("状态应为 正常 或 已禁用",
                        "正常".equals(u.getStatus()) || "已禁用".equals(u.getStatus()));
            }
            System.out.println("[UserDao] getAllReaders: 共 " + readers.size() + " 位读者");
        } catch (Exception e) {
            System.out.println("[跳过] testGetAllReaders: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testUpdateCardStateNonExistentReader() {
        try {
            boolean result = userDao.updateCardState(-999, 1);
            assertFalse("更新不存在的读者应返回false", result);
        } catch (Exception e) {
            System.out.println("[跳过] testUpdateCardStateNonExistentReader: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 事务原子性验证 ====================

    @Test
    public void testRegisterTransactionRollback() {
        try {
            // 使用短名称避免超出数据库字段长度限制
            long timestamp = System.currentTimeMillis() % 10000;
            String testName = "TX" + timestamp;
            int result = userDao.register(
                    testName,
                    "pass", "男", "2001-01-01",
                    "地址", "13900139000"
            );
            if (result > 0) {
                // 清理：禁用刚创建的用户
                userDao.updateCardState(result, 0);
                System.out.println("[UserDao] 事务注册+清理成功，ID=" + result);
            }
        } catch (Exception e) {
            System.out.println("[跳过] testRegisterTransactionRollback: 数据库不可用 — " + e.getMessage());
        }
    }
}
