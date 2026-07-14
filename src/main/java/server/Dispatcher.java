package server;

import com.google.gson.*;
import dao.BookDao;
import model.Book;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 命令调度器 — 核心：JSON 命令解析 → 路由 → 执行 → 响应
 *
 * 支持的 JSON 命令格式（每行一个）:
 *   {"command":"addBook",     "data":{"bookName":"...","author":"...","publish":"...","isbn":"...",...}}
 *   {"command":"deleteBook",  "data":{"bookId":1}}
 *   {"command":"updateBook",  "data":{"bookId":1,"bookName":"...","author":"...",...}}
 *   {"command":"findAllBooks"}
 *   {"command":"findBookById", "data":{"bookId":1}}
 *   {"command":"searchBooks", "data":{"keyword":"..."}}
 *   {"command":"findByClassId","data":{"classId":1}}
 *   {"command":"getAllClasses"}
 *   {"command":"ping"}
 *   {"command":"help"}
 */
public class Dispatcher {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Gson LENIENT_GSON = new GsonBuilder().setLenient().serializeNulls().create();
    private final BookDao bookDao = new BookDao();

    /** 命令路由表: command名 → 处理函数 */
    private final Map<String, Function<JsonObject, String>> routes = new HashMap<>();

    public Dispatcher() {
        initRoutes();
    }

    /**
     * 注册所有命令路由
     */
    private void initRoutes() {
        routes.put("help",          this::cmdHelp);
        routes.put("ping",          this::cmdPing);
        routes.put("addBook",       this::cmdAddBook);
        routes.put("deleteBook",    this::cmdDeleteBook);
        routes.put("updateBook",    this::cmdUpdateBook);
        routes.put("findAllBooks",  this::cmdFindAllBooks);
        routes.put("findBookById",  this::cmdFindBookById);
        routes.put("searchBooks",   this::cmdSearchBooks);
        routes.put("findByClassId", this::cmdFindByClassId);
        routes.put("getAllClasses", this::cmdGetAllClasses);
    }

    // ========================= 入口 =========================

    /**
     * 解析一行 JSON 命令并返回一行 JSON 响应
     * @param rawJson 原始 JSON 字符串
     * @return JSON 响应字符串（单行）
     */
    public String dispatch(String rawJson) {
        // 预处理：修复常见 JSON 格式问题
        String json = preprocessJson(rawJson);

        try {
            JsonObject request;
            try {
                request = GSON.fromJson(json, JsonObject.class);
            } catch (JsonSyntaxException e1) {
                // 宽松模式重试（容忍末尾逗号等）
                try {
                    request = LENIENT_GSON.fromJson(json, JsonObject.class);
                } catch (JsonSyntaxException e2) {
                    throw e1; // 抛原始异常，含准确行列信息
                }
            }

            if (request == null || !request.has("command")) {
                return error("缺少 'command' 字段，请发送 {\"command\":\"help\"} 查看帮助");
            }

            String command = request.get("command").getAsString().trim();
            Function<JsonObject, String> handler = routes.get(command);

            if (handler == null) {
                return error("未知命令: '" + command + "'，请发送 {\"command\":\"help\"} 查看支持的命令");
            }

            return handler.apply(request);

        } catch (JsonSyntaxException e) {
            return error("JSON 格式错误: " + e.getMessage() +
                    "，请确保发送的是合法的 JSON 格式（一行一个命令）");
        } catch (Exception e) {
            return error("服务器内部错误: " + e.getMessage());
        }
    }

    // ========================= 命令实现 =========================

    private String cmdHelp(JsonObject req) {
        String help = "====== 图书管理系统远程命令 ======\n"
                + "{\"command\":\"help\"}                    — 显示帮助\n"
                + "{\"command\":\"ping\"}                    — 测试连接\n"
                + "{\"command\":\"addBook\",    \"data\":{...}} — 添加图书\n"
                + "{\"command\":\"deleteBook\", \"data\":{\"bookId\":1}} — 删除图书\n"
                + "{\"command\":\"updateBook\", \"data\":{...}} — 更新图书\n"
                + "{\"command\":\"findAllBooks\"}             — 查询全部图书\n"
                + "{\"command\":\"findBookById\",\"data\":{\"bookId\":1}} — 按ID查询\n"
                + "{\"command\":\"searchBooks\", \"data\":{\"keyword\":\"Java\"}} — 模糊搜索\n"
                + "{\"command\":\"findByClassId\",\"data\":{\"classId\":1}} — 按分类查询\n"
                + "{\"command\":\"getAllClasses\"}            — 获取全部分类\n"
                + "=========================================\n"
                + "图书字段: bookId, bookName, author, publish, isbn, introduction,\n"
                + "          bookLanguage, price, pubdate, classId, pressmark, state\n"
                + "必填: bookName, author, publish\n"
                + "所有命令请以一行 JSON 发送，换行符为分隔符";
        return ok("帮助信息", help);
    }

