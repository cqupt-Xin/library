package server;

import chat.server.ChatServer;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 图书管理系统后台服务器
 * 实验九 C/S 模式 + 实验十 多线程
 * 自动附带启动IM服务器（端口 9000）
 */
public class BookCommandServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService threadPool;
    private ChatServer chatServer;

    private static final AtomicInteger CLIENT_THREAD_COUNTER = new AtomicInteger(1);
    private static final int THREAD_POOL_SIZE = 20;
    private static final int IM_PORT = 9000;

    public BookCommandServer(int port) { this.port = port; }
    public BookCommandServer() { this(8888); }

    public void start(JTextArea logArea, Consumer<Boolean> onResult) {
        start(msg -> SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(msg + "\n");
        }), onResult);
    }

    public void start(Consumer<String> logConsumer, Consumer<Boolean> onResult) {
        if (running) {
            if (logConsumer != null) logConsumer.accept("服务器已在运行中");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
                running = true;
                threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
                        r -> {
                            Thread thread = new Thread(r, "ClientHandler-" + CLIENT_THREAD_COUNTER.getAndIncrement());
                            thread.setDaemon(true);
                            return thread;
                        });

                // 自动启动IM服务器（端口9000）
                try {
                    chatServer = new ChatServer(IM_PORT);
                    chatServer.start(
                            msg -> {},  // IM日志静默
                            ok -> System.out.println("[IM] 即时通信服务已启动 — 端口:" + IM_PORT)
                    );
                } catch (Exception e) {
                    System.out.println("[IM] 即时通信服务启动失败: " + e.getMessage());
                }

                SwingUtilities.invokeLater(() -> { if (onResult != null) onResult.accept(true); });

                Consumer<String> safeLog = logConsumer != null ? logConsumer : msg -> {};
                safeLog.accept("========================================");
                safeLog.accept("服务端绑定成功 — 127.0.0.1:" + port + " | IM:" + IM_PORT);
                safeLog.accept("线程池大小: " + THREAD_POOL_SIZE + " | 支持多客户端并发");
                safeLog.accept("========================================");

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        threadPool.submit(new ClientHandler(client, safeLog));
                    } catch (IOException e) {
                        if (running) safeLog.accept("[错误] " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                running = false;
                SwingUtilities.invokeLater(() -> { if (onResult != null) onResult.accept(false); });
                if (logConsumer != null) logConsumer.accept("[失败] 端口绑定失败: " + e.getMessage());
            }
        }, "BookServer");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        if (chatServer != null) { chatServer.stop(); chatServer = null; }
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (threadPool != null && !threadPool.isShutdown()) threadPool.shutdownNow();
    }

    public boolean isRunning() { return running; }
}
