package server;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * TCP 命令服务器 — 单线程阻塞模式
 * 一次仅处理一个客户端连接，逻辑直接内联，无线程池/并发控制。
 */
public class BookCommandServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private JTextArea logArea;

    public BookCommandServer(int port) { this.port = port; }
    public BookCommandServer() { this(8888); }

    /**
     * 启动服务器（后台线程中执行 accept 循环）
     */
    public void start(JTextArea logArea, Consumer<Boolean> onResult) {
        if (running) { log("服务器已在运行中"); return; }
        this.logArea = logArea;

        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
                running = true;
                SwingUtilities.invokeLater(() -> onResult.accept(true));

                log("========================================");
                log("服务端绑定成功 — 127.0.0.1:" + port);
                log("NetAssist 配置: TCP Client → 127.0.0.1:" + port);
                log("========================================");

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        String addr = client.getInetAddress().getHostAddress() + ":" + client.getPort();
                        log("[连接] " + addr);
                        handleClient(client, addr);
                        log("[断开] " + addr);
                    } catch (IOException e) {
                        if (running) log("[错误] " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                running = false;
                SwingUtilities.invokeLater(() -> onResult.accept(false));
                log("[失败] 端口绑定失败: " + e.getMessage());
            }
        }, "BookServer");
        t.setDaemon(true);
        t.start();
    }

    /** 处理单个客户端连接的全部请求，直到断开 */
    private void handleClient(Socket socket, String addr) {
        Dispatcher dispatcher = new Dispatcher();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                log("[收] " + addr + " → " + line);
                String resp = dispatcher.dispatch(line.trim());
                log("[发] → " + addr + " : " + resp);
                out.write(resp);
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {
            log("[异常] " + addr + " : " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        log("服务端已停止");
    }

    public boolean isRunning() { return running; }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(msg + "\n");
        });
    }
}
