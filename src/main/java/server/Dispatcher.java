package server;

import com.google.gson.*;
import dao.BookDao;
import dao.BorrowDao;
import dao.UserDao;
import model.Book;
import model.Borrow;
import model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 命令分发器 — 实验九 C/S 模式核心
 * 负责解析客户端 JSON 命令并调用 DAO 层执行数据库操作
 * 每个 ClientHandler 线程各自持有一个独立的 Dispatcher 实例，保证线程安全
 */
public class Dispatcher {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final BookDao bookDao = new BookDao();
    private final BorrowDao borrowDao = new BorrowDao();
    private final UserDao userDao = new UserDao();

    // ==================== 命令路由 ====================

    public String dispatch(String raw) {
        String json = preprocess(raw);
        try {
            JsonObject req = GSON.fromJson(json, JsonObject.class);
            if (req == null || !req.has("command"))
                return err("缺少 'command' 字段");

            switch (req.get("command").getAsString().trim()) {
                // 系统
                case "help":          return cmdHelp();
                case "ping":          return cmdPing();

                // 用户 — 实验九新增
                case "login":         return cmdLogin(req);
                case "register":      return cmdRegister(req);

                // 图书
                case "findAllBooks":  return cmdFindAllBooks();
                case "findBookById":  return cmdFindBookById(req);
                case "searchBooks":   return cmdSearchBooks(req);
                case "findByClassId": return cmdFindByClassId(req);
                case "addBook":       return cmdAddBook(req);
                case "updateBook":    return cmdUpdateBook(req);
                case "deleteBook":    return cmdDeleteBook(req);
                case "getAllClasses": return cmdGetAllClasses();

                // 借阅 — 实验九新增
                case "borrowBook":          return cmdBorrowBook(req);
                case "returnBook":          return cmdReturnBook(req);
                case "findAllBorrows":      return cmdFindAllBorrows();
                case "findBorrowsByReaderId": return cmdFindBorrowsByReaderId(req);
                case "findActiveBorrows":   return cmdFindActiveBorrows();

                // 读者管理 — 实验九新增
                case "getAllReaders":  return cmdGetAllReaders();
                case "disableReader":  return cmdDisableReader(req);
                case "enableReader":   return cmdEnableReader(req);

                // 导入导出 — 实验九新增
                case "exportBooks":    return cmdExportBooks();
                case "importBooks":    return cmdImportBooks(req);

                default:
                    return err("未知命令，发送 {\"command\":\"help\"} 查看帮助");
            }
        } catch (JsonSyntaxException e) {
            return err("JSON 格式错误: " + e.getMessage());
        } catch (Exception e) {
            return err("服务器内部错误: " + e.getMessage());
        }
    }

    // ==================== 系统命令 ====================

    private String cmdHelp() {
        return ok("帮助信息", String.join("\n",
                "====== 图书管理系统远程命令 V7.1 ======",
                "help                              — 显示帮助",
                "ping                              — 测试连接",
                "",
                "--- 用户 ---",
                "login         data:{id,password} — 登录",
                "register      data:{name,passwd,sex,birth,address,telcode} — 注册",
                "",
                "--- 图书 ---",
                "addBook       data:{...}          — 添加图书",
                "deleteBook    data:{bookId:1}     — 删除图书",
                "updateBook    data:{...}          — 更新图书",
                "findAllBooks                       — 查询全部图书",
                "findBookById  data:{bookId:1}     — 按ID查询",
                "searchBooks   data:{keyword:\"x\"} — 模糊搜索",
                "findByClassId data:{classId:1}    — 按分类查询",
                "getAllClasses                      — 获取全部分类",
                "",
                "--- 借阅 ---",
                "borrowBook         data:{bookId,readerId}  — 借书",
                "returnBook         data:{sernum,bookId}    — 还书",
                "findAllBorrows                          — 全部借阅",
                "findBorrowsByReaderId data:{readerId}     — 某读者借阅",
                "findActiveBorrows                        — 未归还记录",
                "",
                "--- 读者管理 ---",
                "getAllReaders                        — 全部读者",
                "disableReader    data:{readerId}     — 禁用读者",
                "enableReader     data:{readerId}     — 启用读者",
                "",
                "--- 导入导出 ---",
                "exportBooks                          — 导出图书JSON",
                "importBooks   data:{books:[...]}      — 导入图书",
                "========================================="));
    }

