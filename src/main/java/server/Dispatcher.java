package server;

import com.google.gson.*;
import dao.BookDao;
import model.Book;

import java.math.BigDecimal;

/**
 * 命令调度器 — JSON 命令解析 → 路由 → 执行 → 响应
 */
public class Dispatcher {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final BookDao bookDao = new BookDao();

    public String dispatch(String raw) {
        String json = preprocess(raw);
        try {
            JsonObject req = GSON.fromJson(json, JsonObject.class);
            if (req == null || !req.has("command"))
                return err("缺少 'command' 字段");

            switch (req.get("command").getAsString().trim()) {
                case "help":          return cmdHelp();
                case "ping":          return cmdPing();
                case "addBook":       return cmdAddBook(req);
                case "deleteBook":    return cmdDeleteBook(req);
                case "updateBook":    return cmdUpdateBook(req);
                case "findAllBooks":  return cmdFindAllBooks();
                case "findBookById":  return cmdFindBookById(req);
                case "searchBooks":   return cmdSearchBooks(req);
                case "findByClassId": return cmdFindByClassId(req);
                case "getAllClasses": return cmdGetAllClasses();
                default:
                    return err("未知命令，发送 {\"command\":\"help\"} 查看帮助");
            }
        } catch (JsonSyntaxException e) {
            return err("JSON 格式错误: " + e.getMessage());
        } catch (Exception e) {
            return err("服务器内部错误: " + e.getMessage());
        }
    }

    // ==================== 命令 ====================

    private String cmdHelp() {
        return ok("帮助信息", String.join("\n",
                "====== 图书管理系统远程命令 ======",
                "help                              — 显示帮助",
                "ping                              — 测试连接",
                "addBook       data:{...}          — 添加图书",
                "deleteBook    data:{bookId:1}     — 删除图书",
                "updateBook    data:{...}          — 更新图书",
                "findAllBooks                       — 查询全部图书",
                "findBookById  data:{bookId:1}     — 按ID查询",
                "searchBooks   data:{keyword:\"x\"} — 模糊搜索",
                "findByClassId data:{classId:1}    — 按分类查询",
                "getAllClasses                      — 获取全部分类",
                "=========================================",
                "必填字段: bookName, author, publish",
                "所有命令以一行 JSON 发送"));
    }

    private String cmdPing() {
        return ok("pong", "服务器运行正常 — " + java.time.LocalDateTime.now());
    }

    // -- 增 --

    private String cmdAddBook(JsonObject req) {
        JsonObject d = data(req, "bookName", "author", "publish");
        if (d == null) return err("缺少必填字段: bookName, author, publish");
        Book book = toBook(d, null);
        return bookDao.addBook(book)
                ? ok("添加成功", "《" + book.getBookName() + "》已添加")
                : err("添加失败");
    }

    // -- 删 --

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

    // -- 改 --

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

    // -- 查 --

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

    // ==================== 工具 ====================

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
