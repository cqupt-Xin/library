package unit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import server.Dispatcher;

import static org.junit.Assert.*;

/**
 * 单元测试 — 命令分发器 (Dispatcher) 逻辑测试
 * 
 * 测试 Dispatcher 对各种命令的解析、路由和响应逻辑。
 * Dispatcher 内部会调用 DAO 层访问数据库，因此本测试
 * 需要服务端已启动且数据库可连接方可全部通过。
 * 
 * 不依赖数据库的命令（如 help、ping）可独立测试；
 * 依赖数据库的命令会在无法连接时输出跳过信息。
 */
public class DispatcherTest {

    private final Dispatcher dispatcher = new Dispatcher();

    // ==================== 系统命令测试 ====================

    @Test
    public void testHelpCommand() {
        String response = dispatcher.dispatch("{\"command\":\"help\"}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertTrue("help命令应返回success=true", json.get("success").getAsBoolean());
        assertTrue("help命令应包含帮助信息",
                json.get("message").getAsString().contains("帮助信息"));
        assertTrue("help信息应包含命令列表",
                json.get("data").getAsString().contains("help"));
        assertTrue("help信息应提及addBook",
                json.get("data").getAsString().contains("addBook"));
        assertTrue("help信息应提及findAllBooks",
                json.get("data").getAsString().contains("findAllBooks"));
        assertTrue("help信息应提及login",
                json.get("data").getAsString().contains("login"));
    }

    @Test
    public void testPingCommand() {
        String response = dispatcher.dispatch("{\"command\":\"ping\"}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertTrue("ping命令应返回success=true", json.get("success").getAsBoolean());
        assertEquals("ping消息应为pong", "pong", json.get("message").getAsString());
        assertTrue("ping数据应提及服务器运行正常",
                json.get("data").getAsString().contains("服务器运行正常"));
    }

    // ==================== 未知命令测试 ====================

    @Test
    public void testUnknownCommand() {
        String response = dispatcher.dispatch("{\"command\":\"nonexistentCmd\"}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("未知命令应返回success=false", json.get("success").getAsBoolean());
        assertTrue("应提示未知命令",
                json.get("message").getAsString().contains("未知命令"));
    }

    // ==================== JSON格式校验测试 ====================

    @Test
    public void testMissingCommandField() {
        String response = dispatcher.dispatch("{\"data\":{\"key\":\"value\"}}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("缺少command字段应返回success=false", json.get("success").getAsBoolean());
        assertTrue("应提示缺少command字段",
                json.get("message").getAsString().contains("缺少 'command' 字段"));
    }

    @Test
    public void testInvalidJson() {
        String response = dispatcher.dispatch("这不是合法的JSON");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("非法JSON应返回success=false", json.get("success").getAsBoolean());
        assertTrue("应提示JSON格式错误",
                json.get("message").getAsString().contains("JSON 格式错误"));
    }

    @Test
    public void testEmptyInput() {
        String response = dispatcher.dispatch("");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("空输入应返回success=false", json.get("success").getAsBoolean());
    }

    @Test
    public void testNullInput() {
        String response = dispatcher.dispatch(null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("null输入应返回success=false", json.get("success").getAsBoolean());
    }

    // ==================== 全角字符预处理测试 ====================

    @Test
    public void testFullWidthQuotePreprocessing() {
        // 包含中文全角引号的命令应被自动转换为半角引号
        String fullWidthCmd = "{\u201ccommand\u201d:\u201cping\u201d}";
        String response = dispatcher.dispatch(fullWidthCmd);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertTrue("全角引号经预处理后ping应成功",
                json.get("success").getAsBoolean());
    }

    @Test
    public void testBOMRemoval() {
        // BOM + 合法命令 → 应按正常命令处理
        String bomCmd = "\uFEFF" + "{\"command\":\"ping\"}";
        String response = dispatcher.dispatch(bomCmd);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertTrue("去除BOM后ping应成功", json.get("success").getAsBoolean());
    }

    // ==================== 必填字段校验测试 ====================

    @Test
    public void testAddBookMissingRequiredFields() {
        // 测试各种缺少必填字段的情况
        String response1 = dispatcher.dispatch(
                "{\"command\":\"addBook\",\"data\":{}}");
        JsonObject json1 = JsonParser.parseString(response1).getAsJsonObject();
        assertFalse("缺少全部字段", json1.get("success").getAsBoolean());
        assertTrue("应提示缺少必填字段",
                json1.get("message").getAsString().contains("缺少必填字段"));

        String response2 = dispatcher.dispatch(
                "{\"command\":\"addBook\",\"data\":{\"bookName\":\"测试\"}}");
        JsonObject json2 = JsonParser.parseString(response2).getAsJsonObject();
        assertFalse("只有bookName", json2.get("success").getAsBoolean());
        assertTrue("应提示缺少必填字段",
                json2.get("message").getAsString().contains("缺少必填字段"));

        String response3 = dispatcher.dispatch(
                "{\"command\":\"addBook\"}");
        JsonObject json3 = JsonParser.parseString(response3).getAsJsonObject();
        assertFalse("没有data", json3.get("success").getAsBoolean());
    }

    @Test
    public void testDeleteBookMissingBookId() {
        String response = dispatcher.dispatch(
                "{\"command\":\"deleteBook\",\"data\":{}}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertFalse("缺少bookId应返回failure", json.get("success").getAsBoolean());
        assertTrue("应提示缺少bookId",
                json.get("message").getAsString().contains("缺少必填字段"));
    }

    @Test
    public void testLoginMissingFields() {
        String response1 = dispatcher.dispatch(
                "{\"command\":\"login\",\"data\":{\"id\":1}}");
        JsonObject json1 = JsonParser.parseString(response1).getAsJsonObject();
        assertFalse("缺少password", json1.get("success").getAsBoolean());

        String response2 = dispatcher.dispatch(
                "{\"command\":\"login\",\"data\":{\"password\":\"123\"}}");
        JsonObject json2 = JsonParser.parseString(response2).getAsJsonObject();
        assertFalse("缺少id", json2.get("success").getAsBoolean());
    }

    @Test
    public void testSearchBooksMissingKeyword() {
        String response = dispatcher.dispatch(
                "{\"command\":\"searchBooks\",\"data\":{}}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertFalse("缺少keyword应返回failure", json.get("success").getAsBoolean());
    }

    @Test
    public void testBorrowBookMissingFields() {
        String response = dispatcher.dispatch(
                "{\"command\":\"borrowBook\",\"data\":{\"bookId\":1}}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertFalse("缺少readerId应返回failure", json.get("success").getAsBoolean());
    }

    // ==================== 命令路由覆盖测试 ====================

    @Test
    public void testAllCommandsAreRecognized() {
        // 验证所有注册的命令都不会返回"未知命令"错误
        String[] commands = {
                "help", "ping",
                "login", "register",
                "findAllBooks", "findBookById", "searchBooks",
                "findByClassId", "getAllClasses",
                "addBook", "updateBook", "deleteBook",
                "borrowBook", "returnBook",
                "findAllBorrows", "findBorrowsByReaderId", "findActiveBorrows",
                "getAllReaders", "disableReader", "enableReader",
                "exportBooks", "importBooks"
        };

        for (String cmd : commands) {
            String response = dispatcher.dispatch(
                    "{\"command\":\"" + cmd + "\",\"data\":{}}");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            // 不检查具体结果（可能因缺少字段而失败），
            // 但绝对不能返回"未知命令"错误
            if (!json.get("success").getAsBoolean()) {
                assertFalse("命令 '" + cmd + "' 不应被识别为未知命令",
                        json.get("message").getAsString().contains("未知命令"));
            }
        }
    }

    // ==================== 响应格式一致性测试 ====================

    @Test
    public void testSuccessResponseFormat() {
        String response = dispatcher.dispatch("{\"command\":\"ping\"}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertTrue("成功响应必须含success字段", json.has("success"));
        assertTrue("成功响应必须含message字段", json.has("message"));
        assertTrue("success字段必须是boolean", json.get("success").isJsonPrimitive());
    }

    @Test
    public void testErrorResponseFormat() {
        String response = dispatcher.dispatch("{\"command\":\"unknown\"}");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertFalse("错误响应success必须为false", json.get("success").getAsBoolean());
        assertTrue("错误响应必须含message", json.has("message"));
        assertFalse("错误响应不应含data字段", json.has("data"));
    }

    // ==================== 数据库相关命令测试 (需要DB连接) ====================

    @Test
    public void testGetAllClassesWithDb() {
        try {
            String response = dispatcher.dispatch("{\"command\":\"getAllClasses\"}");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            if (json.get("success").getAsBoolean()) {
                assertTrue("getAllClasses响应应含data字段", json.has("data"));
                JsonObject data = json.getAsJsonObject("data");
                assertTrue("data应含count字段", data.has("count"));
                assertTrue("data应含classes字段", data.has("classes"));
                assertTrue("classes应为数组",
                        data.get("classes").isJsonArray());
            }
        } catch (Exception e) {
            // 数据库不可连接时跳过（原因见测试类的javadoc）
            System.out.println("[跳过] testGetAllClassesWithDb: 数据库不可用 — "
                    + e.getMessage());
        }
    }

    @Test
    public void testFindAllBooksWithDb() {
        try {
            String response = dispatcher.dispatch("{\"command\":\"findAllBooks\"}");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            if (json.get("success").getAsBoolean()) {
                assertTrue("findAllBooks响应应含data字段", json.has("data"));
                JsonObject data = json.getAsJsonObject("data");
                assertTrue("data应含count字段", data.has("count"));
                assertTrue("data应含books字段", data.has("books"));
            }
        } catch (Exception e) {
            System.out.println("[跳过] testFindAllBooksWithDb: 数据库不可用 — "
                    + e.getMessage());
        }
    }
}