    private String cmdPing() {
        return ok("pong", "服务器运行正常 — " + LocalDateTime.now());
    }

    // ==================== 用户命令 ====================

    private String cmdLogin(JsonObject req) {
        JsonObject d = data(req, "id", "password");
        if (d == null) return err("缺少必填字段: id, password");

        int id = d.get("id").getAsInt();
        String password = d.get("password").getAsString();

        // 先尝试管理员登录
        User admin = userDao.loginAdmin(id, password);
        if (admin != null) {
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", admin.getId());
            userJson.addProperty("username", admin.getUsername());
            userJson.addProperty("role", "admin");
            return ok("管理员登录成功", userJson);
        }

        // 再尝试读者登录
        User reader = userDao.loginReader(id, password);
        if (reader != null) {
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", reader.getId());
            userJson.addProperty("username", reader.getUsername());
            userJson.addProperty("role", "reader");
            return ok("读者登录成功", userJson);
        }

        return err("登录失败，请检查ID和密码");
    }

    private String cmdRegister(JsonObject req) {
        JsonObject d = data(req, "name", "passwd", "sex", "birth", "address", "telcode");
        if (d == null) return err("缺少必填字段: name, passwd, sex, birth, address, telcode");

        String name = d.get("name").getAsString();
        String passwd = d.get("passwd").getAsString();
        String sex = d.get("sex").getAsString();
        String birth = d.get("birth").getAsString();
        String address = d.get("address").getAsString();
        String telcode = d.get("telcode").getAsString();

        int newId = userDao.register(name, passwd, sex, birth, address, telcode);
        if (newId > 0) {
            JsonObject result = new JsonObject();
            result.addProperty("readerId", newId);
            return ok("注册成功，读者ID: " + newId, result);
        }
        return err("注册失败");
    }

    // ==================== 图书命令 ====================

    private String cmdAddBook(JsonObject req) {
        JsonObject d = data(req, "bookName", "author", "publish");
        if (d == null) return err("缺少必填字段: bookName, author, publish");
        Book book = toBook(d, null);
        return bookDao.addBook(book)
                ? ok("添加成功", "《" + book.getBookName() + "》已添加")
                : err("添加失败");
    }

    private String cmdDeleteBook(JsonObject req) {
        JsonObject d = data(req, "bookId");
        if (d == null) return err("缺少必填字段: bookId");
        long id = d.get("bookId").getAsLong();
        Book exist = bookDao.findById(id);
        if (exist == null) return err("图书 ID=" + id + " 不存在");
        return bookDao.deleteBook(id)
                ? ok("删除成功", "《" + exist.getBookName() + "》已删除")
                : err("删除失败，可能存在未归还借阅记录");
    }

    private String cmdUpdateBook(JsonObject req) {
        JsonObject d = data(req, "bookId");
        if (d == null) return err("缺少必填字段: bookId");
        long id = d.get("bookId").getAsLong();
        Book exist = bookDao.findById(id);
        if (exist == null) return err("图书 ID=" + id + " 不存在");
        Book book = toBook(d, id);
        merge(book, exist);
        return bookDao.updateBook(book)
                ? ok("更新成功", "图书 ID=" + id + " 已更新")
                : err("更新失败");
    }

    private String cmdFindAllBooks() {
        var books = bookDao.findAll();
        JsonObject r = new JsonObject();
        r.addProperty("count", books.size());
        r.add("books", GSON.toJsonTree(books));
        return ok("共 " + books.size() + " 条", r);
    }

    private String cmdFindBookById(JsonObject req) {
        JsonObject d = data(req, "bookId");
        if (d == null) return err("缺少必填字段: bookId");
        Book book = bookDao.findById(d.get("bookId").getAsLong());
        return book != null
                ? ok("查询成功", GSON.toJsonTree(book))
                : err("未找到该图书");
    }