    private String cmdPing(JsonObject req) {
        return ok("pong", "服务器运行正常，当前时间: " + java.time.LocalDateTime.now());
    }

    // ———————— 增 ————————

    private String cmdAddBook(JsonObject req) {
        JsonObject data = requireData(req, "bookName", "author", "publish");
        if (data == null) return error("缺少必填字段: bookName, author, publish");

        Book book = jsonToBook(data, null);
        boolean success = bookDao.addBook(book);
        if (success) {
            return ok("添加成功", "图书《" + book.getBookName() + "》已添加");
        } else {
            return error("添加失败，请检查数据是否完整或数据库是否可用");
        }
    }

    // ———————— 删 ————————

    private String cmdDeleteBook(JsonObject req) {
        JsonObject data = requireData(req, "bookId");
        if (data == null) return error("缺少必填字段: bookId");

        long bookId = data.get("bookId").getAsLong();
        Book existing = bookDao.findById(bookId);
        if (existing == null) {
            return error("图书 ID=" + bookId + " 不存在");
        }

        boolean success = bookDao.deleteBook(bookId);
        if (success) {
            return ok("删除成功", "图书《" + existing.getBookName() + "》(ID=" + bookId + ") 已删除");
        } else {
            return error("删除失败，该书可能存在借阅记录未归还");
        }
    }

    // ———————— 改 ————————

    private String cmdUpdateBook(JsonObject req) {
        JsonObject data = requireData(req, "bookId");
        if (data == null) return error("缺少必填字段: bookId");

        long bookId = data.get("bookId").getAsLong();
        Book existing = bookDao.findById(bookId);
        if (existing == null) {
            return error("图书 ID=" + bookId + " 不存在，无法更新");
        }

        Book book = jsonToBook(data, bookId);
        // 空字段保留原值
        mergeNullFields(book, existing);
        boolean success = bookDao.updateBook(book);
        if (success) {
            return ok("更新成功", "图书 ID=" + bookId + " 已更新");
        } else {
            return error("更新失败");
        }
    }

    // ———————— 查 ————————

    private String cmdFindAllBooks(JsonObject req) {
        List<Book> books = bookDao.findAll();
        JsonObject result = new JsonObject();
        result.addProperty("count", books.size());
        result.add("books", GSON.toJsonTree(books));
        return ok("查询到 " + books.size() + " 条记录", result);
    }

    private String cmdFindBookById(JsonObject req) {
        JsonObject data = requireData(req, "bookId");
        if (data == null) return error("缺少必填字段: bookId");

        long bookId = data.get("bookId").getAsLong();
        Book book = bookDao.findById(bookId);
        if (book == null) {
            return error("未找到图书 ID=" + bookId);
        }
        return ok("查询成功", GSON.toJsonTree(book));
    }

    private String cmdSearchBooks(JsonObject req) {
        JsonObject data = requireData(req, "keyword");
        if (data == null) return error("缺少必填字段: keyword");

        String keyword = data.get("keyword").getAsString();
        List<Book> books = bookDao.search(keyword);
        JsonObject result = new JsonObject();
        result.addProperty("keyword", keyword);
        result.addProperty("count", books.size());
        result.add("books", GSON.toJsonTree(books));
        return ok("搜索 '" + keyword + "' 找到 " + books.size() + " 条记录", result);
    }

    private String cmdFindByClassId(JsonObject req) {
        JsonObject data = requireData(req, "classId");
        if (data == null) return error("缺少必填字段: classId");

        int classId = data.get("classId").getAsInt();
        List<Book> books = bookDao.findByClassId(classId);
        JsonObject result = new JsonObject();
        result.addProperty("classId", classId);
        result.addProperty("count", books.size());
        result.add("books", GSON.toJsonTree(books));
        return ok("分类ID=" + classId + " 查询到 " + books.size() + " 条记录", result);
    }

    private String cmdGetAllClasses(JsonObject req) {
        var classes = bookDao.getAllClasses();
        JsonArray arr = new JsonArray();
        for (var c : classes) {
            JsonObject obj = new JsonObject();
            obj.addProperty("classId", c.getClassId());
            obj.addProperty("className", c.getClassName());
            arr.add(obj);
        }
        JsonObject result = new JsonObject();
        result.addProperty("count", classes.size());
        result.add("classes", arr);
        return ok("共 " + classes.size() + " 个分类", result);
    }

