package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

/**
 * 即时通信服务器主控
 * 长连接管理 + 心跳检测 + 线程池 + 断线清理
 */
public class ChatServer {
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ScheduledExecutorService heartbeatExecutor;
    private volatile boolean running;
    private final ConcurrentHashMap<ChatClientHandler, Long> handlers = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();  // 动态线程池
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start(java.util.function.Consumer<String> logger,
                      java.util.function.Consumer<Boolean> onResult) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // accept超时1秒，便于检查running状态
            running = true;

            // 启动心跳检测线程（每30秒检查一次）
            heartbeatExecutor.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);

            // 主接受循环
            Thread acceptThread = new Thread(() -> {
                logger.accept("IM服务器启动成功 - 端口:" + port);
                onResult.accept(true);

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        ChatClientHandler handler = new ChatClientHandler(client);
                        handlers.put(handler, System.currentTimeMillis());
                        threadPool.submit(handler);
                        logger.accept("新连接: " + client.getInetAddress());
                    } catch (SocketTimeoutException e) {
                        // 超时后继续循环
                    } catch (IOException e) {
                        if (running) logger.accept("接受连接异常: " + e.getMessage());
                    }
                }
            }, "ChatServer-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            onResult.accept(false);
            logger.accept("启动失败: " + e.getMessage());
        }
    }

    /**
     * 心跳检测：超过60秒无心跳的客户端强制断开
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        handlers.forEach((handler, lastSeen) -> {
            if (now - handler.getLastHeartbeat() > 60000) {
                handler.stop();
                handlers.remove(handler);
                System.out.println("[IM] 超时断开: " + Thread.currentThread().getName());
            }
        });
    }

    public void stop() {
        running = false;
        try { heartbeatExecutor.shutdownNow(); } catch (Exception ignored) {}
        try { threadPool.shutdownNow(); } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        System.out.println("[IM] 服务器已停止");
    }

    public boolean isRunning() { return running; }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        ChatServer server = new ChatServer(port);
        server.start(
                System.out::println,
                success -> {
                    if (!success) System.err.println("IM服务器启动失败");
                }
        );
        // 保持运行
        while (server.isRunning()) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }
}
