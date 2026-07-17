package unit;

import model.Book;
import model.User;
import model.Borrow;
import model.ClassInfo;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 单元测试 — 数据模型（Model）类测试
 * 验证所有实体类的构造器、getter/setter、toString 方法
 */
public class ModelTest {

    // ==================== Book 模型测试 ====================

    @Test
    public void testBookDefaultConstructor() {
        Book book = new Book();
        assertNull("bookId 默认应为 null", book.getBookId());
        assertNull("bookName 默认应为 null", book.getBookName());
        assertNull("author 默认应为 null", book.getAuthor());
        assertNull("publish 默认应为 null", book.getPublish());
        assertNull("isbn 默认应为 null", book.getIsbn());
        assertNull("price 默认应为 null", book.getPrice());
        assertNull("classId 默认应为 null", book.getClassId());
        assertNull("state 默认应为 null", book.getState());
    }

    @Test
    public void testBookGetterSetter() {
        Book book = new Book();
        book.setBookId(1001L);
        book.setBookName("Java编程思想");
        book.setAuthor("Bruce Eckel");
        book.setPublish("机械工业出版社");
        book.setIsbn("7111212506");
        book.setIntroduction("Java经典入门书籍");
        book.setBookLanguage("中文");
        book.setPrice(new BigDecimal("99.00"));
        book.setPubdate("2023-01-15");
        book.setClassId(1);
        book.setClassName("计算机科学");
        book.setPressmark(101);
        book.setState(0);

        assertEquals("bookId", Long.valueOf(1001L), book.getBookId());
        assertEquals("bookName", "Java编程思想", book.getBookName());
        assertEquals("author", "Bruce Eckel", book.getAuthor());
        assertEquals("publish", "机械工业出版社", book.getPublish());
        assertEquals("isbn", "7111212506", book.getIsbn());
        assertEquals("price", new BigDecimal("99.00"), book.getPrice());
        assertEquals("classId", Integer.valueOf(1), book.getClassId());
        assertEquals("className", "计算机科学", book.getClassName());
        assertEquals("state", Integer.valueOf(0), book.getState());
    }

    @Test
    public void testBookToString() {
        Book book = new Book();
        book.setBookId(1L);
        book.setBookName("测试书");
        book.setAuthor("测试作者");
        String str = book.toString();
        assertTrue("toString 应包含 bookId", str.contains("1"));
        assertTrue("toString 应包含 bookName", str.contains("测试书"));
        assertTrue("toString 应包含 author", str.contains("测试作者"));
    }

    @Test
    public void testBookNullableFields() {
        // 验证可选字段可为 null
        Book book = new Book();
        book.setBookId(null);
        book.setIsbn(null);
        book.setIntroduction(null);
        book.setBookLanguage(null);
        book.setPrice(null);
        book.setPubdate(null);
        book.setClassId(null);
        book.setClassName(null);
        book.setPressmark(null);
        book.setState(null);

        assertNull(book.getBookId());
        assertNull(book.getIsbn());
        assertNull(book.getIntroduction());
        assertNull(book.getPrice());
        assertNull(book.getClassId());
        assertNull(book.getPressmark());
        assertNull(book.getState());
    }

    @Test
    public void testBookStateEnumValues() {
        // 验证状态值的约定：0=在馆，1=已借出
        Book book = new Book();
        book.setState(0);
        assertEquals(Integer.valueOf(0), book.getState());

        book.setState(1);
        assertEquals(Integer.valueOf(1), book.getState());
    }

    // ==================== User 模型测试 ====================

    @Test
    public void testUserDefaultConstructor() {
        User user = new User();
        assertEquals("默认id", 0, user.getId());
        assertNull("默认username", user.getUsername());
        assertNull("默认password", user.getPassword());
        assertNull("默认role", user.getRole());
        assertNull("默认status", user.getStatus());
    }

    @Test
    public void testUserParameterizedConstructor() {
        User user = new User(1001, "admin", "admin");
        assertEquals("id", 1001, user.getId());
        assertEquals("username", "admin", user.getUsername());
        assertEquals("role", "admin", user.getRole());
    }

    @Test
    public void testUserGetterSetter() {
        User user = new User();
        user.setId(2001);
        user.setUsername("张三");
        user.setPassword("pass123");
        user.setRole("reader");
        user.setStatus("正常");

        assertEquals("id", 2001, user.getId());
        assertEquals("username", "张三", user.getUsername());
        assertEquals("password", "pass123", user.getPassword());
        assertEquals("role", "reader", user.getRole());
        assertEquals("status", "正常", user.getStatus());
    }