    // ========================= 工具方法 =========================

    /**
     * 校验并提取 data 字段，同时校验必填字段存在
     */
    private JsonObject requireData(JsonObject request, String... requiredFields) {
        if (!request.has("data") || request.get("data").isJsonNull()) {
            return null;
        }
        JsonObject data = request.getAsJsonObject("data");
        for (String field : requiredFields) {
            if (!data.has(field) || data.get(field).isJsonNull()) {
                return null;
            }
        }
        return data;
    }

    /**
     * JSON → Book 对象（addBook 时 bookId 为 null，updateBook 时传入 bookId）
     */
    private Book jsonToBook(JsonObject data, Long bookId) {
        Book book = new Book();
        if (bookId != null) book.setBookId(bookId);
        if (data.has("bookName") && !data.get("bookName").isJsonNull())
            book.setBookName(data.get("bookName").getAsString());
        if (data.has("author") && !data.get("author").isJsonNull())
            book.setAuthor(data.get("author").getAsString());
        if (data.has("publish") && !data.get("publish").isJsonNull())
            book.setPublish(data.get("publish").getAsString());
        if (data.has("isbn") && !data.get("isbn").isJsonNull())
            book.setIsbn(data.get("isbn").getAsString());
        if (data.has("introduction") && !data.get("introduction").isJsonNull())
            book.setIntroduction(data.get("introduction").getAsString());
        if (data.has("bookLanguage") && !data.get("bookLanguage").isJsonNull())
            book.setBookLanguage(data.get("bookLanguage").getAsString());
        if (data.has("price") && !data.get("price").isJsonNull()) {
            try {
                book.setPrice(new BigDecimal(data.get("price").getAsString()));
            } catch (NumberFormatException e) {
                book.setPrice(null);
            }
        }
        if (data.has("pubdate") && !data.get("pubdate").isJsonNull())
            book.setPubdate(data.get("pubdate").getAsString());
        if (data.has("classId") && !data.get("classId").isJsonNull())
            book.setClassId(data.get("classId").getAsInt());
        if (data.has("pressmark") && !data.get("pressmark").isJsonNull())
            book.setPressmark(data.get("pressmark").getAsInt());
        if (data.has("state") && !data.get("state").isJsonNull())
            book.setState(data.get("state").getAsInt());
        return book;
    }

    /**
     * 合并：将 existing 中的非空值保留到 book 的空字段中（update 场景）
     */
    private void mergeNullFields(Book book, Book existing) {
        if (book.getBookName() == null) book.setBookName(existing.getBookName());
        if (book.getAuthor() == null) book.setAuthor(existing.getAuthor());
        if (book.getPublish() == null) book.setPublish(existing.getPublish());
        if (book.getIsbn() == null) book.setIsbn(existing.getIsbn());
        if (book.getIntroduction() == null) book.setIntroduction(existing.getIntroduction());
        if (book.getBookLanguage() == null) book.setBookLanguage(existing.getBookLanguage());
        if (book.getPrice() == null) book.setPrice(existing.getPrice());
        if (book.getPubdate() == null) book.setPubdate(existing.getPubdate());
        if (book.getClassId() == null) book.setClassId(existing.getClassId());
        if (book.getPressmark() == null) book.setPressmark(existing.getPressmark());
        if (book.getState() == null) book.setState(existing.getState());
    }

    // ========================= 响应格式化 =========================

    /**
     * 预处理 JSON：修复常见输入问题（中文引号、全角字符等）
     */
    private String preprocessJson(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return raw
                // 中文引号 → 英文引号
                .replace('\u201c', '"')  // "
                .replace('\u201d', '"')  // "
                .replace('\u2018', '\'') // '
                .replace('\u2019', '\'') // '
                // 全角冒号/逗号 → 半角
                .replace('\uff1a', ':')
                .replace('\uff0c', ',')
                // 移除 BOM 头
                .replace("\uFEFF", "")
                .trim();
    }

    /** 成功响应 */
    private String ok(String message, Object data) {
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("message", message);
        if (data instanceof JsonElement) {
            resp.add("data", (JsonElement) data);
        } else if (data instanceof String) {
            resp.addProperty("data", (String) data);
        } else {
            resp.add("data", GSON.toJsonTree(data));
        }
        return GSON.toJson(resp);
    }

    /** 失败响应 */
    private String error(String message) {
        JsonObject resp = new JsonObject();
        resp.addProperty("success", false);
        resp.addProperty("message", message);
        return GSON.toJson(resp);
    }
}
