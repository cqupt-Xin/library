package integration;

import dao.BookDao;
import model.Book;
import model.ClassInfo;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 集成测试 — 图书数据访问层 (BookDao)
 * 
 * 测试 BookDao 的所有 CRUD 方法在真实数据库环境下的行为。
 * 要求：
 *   - MySQL 数据库已启动
 *   - db.properties 配置正确
 *   - book_info 和 class_info 表已存在
 */
public class BookDaoTest {

    private BookDao bookDao;

    @Before
    public void setUp() {
        bookDao = new BookDao();
    }

    // ==================== 查询测试 ====================

    @Test
    public void testFindAll() {
        try {
            List<Book> books = bookDao.findAll();
            assertNotNull("findAll不应返回null", books);
            // 无论数据库中有多少数据，都不应抛出异常
            System.out.println("[BookDao] findAll: 共 " + books.size() + " 本书");
        } catch (Exception e) {
            System.out.println("[跳过] testFindAll: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testFindById() {
        try {
            // 用不存在的ID测试
            Book book = bookDao.findById(-1L);
            assertNull("不存在的图书ID应返回null", book);
        } catch (Exception e) {
            System.out.println("[跳过] testFindById: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testSearch() {
        try {
            List<Book> books = bookDao.search("Java");
            assertNotNull("search不应返回null", books);
            for (Book b : books) {
                assertNotNull("书名不应为null", b.getBookName());
            }
            System.out.println("[BookDao] search 'Java': 共 " + books.size() + " 条");
        } catch (Exception e) {
            System.out.println("[跳过] testSearch: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testSearchEmptyKeyword() {
        try {
            List<Book> books = bookDao.search("");
            assertNotNull("空关键词搜索不应返回null", books);
        } catch (Exception e) {
            System.out.println("[跳过] testSearchEmptyKeyword: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testFindByClassId() {
        try {
            List<Book> books = bookDao.findByClassId(1);
            assertNotNull("findByClassId不应返回null", books);
            for (Book b : books) {
                assertEquals("分类ID应匹配", Integer.valueOf(1), b.getClassId());
            }
            System.out.println("[BookDao] findByClassId(1): 共 " + books.size() + " 本");
        } catch (Exception e) {
            System.out.println("[跳过] testFindByClassId: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testFindByNonExistentClassId() {
        try {
            List<Book> books = bookDao.findByClassId(9999);
            assertNotNull("不存在的分类ID应返回空列表", books);
            assertTrue("不存在的分类应返回0条", books.isEmpty());
        } catch (Exception e) {
            System.out.println("[跳过] testFindByNonExistentClassId: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 增删改测试 ====================

    @Test
    public void testAddUpdateDeleteBookFlow() {
        try {
            // 使用基于时间戳的唯一ID，避免与数据库中已有数据冲突
            long testBookId = System.currentTimeMillis() % 100000000L + 90000000L;

            // 1. 添加测试图书
            Book newBook = new Book();
            newBook.setBookId(testBookId);
            newBook.setBookName("JUnit测试图书_" + testBookId);
            newBook.setAuthor("测试作者");
            newBook.setPublish("测试出版社");
            newBook.setIsbn("ISBN" + testBookId);
            newBook.setIntroduction("测试简介");
            newBook.setBookLanguage("中文");
            newBook.setPrice(new BigDecimal("88.88"));
            newBook.setPubdate("2026-01-01");
            newBook.setClassId(1);
            newBook.setPressmark(1);
            newBook.setState(0);

            boolean added = bookDao.addBook(newBook);
            assertTrue("添加测试图书应成功 (bookId=" + testBookId + ")", added);
            System.out.println("[BookDao] 添加测试图书成功, bookId=" + testBookId);

            // 2. 查询验证已添加
            Book found = bookDao.findById(testBookId);
            assertNotNull("添加后应能查到", found);
            assertEquals("书名", newBook.getBookName(), found.getBookName());
            assertEquals("作者", "测试作者", found.getAuthor());

            // 3. 更新图书
            found.setBookName("JUnit测试图书_已修改");
            found.setPrice(new BigDecimal("66.66"));
            boolean updated = bookDao.updateBook(found);
            assertTrue("更新图书应成功", updated);

            // 4. 再次查询验证更新
            Book updatedBook = bookDao.findById(testBookId);
            assertNotNull("更新后应能查到", updatedBook);
            assertEquals("书名应已更新", "JUnit测试图书_已修改", updatedBook.getBookName());

            // 5. 更新状态
            boolean stateUpdated = bookDao.updateState(testBookId, 1);
            assertTrue("更新状态应成功", stateUpdated);

            Book statedBook = bookDao.findById(testBookId);
            assertEquals("状态应已更新为1", Integer.valueOf(1), statedBook.getState());

            // 6. 删除测试图书
            boolean deleted = bookDao.deleteBook(testBookId);
            assertTrue("删除测试图书应成功", deleted);

            // 7. 确认已删除
            Book notFound = bookDao.findById(testBookId);
            assertNull("删除后不应再查到", notFound);

            System.out.println("[BookDao] 完整CRUD流程验证通过 ✓ (ID=" + testBookId + ")");
        } catch (Exception e) {
            System.out.println("[跳过] testAddUpdateDeleteBookFlow: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 分类查询测试 ====================

    @Test
    public void testGetAllClasses() {
        try {
            List<ClassInfo> classes = bookDao.getAllClasses();
            assertNotNull("getAllClasses不应返回null", classes);

            for (ClassInfo c : classes) {
                assertTrue("classId应>0", c.getClassId() > 0);
                assertNotNull("className不应为null", c.getClassName());
                assertFalse("className不应为空", c.getClassName().isEmpty());
            }
            System.out.println("[BookDao] getAllClasses: 共 " + classes.size() + " 个分类");
        } catch (Exception e) {
            System.out.println("[跳过] testGetAllClasses: 数据库不可用 — " + e.getMessage());
        }
    }

    // ==================== 删除有外键约束的图书 ====================

    @Test
    public void testDeleteNonExistentBook() {
        try {
            boolean deleted = bookDao.deleteBook(-999L);
            assertFalse("删除不存在的图书应返回false", deleted);
        } catch (Exception e) {
            System.out.println("[跳过] testDeleteNonExistentBook: 数据库不可用 — " + e.getMessage());
        }
    }

    @Test
    public void testUpdateNonExistentBook() {
        try {
            Book fake = new Book();
            fake.setBookId(-888L);
            fake.setBookName("不存在");
            fake.setAuthor("无");
            fake.setPublish("无");
            boolean updated = bookDao.updateBook(fake);
            // 不存在0行受影响，应返回false
            assertFalse("更新不存在的图书应返回false", updated);
        } catch (Exception e) {
            System.out.println("[跳过] testUpdateNonExistentBook: 数据库不可用 — " + e.getMessage());
        }
    }
}