    private String cmdSearchBooks(JsonObject req) {
        JsonObject d = data(req, "keyword");
        if (d == null) return err("缺少必填字段: keyword");
        var books = bookDao.search(d.get("keyword").getAsString());
        JsonObject r = new JsonObject();
        r.addProperty("keyword", d.get("keyword").getAsString());
        r.addProperty("count", books.size());
        r.add("books", GSON.toJsonTree(books));
        return ok("搜索到 " + books.size() + " 条", r);
    }

    private String cmdFindByClassId(JsonObject req) {
        JsonObject d = data(req, "classId");
        if (d == null) return err("缺少必填字段: classId");
        var books = bookDao.findByClassId(d.get("classId").getAsInt());
        JsonObject r = new JsonObject();
        r.addProperty("classId", d.get("classId").getAsInt());
        r.addProperty("count", books.size());
        r.add("books", GSON.toJsonTree(books));
        return ok("分类查询到 " + books.size() + " 条", r);
    }

    private String cmdGetAllClasses() {
        var classes = bookDao.getAllClasses();
        JsonArray arr = new JsonArray();
        for (var c : classes) {
            JsonObject o = new JsonObject();
            o.addProperty("classId", c.getClassId());
            o.addProperty("className", c.getClassName());
            arr.add(o);
        }
        JsonObject r = new JsonObject();
        r.addProperty("count", classes.size());
        r.add("classes", arr);
        return ok("共 " + classes.size() + " 个分类", r);
    }

    // ==================== 借阅命令 ====================

    private String cmdBorrowBook(JsonObject req) {
        JsonObject d = data(req, "bookId", "readerId");
        if (d == null) return err("缺少必填字段: bookId, readerId");

        long bookId = d.get("bookId").getAsLong();
        int readerId = d.get("readerId").getAsInt();

        // 检查图书状态
        Book book = bookDao.findById(bookId);
        if (book == null) return err("图书不存在");
        if (book.getState() != null && book.getState() == 1) return err("该图书已被借出");

        String lendDate = LocalDate.now().toString();
        boolean ok = borrowDao.borrowBook(bookId, readerId, lendDate);
        return ok ? ok("借阅成功", "《" + book.getBookName() + "》借阅成功") : err("借阅失败");
    }

    private String cmdReturnBook(JsonObject req) {
        JsonObject d = data(req, "sernum", "bookId");
        if (d == null) return err("缺少必填字段: sernum, bookId");

        long sernum = d.get("sernum").getAsLong();
        long bookId = d.get("bookId").getAsLong();

        boolean ok = borrowDao.returnBook(sernum, bookId);
        return ok ? ok("归还成功", "记录号 " + sernum + " 已归还") : err("归还失败");
    }

    private String cmdFindAllBorrows() {
        List<Borrow> list = borrowDao.findAll();
        JsonObject r = new JsonObject();
        r.addProperty("count", list.size());
        r.add("borrows", GSON.toJsonTree(list));
        return ok("共 " + list.size() + " 条借阅记录", r);
    }

    private String cmdFindBorrowsByReaderId(JsonObject req) {
        JsonObject d = data(req, "readerId");
        if (d == null) return err("缺少必填字段: readerId");
        int readerId = d.get("readerId").getAsInt();
        List<Borrow> list = borrowDao.findByReaderId(readerId);
        JsonObject r = new JsonObject();
        r.addProperty("count", list.size());
        r.add("borrows", GSON.toJsonTree(list));
        return ok("共 " + list.size() + " 条借阅记录", r);
    }

    private String cmdFindActiveBorrows() {
        List<Borrow> list = borrowDao.findActive();
        JsonObject r = new JsonObject();
        r.addProperty("count", list.size());
        r.add("borrows", GSON.toJsonTree(list));
        return ok("共 " + list.size() + " 条未归还记录", r);
    }

    // ==================== 读者管理命令 ====================

    private String cmdGetAllReaders() {
        List<User> readers = userDao.getAllReaders();
        JsonArray arr = new JsonArray();
        for (User u : readers) {
            JsonObject o = new JsonObject();
            o.addProperty("readerId", u.getId());
            o.addProperty("name", u.getUsername());
            o.addProperty("cardState", "正常".equals(u.getStatus()) ? 1 : 0);
            arr.add(o);
        }
        JsonObject r = new JsonObject();
        r.addProperty("count", readers.size());
        r.add("readers", arr);
        return ok("共 " + readers.size() + " 位读者", r);
    }

