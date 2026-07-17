package integration;

import org.junit.Test;
import server.BookCommandServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * 集成测试 — 服务端启动/停止与客户端通信
 * 
 * 验证服务端能正常启动、监听端口、处理客户端请求并响应。
 */
public class ServerIntegrationTest {

    private static final int TEST_PORT = 18888;

    // ==================== 服务端启停测试 ====================

    @Test
    public void testServerStartAndStop() throws InterruptedException {
        BookCommandServer server = new BookCommandServer(TEST_PORT);

        // 启动服务端（不依赖UI的Consumer版本）
        server.start(
                msg -> System.out.println("[服务端日志] " + msg),
                success -> {
                    if (!success) {
                        System.err.println("[服务端] 启动失败");
                    }
                }
        );

        // 等待服务端启动
        Thread.sleep(1000);

        assertTrue("服务端应处于运行状态", server.isRunning());

        // 停止服务端
        server.stop();
        Thread.sleep(500);

        assertFalse("服务端应已停止", server.isRunning());
        System.out.println("[集成测试] 服务端启停测试通过 ✓");
    }

    // ==================== 客户端连接测试 ====================

    @Test
    public void testClientConnectionAndPing() throws Exception {
        BookCommandServer server = new BookCommandServer(TEST_PORT);

        // 启动服务端
        server.start(
                msg -> System.out.println("[服务端] " + msg),
                success -> {}
        );

        Thread.sleep(1000);

        try {
            assertTrue("服务端应启动成功", server.isRunning());

            // 客户端连接并发送ping
            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                // 发送 ping 命令
                out.write("{\"command\":\"ping\"}");
                out.newLine();
                out.flush();

                String response = in.readLine();
                assertNotNull("应收到服务端响应", response);
                assertTrue("响应应为有效JSON", response.contains("success"));
                assertTrue("响应应含pong", response.contains("pong"));
                System.out.println("[客户端] ping响应: " + response);
            }

            // 测试 help 命令
            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out.write("{\"command\":\"help\"}");
                out.newLine();
                out.flush();

                String response = in.readLine();
                assertNotNull("应收到help响应", response);
                assertTrue("help响应应包含命令列表", response.contains("help"));
                assertTrue("help响应应包含addBook", response.contains("addBook"));
                System.out.println("[客户端] help响应: " + response.substring(0, Math.min(80, response.length())) + "...");
            }

            System.out.println("[集成测试] 客户端连接通信测试通过 ✓");
        } finally {
            server.stop();
            Thread.sleep(500);
        }
    }

    // ==================== 无效命令测试 ====================

    @Test
    public void testUnknownCommandResponse() throws Exception {
        BookCommandServer server = new BookCommandServer(TEST_PORT);

        server.start(
                msg -> System.out.println("[服务端] " + msg),
                success -> {}
        );

        Thread.sleep(1000);

        try {
            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out.write("{\"command\":\"invalidCmd\"}");
                out.newLine();
                out.flush();

                String response = in.readLine();
                assertNotNull("应收到错误响应", response);
                assertTrue("错误响应应含success:false", response.contains("\"success\":false"));
                assertTrue("错误响应应提示未知命令", response.contains("未知命令"));
                System.out.println("[客户端] 未知命令响应: " + response);
            }

            System.out.println("[集成测试] 未知命令处理测试通过 ✓");
        } finally {
            server.stop();
            Thread.sleep(500);
        }
    }

    // ==================== JSON格式错误测试 ====================

    @Test
    public void testMalformedJsonResponse() throws Exception {
        BookCommandServer server = new BookCommandServer(TEST_PORT);

        server.start(
                msg -> System.out.println("[服务端] " + msg),
                success -> {}
        );

        Thread.sleep(1000);

        try {
            try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                // 发送非法的JSON
                out.write("这不是JSON");
                out.newLine();
                out.flush();

                String response = in.readLine();
                assertNotNull("应收到错误响应", response);
                assertTrue("响应应含success:false", response.contains("\"success\":false"));
                assertTrue("响应应提示JSON格式错误", response.contains("格式错误"));
                System.out.println("[客户端] 非法JSON响应: " + response);
            }

            System.out.println("[集成测试] 非法JSON处理测试通过 ✓");
        } finally {
            server.stop();
            Thread.sleep(500);
        }
    }

    // ==================== 并发连接测试 ====================

    @Test
    public void testMultipleConcurrentConnections() throws Exception {
        BookCommandServer server = new BookCommandServer(TEST_PORT);

        server.start(
                msg -> System.out.println("[服务端] " + msg),
                success -> {}
        );

        Thread.sleep(1000);

        try {
            // 同时建立3个连接
            Thread t1 = new Thread(() -> sendPingAndVerify("客户端1"));
            Thread t2 = new Thread(() -> sendPingAndVerify("客户端2"));
            Thread t3 = new Thread(() -> sendPingAndVerify("客户端3"));

            t1.start(); t2.start(); t3.start();
            t1.join(); t2.join(); t3.join();

            System.out.println("[集成测试] 多客户端并发连接测试通过 ✓");
        } finally {
            server.stop();
            Thread.sleep(500);
        }
    }

    private void sendPingAndVerify(String clientName) {
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.write("{\"command\":\"ping\"}");
            out.newLine();
            out.flush();

            String response = in.readLine();
            assertNotNull(clientName + "应收到响应", response);
            assertTrue(clientName + "响应应含pong", response.contains("pong"));
            System.out.println("[" + clientName + "] ping成功 ✓");
        } catch (Exception e) {
            fail(clientName + "连接失败: " + e.getMessage());
        }
    }

    // ==================== 端口不可重用验证 ====================

    @Test
    public void testServerPortBinding() throws Exception {
        BookCommandServer server1 = new BookCommandServer(TEST_PORT);
        server1.start(msg -> {}, success -> {});
        Thread.sleep(1000);

        try {
            assertTrue("服务器1应启动成功", server1.isRunning());

            // 尝试再绑定同一端口（应失败或返回running=false）
            BookCommandServer server2 = new BookCommandServer(TEST_PORT);
            // server2的start是非阻塞的，但它内部应catch到端口占用错误
            server2.start(msg -> {}, success -> {});
            Thread.sleep(500);

            System.out.println("[集成测试] 端口占用处理测试完成");
        } finally {
            server1.stop();
            Thread.sleep(500);
        }
    }
}
