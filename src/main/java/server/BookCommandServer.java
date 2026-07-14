package server;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * TCP 命令服务器
 * 绑定到 localhost，通过回调通知 UI 启动结果，避免"假运行"状态
 */
public class BookCommandServer {

    private final int port;
    private final String bindAddr;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger clientCount = new AtomicInteger(0);
    private JTextArea logArea;

    /** 启动结果回调: success=true 表示端口绑定成功 */
    private Consumer<Boolean> onStartResult;

    public BookCommandServer(int port) {
        this(port, "127.0.0.1");
    }

    public BookCommandServer(int port, String bindAddr) {
        this.port = port;
        this.bindAddr = bindAddr;
    }

    public BookCommandServer() {
        this(8888);
    }

    /**
     * 启动服务器
     * @param logArea   日志输出区域
     * @param onStartResult 启动结果回调: true=绑定成功, false=绑定失败
     */
    public void start(JTextArea logArea, Consumer<Boolean> onStartResult) {
        if (running.get()) {
            appendLog("[警告] 服务器已在运行中");
            return;
        }
        this.logArea = logArea;
        this.onStartResult = onStartResult;
        threadPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ClientHandler");
            t.setDaemon(true);
            return t;
        });

        Thread listenThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(bindAddr, port), 50);  // 同步绑定

                // 绑定成功后才设置状态
                running.set(true);
                notifyStartResult(true);

                appendLog("========================================");
                appendLog("[服务端] 绑定成功");
                appendLog("[服务端] 地址: " + bindAddr + ":" + port);
                appendLog("[服务端] NetAssist 配置: TCP Client → " + bindAddr + ":" + port);
                appendLog("[服务端] 等待客户端连接...");
                appendLog("========================================");

                while (running.get()) {
                    try {
                        Socket client = serverSocket.accept();
                        int current = clientCount.incrementAndGet();
                        appendLog("[连接] " + client.getInetAddress().getHostAddress()
                                + ":" + client.getPort() + " (在线: " + current + ")");

                        ClientHandler handler = new ClientHandler(client,
                                BookCommandServer.this::onClientMessage,
                                BookCommandServer.this::onClientDisconnect);
                        threadPool.submit(handler);
                    } catch (IOException e) {
                        if (running.get()) {
                            appendLog("[错误] 接受连接异常: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                running.set(false);
                notifyStartResult(false);
                String msg = e.getMessage();
                appendLog("[失败] 端口绑定失败: " + msg);
                appendLog("----------------------------------------");
                if (msg != null && msg.contains("Address already in use")) {
                    appendLog("  → 端口 " + port + " 被占用，请换端口或执行:");
                    appendLog("     netstat -ano | findstr :" + port);
                } else if (msg != null && msg.contains("Permission denied")) {
                    appendLog("  → 权限不足，端口需 ≥ 1024，请换端口重试");
                } else {
                    appendLog("  → 请更换端口号后重试");
                }
                appendLog("----------------------------------------");
            }
        }, "ServerListener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    /** 通知 UI 启动结果 */
    private void notifyStartResult(boolean success) {
        if (onStartResult != null) {
            SwingUtilities.invokeLater(() -> onStartResult.accept(success));
        }
    }

    public void stop() {
        running.set(false);
        if (threadPool != null) threadPool.shutdownNow();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        appendLog("[服务端] 已停止");
    }

    public boolean isRunning() { return running.get(); }
    public int getClientCount() { return clientCount.get(); }

    // ==================== 回调 ====================
    private void onClientMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(msg + "\n");
        });
    }
    private void onClientDisconnect(String info) {
        clientCount.decrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(info + "\n");
        });
    }
    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(msg + "\n");
        });
    }
}