    private String cmdDisableReader(JsonObject req) {
        JsonObject d = data(req, "readerId");
        if (d == null) return err("缺少必填字段: readerId");
        int readerId = d.get("readerId").getAsInt();
        return userDao.updateCardState(readerId, 0)
                ? ok("已禁用", "读者 ID=" + readerId + " 已禁用")
                : err("禁用失败");
    }

    private String cmdEnableReader(JsonObject req) {
        JsonObject d = data(req, "readerId");
        if (d == null) return err("缺少必填字段: readerId");
        int readerId = d.get("readerId").getAsInt();
        return userDao.updateCardState(readerId, 1)
                ? ok("已启用", "读者 ID=" + readerId + " 已启用")
                : err("启用失败");
    }

    // ==================== 导入导出命令 ====================

    private String cmdExportBooks() {
        var books = bookDao.findAll();
        JsonObject r = new JsonObject();
        r.addProperty("type", "books");
        r.addProperty("exportTime", LocalDateTime.now().toString());
        r.addProperty("recordCount", books.size());
        JsonArray arr = new JsonArray();
        for (Book b : books) {
            JsonObject o = new JsonObject();
            o.addProperty("bookId", b.getBookId());
            o.addProperty("bookName", b.getBookName());
            o.addProperty("author", b.getAuthor());
            o.addProperty("publish", b.getPublish());
            o.addProperty("isbn", b.getIsbn());
            o.addProperty("introduction", b.getIntroduction());
            o.addProperty("bookLanguage", b.getBookLanguage());
            o.addProperty("price", b.getPrice() != null ? b.getPrice().toString() : null);
            o.addProperty("pubdate", b.getPubdate());
            o.addProperty("classId", b.getClassId());
            o.addProperty("className", b.getClassName());
            o.addProperty("pressmark", b.getPressmark());
            o.addProperty("state", b.getState());
            arr.add(o);
        }
        r.add("data", arr);
        return ok("导出成功", r);
    }

