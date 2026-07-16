package server;

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
 * 图书管理系统后台服务器 — 实验九 C/S 模式 + 实验十 多线程
 * 使用线程池处理多个客户端并发连接
 */
public class BookCommandServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService threadPool;

    private static final AtomicInteger CLIENT_THREAD_COUNTER = new AtomicInteger(1);
    private static final int THREAD_POOL_SIZE = 20;

    public BookCommandServer(int port) { this.port = port; }
    public BookCommandServer() { this(8888); }

    /**
     * 启动服务器 — 兼容原有 JTextArea 接口
     */
    public void start(JTextArea logArea, Consumer<Boolean> onResult) {
        start(msg -> {
            SwingUtilities.invokeLater(() -> {
                if (logArea != null) logArea.append(msg + "\n");
            });
        }, onResult);
    }

    /**
     * 启动服务器（线程池模式）
     */
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
                            Thread thread = new Thread(r,
                                    "ClientHandler-" + CLIENT_THREAD_COUNTER.getAndIncrement());
                            thread.setDaemon(true);
                            return thread;
                        });

                SwingUtilities.invokeLater(() -> {
                    if (onResult != null) onResult.accept(true);
                });

                Consumer<String> safeLog = logConsumer != null ? logConsumer : msg -> {};
                safeLog.accept("========================================");
                safeLog.accept("服务端绑定成功 — 127.0.0.1:" + port);
                safeLog.accept("线程池大小: " + THREAD_POOL_SIZE + " | 支持多客户端并发");
                safeLog.accept("========================================");

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        // 实验十：每个客户端连接提交到线程池中独立处理
                        threadPool.submit(new ClientHandler(client, safeLog));
                    } catch (IOException e) {
                        if (running) safeLog.accept("[错误] " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                running = false;
                SwingUtilities.invokeLater(() -> {
                    if (onResult != null) onResult.accept(false);
                });
                if (logConsumer != null) {
                    logConsumer.accept("[失败] 端口绑定失败: " + e.getMessage());
                }
            }
        }, "BookServer");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 停止服务器并关闭线程池
     */
    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
    }

    public boolean isRunning() { return running; }
}
