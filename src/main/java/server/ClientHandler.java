package server;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 客户端处理器 — 实验十 多线程核心
 * 每个客户端连接分配一个独立线程处理，支持多个客户端并发访问
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Consumer<String> logger;

    public ClientHandler(Socket socket, Consumer<String> logger) {
        this.socket = socket;
        this.logger = logger;
    }

    @Override
    public void run() {
        String addr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        log("[连接] " + addr + " (线程: " + Thread.currentThread().getName() + ")");

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
            log("[断开] " + addr);
        }
    }

    private void log(String msg) {
        if (logger != null) {
            SwingUtilities.invokeLater(() -> logger.accept(msg));
        }
    }
}