    private String cmdImportBooks(JsonObject req) {
        if (!req.has("data") || req.get("data").isJsonNull())
            return err("缺少 data 字段");
        JsonObject d = req.getAsJsonObject("data");
        if (!d.has("books") || d.get("books").isJsonNull())
            return err("缺少 books 数组");

        JsonArray books = d.getAsJsonArray("books");
        int newCount = 0, updateCount = 0, failCount = 0;
        JsonArray errors = new JsonArray();

        for (int i = 0; i < books.size(); i++) {
            try {
                JsonObject obj = books.get(i).getAsJsonObject();
                if (!obj.has("bookId") || !obj.has("bookName")
                        || !obj.has("author") || !obj.has("publish")) {
                    failCount++;
                    errors.add("第" + (i + 1) + "条: 缺少必填字段");
                    continue;
                }

                Book book = new Book();
                book.setBookId(obj.get("bookId").getAsLong());
                book.setBookName(obj.get("bookName").getAsString());
                book.setAuthor(obj.get("author").getAsString());
                book.setPublish(obj.get("publish").getAsString());
                if (has(obj, "isbn")) book.setIsbn(obj.get("isbn").getAsString());
                if (has(obj, "introduction")) book.setIntroduction(obj.get("introduction").getAsString());
                if (has(obj, "bookLanguage")) book.setBookLanguage(obj.get("bookLanguage").getAsString());
                if (has(obj, "price")) try {
                    book.setPrice(new BigDecimal(obj.get("price").getAsString()));
                } catch (Exception ignored) {}
                if (has(obj, "pubdate")) book.setPubdate(obj.get("pubdate").getAsString());
                if (has(obj, "classId")) book.setClassId(obj.get("classId").getAsInt());
                if (has(obj, "pressmark")) book.setPressmark(obj.get("pressmark").getAsInt());
                if (has(obj, "state")) book.setState(obj.get("state").getAsInt());

                if (bookDao.findById(book.getBookId()) != null) {
                    if (bookDao.updateBook(book)) updateCount++;
                    else { failCount++; errors.add("第" + (i + 1) + "条: 更新失败"); }
                } else {
                    if (bookDao.addBook(book)) newCount++;
                    else { failCount++; errors.add("第" + (i + 1) + "条: 新增失败"); }
                }
            } catch (Exception e) {
                failCount++;
                errors.add("第" + (i + 1) + "条: " + e.getMessage());
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("total", books.size());
        result.addProperty("newCount", newCount);
        result.addProperty("updateCount", updateCount);
        result.addProperty("failCount", failCount);
        result.add("errors", errors);
        return ok("导入完成: 新增" + newCount + " 更新" + updateCount + " 失败" + failCount, result);
    }

    // ==================== 辅助方法 ====================

    private JsonObject data(JsonObject req, String... required) {
        if (!req.has("data") || req.get("data").isJsonNull()) return null;
        JsonObject d = req.getAsJsonObject("data");
        for (String f : required)
            if (!d.has(f) || d.get(f).isJsonNull()) return null;
        return d;
    }

    private Book toBook(JsonObject d, Long bookId) {
        Book b = new Book();
        if (bookId != null) b.setBookId(bookId);
        if (has(d, "bookName"))     b.setBookName(d.get("bookName").getAsString());
        if (has(d, "author"))       b.setAuthor(d.get("author").getAsString());
        if (has(d, "publish"))      b.setPublish(d.get("publish").getAsString());
        if (has(d, "isbn"))         b.setIsbn(d.get("isbn").getAsString());
        if (has(d, "introduction")) b.setIntroduction(d.get("introduction").getAsString());
        if (has(d, "bookLanguage")) b.setBookLanguage(d.get("bookLanguage").getAsString());
        if (has(d, "price")) try {
            b.setPrice(new BigDecimal(d.get("price").getAsString()));
        } catch (NumberFormatException ignored) {}
        if (has(d, "pubdate"))      b.setPubdate(d.get("pubdate").getAsString());
        if (has(d, "classId"))      b.setClassId(d.get("classId").getAsInt());
        if (has(d, "pressmark"))    b.setPressmark(d.get("pressmark").getAsInt());
        if (has(d, "state"))        b.setState(d.get("state").getAsInt());
        return b;
    }

    private void merge(Book src, Book exist) {
        if (src.getBookName() == null)     src.setBookName(exist.getBookName());
        if (src.getAuthor() == null)       src.setAuthor(exist.getAuthor());
        if (src.getPublish() == null)      src.setPublish(exist.getPublish());
        if (src.getIsbn() == null)         src.setIsbn(exist.getIsbn());
        if (src.getIntroduction() == null) src.setIntroduction(exist.getIntroduction());
        if (src.getBookLanguage() == null) src.setBookLanguage(exist.getBookLanguage());
        if (src.getPrice() == null)        src.setPrice(exist.getPrice());
        if (src.getPubdate() == null)      src.setPubdate(exist.getPubdate());
        if (src.getClassId() == null)      src.setClassId(exist.getClassId());
        if (src.getPressmark() == null)    src.setPressmark(exist.getPressmark());
        if (src.getState() == null)        src.setState(exist.getState());
    }

    private boolean has(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull();
    }

    private String preprocess(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return raw.replace('\u201c', '"').replace('\u201d', '"')
                  .replace('\u2018', '\'').replace('\u2019', '\'')
                  .replace('\uff1a', ':').replace('\uff0c', ',')
                  .replace("\uFEFF", "").trim();
    }

    // ==================== JSON 响应构建 ====================

    private String ok(String msg, Object data) {
        JsonObject r = new JsonObject();
        r.addProperty("success", true);
        r.addProperty("message", msg);
        if (data instanceof JsonElement) r.add("data", (JsonElement) data);
        else if (data instanceof String) r.addProperty("data", (String) data);
        else r.add("data", GSON.toJsonTree(data));
        return GSON.toJson(r);
    }

    private String err(String msg) {
        JsonObject r = new JsonObject();
        r.addProperty("success", false);
        r.addProperty("message", msg);
        return GSON.toJson(r);
    }
}
