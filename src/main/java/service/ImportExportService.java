package service;

import com.google.gson.*;
import dao.BookDao;
import model.Book;
import util.DBUtil;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 图书信息导入导出服务 —— 仅覆盖 UI 层实际调用的图书导入/导出
 */
public class ImportExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TYPE = "books";

    private final BookDao bookDao = new BookDao();

    // ==================== 导出 ====================

    public int exportBooks(String filePath, ProgressCallback cb) throws IOException {
        List<Book> books = bookDao.findAll();
        fireStart(cb, "正在导出图书信息...", books.size());

        List<JsonObject> data = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            data.add(bookToJson(books.get(i)));
            fireProgress(cb, i + 1, books.size());
        }

        writeFile(filePath, data);
        fireComplete(cb, "图书信息导出完成，共 " + data.size() + " 条记录");
        return data.size();
    }

    // ==================== 导入 ====================

    public ImportResult importBooks(String filePath, ProgressCallback cb) throws IOException {
        JsonArray arr = readData(filePath);
        ImportResult r = new ImportResult(arr.size());
        fireStart(cb, "正在导入图书信息...", arr.size());

        for (int i = 0; i < arr.size(); i++) {
            try {
                JsonObject obj = arr.get(i).getAsJsonObject();
                validate(obj);
                Book book = jsonToBook(obj);

                if (bookDao.findById(book.getBookId()) != null) {
                    if (bookDao.updateBook(book)) r.addUpdate();
                    else r.addFail("第" + (i + 1) + "条: 图书ID=" + book.getBookId() + " 更新失败");
                } else {
                    insertBook(book);
                    r.addNew();
                }
            } catch (Exception e) {
                r.addFail("第" + (i + 1) + "条: " + e.getMessage());
            }
            fireProgress(cb, i + 1, arr.size());
        }

        fireComplete(cb, r.getSummary());
        return r;
    }

    // ==================== 进度回调工具 ====================

    private static void fireStart(ProgressCallback cb, String msg, int total) {
        if (cb != null) cb.onStart(msg, total);
    }
    private static void fireProgress(ProgressCallback cb, int cur, int total) {
        if (cb != null) cb.onProgress(cur, total);
    }
    private static void fireComplete(ProgressCallback cb, String msg) {
        if (cb != null) cb.onComplete(msg);
    }

    // ==================== 文件读写 ====================

    private void writeFile(String filePath, List<JsonObject> data) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", TYPE);
        root.addProperty("exportTime", LocalDateTime.now().format(DATE_FMT));
        root.addProperty("recordCount", data.size());
        JsonArray arr = new JsonArray();
        for (JsonObject o : data) arr.add(o);
        root.add("data", arr);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        }
    }

    private JsonArray readData(String filePath) throws IOException {
        try (Reader r = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null || !root.has("data") || root.get("data").isJsonNull())
                throw new IOException("文件内容为空或缺少 data 字段");
            JsonArray data = root.getAsJsonArray("data");
            if (data == null || data.size() == 0)
                throw new IOException("文件中没有可导入的数据记录");
            return data;
        } catch (JsonSyntaxException e) {
            throw new IOException("JSON 格式错误: " + e.getMessage(), e);
        }
    }

    // ==================== 校验 ====================

    private void validate(JsonObject obj) {
        require(obj, "bookId"); require(obj, "bookName"); require(obj, "author"); require(obj, "publish");
        try { obj.get("bookId").getAsLong(); } catch (Exception e) { throw new IllegalArgumentException("图书ID必须为数字"); }
        if (obj.has("price") && !obj.get("price").isJsonNull()) {
            try { new BigDecimal(obj.get("price").getAsString()); } catch (Exception e) { throw new IllegalArgumentException("价格格式不正确"); }
        }
        if (obj.has("state") && !obj.get("state").isJsonNull()) {
            int s = obj.get("state").getAsInt();
            if (s != 0 && s != 1) throw new IllegalArgumentException("图书状态只能为0(在馆)或1(已借出)");
        }
    }

    private void require(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull())
            throw new IllegalArgumentException("缺少必填字段: " + field);
    }

    // ==================== JSON ↔ 对象转换 ====================

    private JsonObject bookToJson(Book b) {
        JsonObject o = new JsonObject();
        o.addProperty("bookId",     b.getBookId());
        o.addProperty("bookName",   b.getBookName());
        o.addProperty("author",     b.getAuthor());
        o.addProperty("publish",    b.getPublish());
        o.addProperty("isbn",       b.getIsbn());
        o.addProperty("introduction", b.getIntroduction());
        o.addProperty("bookLanguage", b.getBookLanguage());
        o.addProperty("price",      b.getPrice() != null ? b.getPrice().toString() : null);
        o.addProperty("pubdate",    b.getPubdate());
        o.addProperty("classId",    b.getClassId());
        o.addProperty("className",  b.getClassName());
        o.addProperty("pressmark",  b.getPressmark());
        o.addProperty("state",      b.getState());
        return o;
    }

    private Book jsonToBook(JsonObject obj) {
        Book b = new Book();
        b.setBookId(obj.get("bookId").getAsLong());
        b.setBookName(obj.get("bookName").getAsString());
        b.setAuthor(obj.get("author").getAsString());
        b.setPublish(obj.get("publish").getAsString());
        b.setIsbn(strOrNull(obj, "isbn"));
        b.setIntroduction(strOrNull(obj, "introduction"));
        b.setBookLanguage(strOrNull(obj, "bookLanguage"));
        if (obj.has("price") && !obj.get("price").isJsonNull()) {
            String s = obj.get("price").getAsString();
            if (!s.isEmpty()) b.setPrice(new BigDecimal(s));
        }
        b.setPubdate(strOrNull(obj, "pubdate"));
        if (obj.has("classId") && !obj.get("classId").isJsonNull()) b.setClassId(obj.get("classId").getAsInt());
        if (obj.has("pressmark") && !obj.get("pressmark").isJsonNull()) b.setPressmark(obj.get("pressmark").getAsInt());
        if (obj.has("state") && !obj.get("state").isJsonNull()) b.setState(obj.get("state").getAsInt());
        return b;
    }

    private String strOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    // ==================== 数据库直接写入 ====================

    private void insertBook(Book b) throws SQLException {
        String sql = "INSERT INTO book_info (book_id,book_name,author,publish,ISBN,introduction,"
                + "book_language,price,pubdate,class_id,pressmark,state) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, b.getBookId());
            ps.setString(2, b.getBookName());    ps.setString(3, b.getAuthor());
            ps.setString(4, b.getPublish());     ps.setString(5, b.getIsbn());
            ps.setString(6, b.getIntroduction()); ps.setString(7, b.getBookLanguage());
            ps.setBigDecimal(8, b.getPrice());    ps.setString(9, b.getPubdate());
            setIntOrNull(ps, 10, b.getClassId()); setIntOrNull(ps, 11, b.getPressmark());
            setIntOrNull(ps, 12, b.getState());
            ps.executeUpdate();
        }
    }

    private void setIntOrNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v != null) ps.setInt(idx, v); else ps.setNull(idx, Types.INTEGER);
    }

    // ==================== 内部类 ====================

    public static class ImportResult {
        private final int total;
        private int success, skip, update, fail;
        private final List<String> errors = new ArrayList<>();

        ImportResult(int total) { this.total = total; }

        void addNew()    { success++; }
        void addUpdate() { update++; success++; }
        void addSkip(String r) { skip++;  errors.add("[跳过] " + r); }
        void addFail(String r) { fail++;  errors.add("[失败] " + r); }

        public int getSuccessCount()  { return success; }
        public int getUpdateCount()   { return update; }
        public int getFailCount()     { return fail; }

        public String getSummary() {
            StringBuilder sb = new StringBuilder("====== 导入结果 ======\n");
            sb.append("总计: ").append(total).append(" 条\n");
            sb.append("新增: ").append(success - update).append(" 条\n");
            sb.append("更新: ").append(update).append(" 条\n");
            sb.append("跳过: ").append(skip).append(" 条\n");
            sb.append("失败: ").append(fail).append(" 条\n");
            if (!errors.isEmpty()) {
                sb.append("------ 详情 ------\n");
                int max = Math.min(errors.size(), 20);
                for (int i = 0; i < max; i++) sb.append(errors.get(i)).append("\n");
                if (errors.size() > 20) sb.append("... 共 ").append(errors.size()).append(" 条\n");
            }
            return sb.toString();
        }
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onStart(String message, int total);
        default void onProgress(int current, int total) {}
        default void onComplete(String message) {}
    }
}
