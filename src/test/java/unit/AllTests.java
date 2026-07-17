package unit;

import integration.BookDaoTest;
import integration.BorrowDaoTest;
import integration.ServerIntegrationTest;
import integration.UserDaoTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 测试套件汇总 — 实验十一 项目总结与系统测试
 *
 * 按执行顺序分组：
 *   1. 单元测试（Model + Dispatcher + DBUtil）— 不依赖数据库连接
 *   2. 集成测试（DAO 层）— 需要数据库连接
 *   3. 集成测试（服务端通信）— 需要独立的 TCP 端口
 *
 * 运行方式：
 *   mvn test                           — 运行所有测试
 *   mvn test -Dtest=AllTests           — 仅运行此测试套件
 *   mvn test -Dtest=ModelTest          — 仅运行指定测试类
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        // ======== 阶段一：单元测试（无外部依赖，快速执行）========
        ModelTest.class,              // Model 实体类
        DispatcherTest.class,         // 命令分发器逻辑

        // ======== 阶段二：数据库集成测试（需MySQL连接）========
        DBUtilTest.class,             // 数据库连接工具
        BookDaoTest.class,            // 图书DAO CRUD
        UserDaoTest.class,            // 用户DAO 登录/注册
        BorrowDaoTest.class,          // 借阅DAO 事务

        // ======== 阶段三：服务端集成测试（需TCP端口）========
        ServerIntegrationTest.class,  // 服务端启停/通信/并发
})
public class AllTests {
    // 此类仅作为测试套件入口，无需实现任何方法
}
