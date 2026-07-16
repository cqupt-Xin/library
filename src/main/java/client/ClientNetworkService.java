package client;

import com.google.gson.*;
import model.Book;
import model.Borrow;
import model.ClassInfo;
import model.User;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP 客户端网络服务 — 实验九 C/S 模式核心
 * 作为单例，所有 UI 通过本类与后台服务器通信，不直接访问数据库
 */
public class ClientNetworkService {

    private static final ClientNetworkService INSTANCE = new ClientNetworkService();
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private String host = "127.0.0.1";
    private int port = 8888;

    private ClientNetworkService() {}

    public static ClientNetworkService getInstance() {
        return INSTANCE;
    }

    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    // ==================== 底层通信 ====================

    /**
     * 发送 JSON 命令并返回响应
     */
    private JsonObject send(String command, JsonObject data) throws IOException {
        JsonObject req = new JsonObject();
        req.addProperty("command", command);
        if (data != null) req.add("data", data);

        String requestJson = GSON.toJson(req);

        try (Socket socket = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.write(requestJson);
            out.newLine();
            out.flush();

            String responseLine = in.readLine();
            if (responseLine == null || responseLine.trim().isEmpty()) {
                throw new IOException("服务器无响应");
            }
            return GSON.fromJson(responseLine.trim(), JsonObject.class);
        }
    }

    private JsonObject send(String command) throws IOException {
        return send(command, null);
    }

    /**
     * 测试服务器连通性
     */
    public boolean ping() {
        try {
            JsonObject resp = send("ping");
            return resp.has("success") && resp.get("success").getAsBoolean();
        } catch (IOException e) {
            return false;
        }
    }

    // ==================== 用户相关 ====================

    /**
     * 登录
     */
    public User login(String idStr, String password) throws IOException {
        try {
            int id = Integer.parseInt(idStr.trim());
            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            data.addProperty("password", password);

            JsonObject resp = send("login", data);
            if (resp.has("success") && resp.get("success").getAsBoolean()) {
                JsonObject userData = resp.getAsJsonObject("data");
                User user = new User();
                user.setId(userData.get("id").getAsInt());
                user.setUsername(userData.get("username").getAsString());
                user.setRole(userData.get("role").getAsString());
                return user;
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    /**
     * 注册读者
     */
    public int register(String name, String passwd, String sex, String birth,
                        String address, String telcode) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("passwd", passwd);
        data.addProperty("sex", sex);
        data.addProperty("birth", birth);
        data.addProperty("address", address);
        data.addProperty("telcode", telcode);

        JsonObject resp = send("register", data);
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            return resp.getAsJsonObject("data").get("readerId").getAsInt();
        }
        return -1;
    }

    // ==================== 图书相关 ====================

    public List<Book> findAllBooks() throws IOException {
        List<Book> list = new ArrayList<>();
        JsonObject resp = send("findAllBooks");
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            JsonArray arr = resp.getAsJsonObject("data").getAsJsonArray("books");
            for (JsonElement e : arr) {
                list.add(jsonToBook(e.getAsJsonObject()));
            }
        }
        return list;
    }

