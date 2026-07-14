package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 客户端连接处理器
 * 每个 TCP 连接一个实例，以行读取 JSON 命令，通过 Dispatcher 路由执行，
 * 结果以单行 JSON 写回客户端。
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Consumer<String> logCallback;       // 日志回调
    private final Consumer<String> disconnectCallback; // 断开回调
    private final Dispatcher dispatcher = new Dispatcher();

    public ClientHandler(Socket socket,
                         Consumer<String> logCallback,
                         Consumer<String> disconnectCallback) {
        this.clientSocket = socket;
        this.logCallback = logCallback;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 空行跳过
                if (line.trim().isEmpty()) {
                    continue;
                }

                logCallback.accept("[接收] " + clientInfo + " → " + line);

                // 通过 Dispatcher 路由命令
                String response = dispatcher.dispatch(line.trim());

                logCallback.accept("[响应] → " + clientInfo + " : " + response);

                // 写回客户端（一行 JSON）
                writer.write(response);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException e) {
            logCallback.accept("[错误] 与 " + clientInfo + " 通信异常: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            disconnectCallback.accept("[断开] 客户端: " + clientInfo);
        }
    }
}
