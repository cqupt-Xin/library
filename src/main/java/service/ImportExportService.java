package service;

import com.google.gson.*;
import dao.BookDao;
import dao.BorrowDao;
import model.Book;
import model.Borrow;
import model.ClassInfo;
import util.DBUtil;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * 数据导入导出服务类
 * 支持图书、借阅记录、读者信息的 JSON 格式导入与导出
 */
public class ImportExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TYPE_BOOKS = "books";
    private static final String TYPE_BORROWS = "borrows";
    private static final String TYPE_READERS = "readers";
    private static final String TYPE_CLASSES = "classes";

    private final BookDao bookDao = new BookDao();
    private final BorrowDao borrowDao = new BorrowDao();

    // ========================= 导出功能 =========================

    /**
     * 导出图书信息到 JSON 文件
     * @param filePath 目标文件路径
     * @param progressCallback 进度回调 (current, total, message)
     * @return 导出的记录数
     */
    public int exportBooks(String filePath, ProgressCallback progressCallback) throws IOException {
        List<Book> books = bookDao.findAll();
        if (progressCallback != null) {
            progressCallback.onStart("正在导出图书信息...", books.size());
        }

        List<JsonObject> data = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            JsonObject obj = new JsonObject();
            obj.addProperty("bookId", b.getBookId());
            obj.addProperty("bookName", b.getBookName());
            obj.addProperty("author", b.getAuthor());
            obj.addProperty("publish", b.getPublish());
            obj.addProperty("isbn", b.getIsbn());
            obj.addProperty("introduction", b.getIntroduction());
            obj.addProperty("bookLanguage", b.getBookLanguage());
            obj.addProperty("price", b.getPrice() != null ? b.getPrice().toString() : null);
            obj.addProperty("pubdate", b.getPubdate());
            obj.addProperty("classId", b.getClassId());
            obj.addProperty("className", b.getClassName());
            obj.addProperty("pressmark", b.getPressmark());
            obj.addProperty("state", b.getState());
            data.add(obj);

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, books.size());
            }
        }

        writeJsonFile(filePath, TYPE_BOOKS, data);
        if (progressCallback != null) {
            progressCallback.onComplete("图书信息导出完成，共 " + data.size() + " 条记录");
        }
        return data.size();
    }

    /**
     * 导出借阅记录到 JSON 文件
     */
    public int exportBorrows(String filePath, ProgressCallback progressCallback) throws IOException {
        List<Borrow> borrows = borrowDao.findAll();
        if (progressCallback != null) {
            progressCallback.onStart("正在导出借阅记录...", borrows.size());
        }

        List<JsonObject> data = new ArrayList<>();
        for (int i = 0; i < borrows.size(); i++) {
            Borrow br = borrows.get(i);
            JsonObject obj = new JsonObject();
            obj.addProperty("sernum", br.getSernum());
            obj.addProperty("bookId", br.getBookId());
            obj.addProperty("readerId", br.getReaderId());
            obj.addProperty("lendDate", br.getLendDate());
            obj.addProperty("backDate", br.getBackDate());
            data.add(obj);

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, borrows.size());
            }
        }

        writeJsonFile(filePath, TYPE_BORROWS, data);
        if (progressCallback != null) {
            progressCallback.onComplete("借阅记录导出完成，共 " + data.size() + " 条记录");
        }
        return data.size();
    }

    /**
     * 导出读者信息到 JSON 文件（包含 reader_info 和 reader_card 完整信息）
     */
    public int exportReaders(String filePath, ProgressCallback progressCallback) throws IOException {
        String sql = "SELECT ri.reader_id, ri.name, ri.sex, ri.birth, ri.address, ri.telcode, " +
                     "rc.passwd, rc.card_state " +
                     "FROM reader_info ri LEFT JOIN reader_card rc ON ri.reader_id = rc.reader_id " +
                     "ORDER BY ri.reader_id";
        List<JsonObject> data = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("readerId", rs.getInt("reader_id"));
                obj.addProperty("name", rs.getString("name"));
                obj.addProperty("sex", rs.getString("sex"));
                obj.addProperty("birth", rs.getString("birth"));
                obj.addProperty("address", rs.getString("address"));
                obj.addProperty("telcode", rs.getString("telcode"));
                obj.addProperty("passwd", rs.getString("passwd"));
                obj.addProperty("cardState", rs.getInt("card_state"));
                data.add(obj);
            }
        } catch (SQLException e) {
            throw new IOException("导出读者信息时数据库错误: " + e.getMessage(), e);
        }

        if (progressCallback != null) {
            progressCallback.onStart("正在导出读者信息...", data.size());
        }

        writeJsonFile(filePath, TYPE_READERS, data);
        if (progressCallback != null) {
            progressCallback.onComplete("读者信息导出完成，共 " + data.size() + " 条记录");
        }
        return data.size();
    }

    /**
     * 导出图书分类到 JSON 文件
     */
    public int exportClasses(String filePath, ProgressCallback progressCallback) throws IOException {
        List<ClassInfo> classes = bookDao.getAllClasses();
        if (progressCallback != null) {
            progressCallback.onStart("正在导出图书分类...", classes.size());
        }

        List<JsonObject> data = new ArrayList<>();
        for (int i = 0; i < classes.size(); i++) {
            ClassInfo c = classes.get(i);
            JsonObject obj = new JsonObject();
            obj.addProperty("classId", c.getClassId());
            obj.addProperty("className", c.getClassName());
            data.add(obj);

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, classes.size());
            }
        }

        writeJsonFile(filePath, TYPE_CLASSES, data);
        if (progressCallback != null) {
            progressCallback.onComplete("图书分类导出完成，共 " + data.size() + " 条记录");
        }
        return data.size();
    }

    private void writeJsonFile(String filePath, String type, List<JsonObject> data) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        root.addProperty("exportTime", LocalDateTime.now().format(DATE_FMT));
        root.addProperty("recordCount", data.size());
        JsonArray arr = new JsonArray();
        for (JsonObject obj : data) {
            arr.add(obj);
        }
        root.add("data", arr);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    // ========================= 导入功能 =========================

    /**
     * 导入结果类
     */
    public static class ImportResult {
        private final int totalCount;
        private int successCount;
        private int skipCount;
        private int updateCount;
        private int failCount;
        private final List<String> errors = new ArrayList<>();
        private final String type;

        public ImportResult(String type, int totalCount) {
            this.type = type;
            this.totalCount = totalCount;
        }

        public void addSuccess() { successCount++; }
        public void addSkip(String reason) { skipCount++; errors.add("[跳过] " + reason); }
        public void addUpdate() { updateCount++; successCount++; }
        public void addFail(String reason) { failCount++; errors.add("[失败] " + reason); }

        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getSkipCount() { return skipCount; }
        public int getUpdateCount() { return updateCount; }
        public int getFailCount() { return failCount; }
        public List<String> getErrors() { return errors; }
        public String getType() { return type; }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("====== 导入结果 ======\n");
            sb.append("数据类型: ").append(getTypeLabel()).append("\n");
            sb.append("总计: ").append(totalCount).append(" 条\n");
            sb.append("成功新增: ").append(successCount - updateCount).append(" 条\n");
            sb.append("成功更新: ").append(updateCount).append(" 条\n");
            sb.append("跳过(重复): ").append(skipCount).append(" 条\n");
            sb.append("失败: ").append(failCount).append(" 条\n");
            if (!errors.isEmpty()) {
                sb.append("------ 详细信息 ------\n");
                int maxShow = Math.min(errors.size(), 20);
                for (int i = 0; i < maxShow; i++) {
                    sb.append(errors.get(i)).append("\n");
                }
                if (errors.size() > 20) {
                    sb.append("... 共 ").append(errors.size()).append(" 条详情，仅显示前20条\n");
                }
            }
            return sb.toString();
        }

        private String getTypeLabel() {
            switch (type) {
                case TYPE_BOOKS: return "图书信息";
                case TYPE_BORROWS: return "借阅记录";
                case TYPE_READERS: return "读者信息";
                case TYPE_CLASSES: return "图书分类";
                default: return type;
            }
        }
    }

    /**
     * 导入图书信息
     * @param filePath 源文件路径
     * @param duplicateStrategy 重复数据处理策略: "skip"=跳过, "update"=更新
     * @param progressCallback 进度回调
     * @return 导入结果
     */
    public ImportResult importBooks(String filePath, String duplicateStrategy,
                                     ProgressCallback progressCallback) throws IOException {
        JsonArray data = readJsonData(filePath, TYPE_BOOKS);
        ImportResult result = new ImportResult(TYPE_BOOKS, data.size());

        if (progressCallback != null) {
            progressCallback.onStart("正在导入图书信息...", data.size());
        }

        for (int i = 0; i < data.size(); i++) {
            try {
                JsonObject obj = data.get(i).getAsJsonObject();
                // 数据格式校验
                validateBookRecord(obj, i + 1);

                Book book = jsonToBook(obj);
                Book existing = bookDao.findById(book.getBookId());

                if (existing != null) {
                    if ("update".equals(duplicateStrategy)) {
                        if (bookDao.updateBook(book)) {
                            result.addUpdate();
                        } else {
                            result.addFail("第" + (i + 1) + "条: 图书ID=" + book.getBookId() + " 更新失败");
                        }
                    } else {
                        result.addSkip("第" + (i + 1) + "条: 图书ID=" + book.getBookId() + " 已存在，跳过");
                    }
                } else {
                    if (bookDao.addBook(book)) {
                        result.addSuccess();
                    } else {
                        result.addFail("第" + (i + 1) + "条: 图书 '" + book.getBookName() + "' 插入失败");
                    }
                }
            } catch (Exception e) {
                result.addFail("第" + (i + 1) + "条: " + e.getMessage());
            }

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, data.size());
            }
        }

        if (progressCallback != null) {
            progressCallback.onComplete(result.getSummary());
        }
        return result;
    }

    /**
     * 导入借阅记录
     */
    public ImportResult importBorrows(String filePath, String duplicateStrategy,
                                       ProgressCallback progressCallback) throws IOException {
        JsonArray data = readJsonData(filePath, TYPE_BORROWS);
        ImportResult result = new ImportResult(TYPE_BORROWS, data.size());

        if (progressCallback != null) {
            progressCallback.onStart("正在导入借阅记录...", data.size());
        }

        for (int i = 0; i < data.size(); i++) {
            try {
                JsonObject obj = data.get(i).getAsJsonObject();
                validateBorrowRecord(obj, i + 1);

                long sernum = obj.get("sernum").getAsLong();
                long bookId = obj.get("bookId").getAsLong();
                int readerId = obj.get("readerId").getAsInt();
                String lendDate = getJsonStringOrNull(obj, "lendDate");
                String backDate = getJsonStringOrNull(obj, "backDate");

                // 检查是否已存在
                if (borrowSernumExists(sernum)) {
                    if ("update".equals(duplicateStrategy)) {
                        if (updateBorrowRecord(sernum, bookId, readerId, lendDate, backDate)) {
                            result.addUpdate();
                        } else {
                            result.addFail("第" + (i + 1) + "条: 记录号=" + sernum + " 更新失败");
                        }
                    } else {
                        result.addSkip("第" + (i + 1) + "条: 记录号=" + sernum + " 已存在，跳过");
                    }
                } else {
                    if (insertBorrowRecord(sernum, bookId, readerId, lendDate, backDate)) {
                        result.addSuccess();
                    } else {
                        result.addFail("第" + (i + 1) + "条: 记录号=" + sernum + " 插入失败");
                    }
                }
            } catch (Exception e) {
                result.addFail("第" + (i + 1) + "条: " + e.getMessage());
            }

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, data.size());
            }
        }

        if (progressCallback != null) {
            progressCallback.onComplete(result.getSummary());
        }
        return result;
    }

    /**
     * 导入读者信息
     */
    public ImportResult importReaders(String filePath, String duplicateStrategy,
                                       ProgressCallback progressCallback) throws IOException {
        JsonArray data = readJsonData(filePath, TYPE_READERS);
        ImportResult result = new ImportResult(TYPE_READERS, data.size());

        if (progressCallback != null) {
            progressCallback.onStart("正在导入读者信息...", data.size());
        }

        for (int i = 0; i < data.size(); i++) {
            try {
                JsonObject obj = data.get(i).getAsJsonObject();
                validateReaderRecord(obj, i + 1);

                int readerId = obj.get("readerId").getAsInt();
                String name = obj.get("name").getAsString();
                String sex = getJsonStringOrNull(obj, "sex");
                String birth = getJsonStringOrNull(obj, "birth");
                String address = getJsonStringOrNull(obj, "address");
                String telcode = getJsonStringOrNull(obj, "telcode");
                String passwd = obj.get("passwd").getAsString();
                int cardState = obj.get("cardState").getAsInt();

                boolean exists = readerExists(readerId);
                if (exists) {
                    if ("update".equals(duplicateStrategy)) {
                        if (updateReader(readerId, name, sex, birth, address, telcode, passwd, cardState)) {
                            result.addUpdate();
                        } else {
                            result.addFail("第" + (i + 1) + "条: 读者ID=" + readerId + " 更新失败");
                        }
                    } else {
                        result.addSkip("第" + (i + 1) + "条: 读者ID=" + readerId + "(" + name + ") 已存在，跳过");
                    }
                } else {
                    if (insertReader(readerId, name, sex, birth, address, telcode, passwd, cardState)) {
                        result.addSuccess();
                    } else {
                        result.addFail("第" + (i + 1) + "条: 读者 '" + name + "' 插入失败");
                    }
                }
            } catch (Exception e) {
                result.addFail("第" + (i + 1) + "条: " + e.getMessage());
            }

            if (progressCallback != null) {
                progressCallback.onProgress(i + 1, data.size());
            }
        }

        if (progressCallback != null) {
            progressCallback.onComplete(result.getSummary());
        }
        return result;
    }

    /**
     * 统一导入入口：根据文件中的 type 字段自动分发到对应的导入方法
     * @param filePath 源文件路径
     * @param duplicateStrategy 重复数据处理策略: "skip"=跳过, "update"=更新
     * @param progressCallback 进度回调
     * @return 导入结果
     */
    public ImportResult importData(String filePath, String duplicateStrategy,
                                    ProgressCallback progressCallback) throws IOException {
        // 先读取文件判断类型
        String type;
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("type")) {
                throw new IOException("文件格式错误: 缺少 type 字段，无法确定数据类型");
            }
            type = root.get("type").getAsString();
        }

        switch (type) {
            case TYPE_BOOKS: return importBooks(filePath, duplicateStrategy, progressCallback);
            case TYPE_BORROWS: return importBorrows(filePath, duplicateStrategy, progressCallback);
            case TYPE_READERS: return importReaders(filePath, duplicateStrategy, progressCallback);
            default:
                throw new IOException("不支持的数据类型: '" + type +
                        "'，支持的类型: books, borrows, readers");
        }
    }

    // ========================= 数据格式校验 =========================

    private void validateBookRecord(JsonObject obj, int row) throws IllegalArgumentException {
        requireField(obj, "bookId", row);
        requireField(obj, "bookName", row);
        requireField(obj, "author", row);
        requireField(obj, "publish", row);

        // 校验 bookId 为数字
        try { obj.get("bookId").getAsLong(); } catch (Exception e) {
            throw new IllegalArgumentException("图书ID必须为数字");
        }
        // 校验 price 如果存在
        if (obj.has("price") && !obj.get("price").isJsonNull()) {
            try { new BigDecimal(obj.get("price").getAsString()); } catch (Exception e) {
                throw new IllegalArgumentException("价格格式不正确: " + obj.get("price").getAsString());
            }
        }
        // 校验 state 如果存在
        if (obj.has("state") && !obj.get("state").isJsonNull()) {
            int state = obj.get("state").getAsInt();
            if (state != 0 && state != 1) {
                throw new IllegalArgumentException("图书状态只能为0(在馆)或1(已借出)");
            }
        }
    }

    private void validateBorrowRecord(JsonObject obj, int row) throws IllegalArgumentException {
        requireField(obj, "sernum", row);
        requireField(obj, "bookId", row);
        requireField(obj, "readerId", row);

        try { obj.get("sernum").getAsLong(); } catch (Exception e) {
            throw new IllegalArgumentException("记录号必须为数字");
        }
        try { obj.get("bookId").getAsLong(); } catch (Exception e) {
            throw new IllegalArgumentException("图书ID必须为数字");
        }
        try { obj.get("readerId").getAsInt(); } catch (Exception e) {
            throw new IllegalArgumentException("读者ID必须为数字");
        }
    }

    private void validateReaderRecord(JsonObject obj, int row) throws IllegalArgumentException {
        requireField(obj, "readerId", row);
        requireField(obj, "name", row);
        requireField(obj, "passwd", row);
        requireField(obj, "cardState", row);

        try { obj.get("readerId").getAsInt(); } catch (Exception e) {
            throw new IllegalArgumentException("读者ID必须为数字");
        }
        int cardState = obj.get("cardState").getAsInt();
        if (cardState != 0 && cardState != 1) {
            throw new IllegalArgumentException("借阅卡状态只能为0(禁用)或1(正常)");
        }
    }

    private void requireField(JsonObject obj, String fieldName, int row) {
        if (!obj.has(fieldName) || obj.get(fieldName).isJsonNull()) {
            throw new IllegalArgumentException("缺少必填字段: " + fieldName);
        }
    }

    // ========================= 文件读取与解析 =========================

    private JsonArray readJsonData(String filePath, String expectedType) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                throw new IOException("文件内容为空或格式不正确");
            }

            // 类型校验
            if (root.has("type")) {
                String type = root.get("type").getAsString();
                if (!expectedType.equals(type)) {
                    throw new IOException("文件类型不匹配: 期望 '" + getTypeLabel(expectedType) +
                                         "', 实际 '" + getTypeLabel(type) + "'");
                }
            }

            if (!root.has("data") || root.get("data").isJsonNull()) {
                throw new IOException("文件中未找到数据(data)字段");
            }

            JsonArray data = root.getAsJsonArray("data");
            if (data == null || data.size() == 0) {
                throw new IOException("文件中没有可导入的数据记录");
            }
            return data;

        } catch (JsonSyntaxException e) {
            throw new IOException("文件格式错误，无法解析JSON: " + e.getMessage(), e);
        }
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case TYPE_BOOKS: return "图书信息";
            case TYPE_BORROWS: return "借阅记录";
            case TYPE_READERS: return "读者信息";
            case TYPE_CLASSES: return "图书分类";
            default: return type;
        }
    }

    // ========================= 辅助方法 =========================

    private String getJsonStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private Book jsonToBook(JsonObject obj) {
        Book book = new Book();
        book.setBookId(obj.get("bookId").getAsLong());
        book.setBookName(obj.get("bookName").getAsString());
        book.setAuthor(obj.get("author").getAsString());
        book.setPublish(obj.get("publish").getAsString());
        book.setIsbn(getJsonStringOrNull(obj, "isbn"));
        book.setIntroduction(getJsonStringOrNull(obj, "introduction"));
        book.setBookLanguage(getJsonStringOrNull(obj, "bookLanguage"));
        String priceStr = getJsonStringOrNull(obj, "price");
        if (priceStr != null && !priceStr.isEmpty()) {
            book.setPrice(new BigDecimal(priceStr));
        }
        book.setPubdate(getJsonStringOrNull(obj, "pubdate"));
        if (obj.has("classId") && !obj.get("classId").isJsonNull()) {
            book.setClassId(obj.get("classId").getAsInt());
        }
        if (obj.has("pressmark") && !obj.get("pressmark").isJsonNull()) {
            book.setPressmark(obj.get("pressmark").getAsInt());
        }
        if (obj.has("state") && !obj.get("state").isJsonNull()) {
            book.setState(obj.get("state").getAsInt());
        }
        return book;
    }

    // ========================= 数据库直接操作 =========================

    private boolean borrowSernumExists(long sernum) {
        String sql = "SELECT COUNT(*) FROM lend_list WHERE sernum = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sernum);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean insertBorrowRecord(long sernum, long bookId, int readerId,
                                        String lendDate, String backDate) {
        String sql = "INSERT INTO lend_list (sernum, book_id, reader_id, lend_date, back_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sernum);
            ps.setLong(2, bookId);
            ps.setInt(3, readerId);
            ps.setString(4, lendDate);
            ps.setString(5, backDate);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateBorrowRecord(long sernum, long bookId, int readerId,
                                        String lendDate, String backDate) {
        String sql = "UPDATE lend_list SET book_id = ?, reader_id = ?, lend_date = ?, back_date = ? WHERE sernum = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setInt(2, readerId);
            ps.setString(3, lendDate);
            ps.setString(4, backDate);
            ps.setLong(5, sernum);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean readerExists(int readerId) {
        String sql = "SELECT COUNT(*) FROM reader_info WHERE reader_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean insertReader(int readerId, String name, String sex, String birth,
                                  String address, String telcode, String passwd, int cardState) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 插入 reader_info
            String sqlInfo = "INSERT INTO reader_info (reader_id, name, sex, birth, address, telcode) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setInt(1, readerId);
                ps.setString(2, name);
                ps.setString(3, sex);
                ps.setString(4, birth);
                ps.setString(5, address);
                ps.setString(6, telcode);
                ps.executeUpdate();
            }

            // 插入 reader_card
            String sqlCard = "INSERT INTO reader_card (reader_id, name, passwd, card_state) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCard)) {
                ps.setInt(1, readerId);
                ps.setString(2, name);
                ps.setString(3, passwd);
                ps.setInt(4, cardState);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private boolean updateReader(int readerId, String name, String sex, String birth,
                                  String address, String telcode, String passwd, int cardState) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 检查 reader_info 是否存在
            String checkSql = "SELECT COUNT(*) FROM reader_info WHERE reader_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, readerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // 更新 reader_info
                        String sqlInfo = "UPDATE reader_info SET name = ?, sex = ?, birth = ?, address = ?, telcode = ? WHERE reader_id = ?";
                        try (PreparedStatement psInfo = conn.prepareStatement(sqlInfo)) {
                            psInfo.setString(1, name);
                            psInfo.setString(2, sex);
                            psInfo.setString(3, birth);
                            psInfo.setString(4, address);
                            psInfo.setString(5, telcode);
                            psInfo.setInt(6, readerId);
                            psInfo.executeUpdate();
                        }
                    } else {
                        // 不存在则插入
                        String sqlInfo = "INSERT INTO reader_info (reader_id, name, sex, birth, address, telcode) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement psInfo = conn.prepareStatement(sqlInfo)) {
                            psInfo.setInt(1, readerId);
                            psInfo.setString(2, name);
                            psInfo.setString(3, sex);
                            psInfo.setString(4, birth);
                            psInfo.setString(5, address);
                            psInfo.setString(6, telcode);
                            psInfo.executeUpdate();
                        }
                    }
                }
            }

            // 检查 reader_card 是否存在
            String checkCardSql = "SELECT COUNT(*) FROM reader_card WHERE reader_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkCardSql)) {
                ps.setInt(1, readerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // 更新 reader_card
                        String sqlCard = "UPDATE reader_card SET name = ?, passwd = ?, card_state = ? WHERE reader_id = ?";
                        try (PreparedStatement psCard = conn.prepareStatement(sqlCard)) {
                            psCard.setString(1, name);
                            psCard.setString(2, passwd);
                            psCard.setInt(3, cardState);
                            psCard.setInt(4, readerId);
                            psCard.executeUpdate();
                        }
                    } else {
                        // 不存在则插入
                        String sqlCard = "INSERT INTO reader_card (reader_id, name, passwd, card_state) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement psCard = conn.prepareStatement(sqlCard)) {
                            psCard.setInt(1, readerId);
                            psCard.setString(2, name);
                            psCard.setString(3, passwd);
                            psCard.setInt(4, cardState);
                            psCard.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ========================= 进度回调接口 =========================

    @FunctionalInterface
    public interface ProgressCallback {
        void onStart(String message, int total);
        default void onProgress(int current, int total) {}
        default void onComplete(String message) {}
    }
}