    @Test
    public void testUserRoles() {
        // 验证角色值约定
        User admin = new User(1, "管理员", "admin");
        assertEquals("管理员角色应为admin", "admin", admin.getRole());

        User reader = new User(10000001, "读者", "reader");
        assertEquals("读者角色应为reader", "reader", reader.getRole());
    }

    @Test
    public void testUserStatusValues() {
        // 验证状态值约定
        User activeUser = new User();
        activeUser.setStatus("正常");
        assertEquals("正常", activeUser.getStatus());

        User disabledUser = new User();
        disabledUser.setStatus("已禁用");
        assertEquals("已禁用", disabledUser.getStatus());
    }

    // ==================== Borrow 模型测试 ====================

    @Test
    public void testBorrowDefaultConstructor() {
        Borrow borrow = new Borrow();
        assertNull("sernum 默认应为 null", borrow.getSernum());
        assertNull("bookId 默认应为 null", borrow.getBookId());
        assertEquals("readerId 默认应为 0", 0, borrow.getReaderId());
        assertNull("lendDate 默认应为 null", borrow.getLendDate());
        assertNull("backDate 默认应为 null", borrow.getBackDate());
    }

    @Test
    public void testBorrowGetterSetter() {
        Borrow borrow = new Borrow();
        borrow.setSernum(5001L);
        borrow.setBookId(1001L);
        borrow.setReaderId(20001);
        borrow.setLendDate("2026-07-01");
        borrow.setBackDate("2026-07-15");
        borrow.setBookName("Java编程思想");
        borrow.setReaderName("张三");

        assertEquals("sernum", Long.valueOf(5001L), borrow.getSernum());
        assertEquals("bookId", Long.valueOf(1001L), borrow.getBookId());
        assertEquals("readerId", 20001, borrow.getReaderId());
        assertEquals("lendDate", "2026-07-01", borrow.getLendDate());
        assertEquals("backDate", "2026-07-15", borrow.getBackDate());
        assertEquals("bookName", "Java编程思想", borrow.getBookName());
        assertEquals("readerName", "张三", borrow.getReaderName());
    }

    @Test
    public void testBorrowBackDateCanBeNull() {
        // 未归还的借阅记录 backDate 为 null
        Borrow borrow = new Borrow();
        borrow.setBackDate(null);
        assertNull(borrow.getBackDate());
    }

    @Test
    public void testBorrowToString() {
        Borrow borrow = new Borrow();
        borrow.setSernum(1L);
        borrow.setBookId(100L);
        String str = borrow.toString();
        assertNotNull("toString 不应返回 null", str);
    }

    // ==================== ClassInfo 模型测试 ====================

    @Test
    public void testClassInfoDefaultConstructor() {
        ClassInfo classInfo = new ClassInfo();
        assertEquals("默认classId", 0, classInfo.getClassId());
        assertNull("默认className", classInfo.getClassName());
    }

    @Test
    public void testClassInfoParameterizedConstructor() {
        ClassInfo classInfo = new ClassInfo(1, "计算机科学");
        assertEquals("classId", 1, classInfo.getClassId());
        assertEquals("className", "计算机科学", classInfo.getClassName());
    }

    @Test
    public void testClassInfoGetterSetter() {
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassId(2);
        classInfo.setClassName("文学小说");

        assertEquals("classId", 2, classInfo.getClassId());
        assertEquals("className", "文学小说", classInfo.getClassName());
    }

    @Test
    public void testClassInfoToString() {
        ClassInfo classInfo = new ClassInfo(3, "历史哲学");
        String str = classInfo.toString();
        assertNotNull("toString 不应返回 null", str);
    }

    // ==================== 综合边界值测试 ====================

    @Test
    public void testBookIdEdgeValues() {
        Book book = new Book();
        book.setBookId(Long.MAX_VALUE);
        assertEquals("最大Long值", Long.valueOf(Long.MAX_VALUE), book.getBookId());

        book.setBookId(1L);
        assertEquals("最小正整数", Long.valueOf(1L), book.getBookId());
    }

    @Test
    public void testBookPricePrecision() {
        Book book = new Book();
        book.setPrice(new BigDecimal("99.99"));
        assertEquals("价格精度", new BigDecimal("99.99"), book.getPrice());

        book.setPrice(new BigDecimal("0.01"));
        assertEquals("最小价格", new BigDecimal("0.01"), book.getPrice());

        book.setPrice(BigDecimal.ZERO);
        assertEquals("零价格", BigDecimal.ZERO, book.getPrice());
    }

    @Test
    public void testBookEmptyStrings() {
        Book book = new Book();
        book.setBookName("");
        book.setAuthor("");
        book.setPublish("");
        // 空字符串是合法的（由业务层校验）
        assertEquals("", book.getBookName());
        assertEquals("", book.getAuthor());
    }
}