    public Book findBookById(Long bookId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("bookId", bookId);
        JsonObject resp = send("findBookById", data);
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            return jsonToBook(resp.getAsJsonObject("data"));
        }
        return null;
    }

    public List<Book> searchBooks(String keyword) throws IOException {
        List<Book> list = new ArrayList<>();
        JsonObject data = new JsonObject();
        data.addProperty("keyword", keyword);
        JsonObject resp = send("searchBooks", data);
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            JsonArray arr = resp.getAsJsonObject("data").getAsJsonArray("books");
            for (JsonElement e : arr) {
                list.add(jsonToBook(e.getAsJsonObject()));
            }
        }
        return list;
    }

    public List<Book> findByClassId(Integer classId) throws IOException {
        List<Book> list = new ArrayList<>();
        JsonObject data = new JsonObject();
        data.addProperty("classId", classId);
        JsonObject resp = send("findByClassId", data);
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            JsonArray arr = resp.getAsJsonObject("data").getAsJsonArray("books");
            for (JsonElement e : arr) {
                list.add(jsonToBook(e.getAsJsonObject()));
            }
        }
        return list;
    }

    public boolean addBook(Book book) throws IOException {
        JsonObject resp = send("addBook", bookToJson(book));
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public boolean updateBook(Book book) throws IOException {
        JsonObject resp = send("updateBook", bookToJson(book));
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public boolean deleteBook(Long bookId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("bookId", bookId);
        JsonObject resp = send("deleteBook", data);
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public List<ClassInfo> getAllClasses() throws IOException {
        List<ClassInfo> list = new ArrayList<>();
        JsonObject resp = send("getAllClasses");
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            JsonArray arr = resp.getAsJsonObject("data").getAsJsonArray("classes");
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                list.add(new ClassInfo(o.get("classId").getAsInt(), o.get("className").getAsString()));
            }
        }
        return list;
    }

    // ==================== 借阅相关 ====================

    public boolean borrowBook(long bookId, int readerId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("bookId", bookId);
        data.addProperty("readerId", readerId);
        JsonObject resp = send("borrowBook", data);
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public boolean returnBook(long sernum, long bookId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("sernum", sernum);
        data.addProperty("bookId", bookId);
        JsonObject resp = send("returnBook", data);
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public List<Borrow> findAllBorrows() throws IOException {
        return parseBorrows(send("findAllBorrows"));
    }

    public List<Borrow> findBorrowsByReaderId(int readerId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("readerId", readerId);
        return parseBorrows(send("findBorrowsByReaderId", data));
    }

    public List<Borrow> findActiveBorrows() throws IOException {
        return parseBorrows(send("findActiveBorrows"));
    }

    private List<Borrow> parseBorrows(JsonObject resp) {
        List<Borrow> list = new ArrayList<>();
        if (resp.has("success") && resp.get("success").getAsBoolean()
                && resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonElement data = resp.get("data");
            JsonArray arr = null;
            if (data.isJsonObject() && data.getAsJsonObject().has("borrows")) {
                arr = data.getAsJsonObject().getAsJsonArray("borrows");
            } else if (data.isJsonArray()) {
                arr = data.getAsJsonArray();
            }
            if (arr != null) {
                for (JsonElement e : arr) {
                    list.add(jsonToBorrow(e.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // ==================== 读者管理相关 ====================

    public List<User> getAllReaders() throws IOException {
        List<User> list = new ArrayList<>();
        JsonObject resp = send("getAllReaders");
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            JsonArray arr = resp.getAsJsonObject("data").getAsJsonArray("readers");
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                User u = new User();
                u.setId(o.get("readerId").getAsInt());
                u.setUsername(o.get("name").getAsString());
                u.setStatus(o.get("cardState").getAsInt() == 1 ? "正常" : "已禁用");
                u.setRole("reader");
                list.add(u);
            }
        }
        return list;
    }

    public boolean disableReader(int readerId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("readerId", readerId);
        JsonObject resp = send("disableReader", data);
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    public boolean enableReader(int readerId) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("readerId", readerId);
        JsonObject resp = send("enableReader", data);
        return resp.has("success") && resp.get("success").getAsBoolean();
    }

    // ==================== 导入导出相关 ====================

    /**
     * 导出：服务器返回所有图书 JSON，客户端负责写文件
     */
    public String exportBooksJson() throws IOException {
        JsonObject resp = send("exportBooks");
        if (resp.has("success") && resp.get("success").getAsBoolean()) {
            return GSON.toJson(resp.getAsJsonObject("data"));
        }
        return null;
    }

    /**
     * 导入：客户端读取文件后发送 JSON 到服务器
     */
    public JsonObject importBooks(JsonArray books) throws IOException {
        JsonObject data = new JsonObject();
        data.add("books", books);
        return send("importBooks", data);
    }

    // ==================== 辅助方法 ====================

    private JsonObject bookToJson(Book b) {
        JsonObject o = new JsonObject();
        if (b.getBookId() != null) o.addProperty("bookId", b.getBookId());
        o.addProperty("bookName", b.getBookName());
        o.addProperty("author", b.getAuthor());
        o.addProperty("publish", b.getPublish());
        o.addProperty("isbn", b.getIsbn());
        o.addProperty("introduction", b.getIntroduction());
        o.addProperty("bookLanguage", b.getBookLanguage());
        if (b.getPrice() != null) o.addProperty("price", b.getPrice().toString());
        o.addProperty("pubdate", b.getPubdate());
        if (b.getClassId() != null) o.addProperty("classId", b.getClassId());
        if (b.getPressmark() != null) o.addProperty("pressmark", b.getPressmark());
        if (b.getState() != null) o.addProperty("state", b.getState());
        return o;
    }

    private Book jsonToBook(JsonObject o) {
        Book b = new Book();
        if (has(o, "bookId")) b.setBookId(o.get("bookId").getAsLong());
        if (has(o, "bookName")) b.setBookName(o.get("bookName").getAsString());
        if (has(o, "author")) b.setAuthor(o.get("author").getAsString());
        if (has(o, "publish")) b.setPublish(o.get("publish").getAsString());
        if (has(o, "isbn")) b.setIsbn(o.get("isbn").getAsString());
        if (has(o, "introduction")) b.setIntroduction(o.get("introduction").getAsString());
        if (has(o, "bookLanguage")) b.setBookLanguage(o.get("bookLanguage").getAsString());
        if (has(o, "price") && !o.get("price").isJsonNull()) {
            try { b.setPrice(new BigDecimal(o.get("price").getAsString())); } catch (Exception ignored) {}
        }
        if (has(o, "pubdate")) b.setPubdate(o.get("pubdate").getAsString());
        if (has(o, "classId")) b.setClassId(o.get("classId").getAsInt());
        if (has(o, "className")) b.setClassName(o.get("className").getAsString());
        if (has(o, "pressmark")) b.setPressmark(o.get("pressmark").getAsInt());
        if (has(o, "state")) b.setState(o.get("state").getAsInt());
        return b;
    }

    private Borrow jsonToBorrow(JsonObject o) {
        Borrow b = new Borrow();
        if (has(o, "sernum")) b.setSernum(o.get("sernum").getAsLong());
        if (has(o, "bookId")) b.setBookId(o.get("bookId").getAsLong());
        if (has(o, "readerId")) b.setReaderId(o.get("readerId").getAsInt());
        if (has(o, "lendDate")) b.setLendDate(o.get("lendDate").getAsString());
        if (has(o, "backDate")) b.setBackDate(o.get("backDate").getAsString());
        if (has(o, "bookName")) b.setBookName(o.get("bookName").getAsString());
        if (has(o, "readerName")) b.setReaderName(o.get("readerName").getAsString());
        return b;
    }

    private boolean has(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull();
    }
}
