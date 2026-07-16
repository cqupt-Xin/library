package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * 图书管理系统 — 独立服务端启动入口
 * 实验九 C/S 模式 + 实验十 多线程
 *
 * 运行方式：java server.ServerMain [端口号]
 * 默认端口：8888
 *
 * 此程序仅包含服务端功能，不包含任何 UI 界面。
 * 客户端请运行 ui.LoginFrame
 */
public class ServerMain {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("无效端口号，使用默认端口 8888");
            }
        }

        printBanner(port);

        BookCommandServer server = new BookCommandServer(port);
        server.start(
                // 日志消费者 — 输出到控制台
                msg -> System.out.println("[" + LocalDateTime.now().format(TIME_FMT) + "] " + msg),
                // 启动结果回调
                success -> {
                    if (success) {
                        System.out.println("[服务端] 启动成功，等待客户端连接...");
                        System.out.println("[服务端] 输入 'stop' 停止服务器");
                    } else {
                        System.err.println("[服务端] 启动失败，请检查端口是否被占用");
                        System.exit(1);
                    }
                }
        );

        // 控制台命令输入
        Scanner scanner = new Scanner(System.in);
        while (server.isRunning()) {
            String cmd = scanner.nextLine().trim();
            if ("stop".equalsIgnoreCase(cmd) || "exit".equalsIgnoreCase(cmd)
                    || "quit".equalsIgnoreCase(cmd)) {
                break;
            }
            if ("status".equalsIgnoreCase(cmd)) {
                System.out.println("[服务端] 运行中 — 端口: " + port);
            }
        }

        System.out.println("[服务端] 正在停止...");
        server.stop();
        System.out.println("[服务端] 已停止");
        scanner.close();
    }

    private static void printBanner(int port) {
        System.out.println();
        System.out.println("============================================");
        System.out.println("  图书管理系统 — 后台服务端  V8.0");
        System.out.println("  协议: TCP / JSON 文本行");
        System.out.println("  绑定: 127.0.0.1:" + port);
        System.out.println("  线程: 线程池 20 并发 (实验十)");
        System.out.println("============================================");
        System.out.println();
    }
}
