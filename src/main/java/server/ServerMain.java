package server;

import java.io.PrintStream;
import java.nio.charset.Charset;
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
    private static volatile BookCommandServer server;

    public static void main(String[] args) {
        // 设置 Windows 终端为 UTF-8 编码，解决中文乱码
        try {
            new ProcessBuilder("cmd", "/c", "chcp 65001 >nul").inheritIO().start().waitFor();
        } catch (Exception ignored) {}
        try {
            System.setOut(new PrintStream(System.out, true, Charset.forName("UTF-8")));
        } catch (Exception ignored) {}

        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("无效端口号，使用默认端口 8888");
            }
        }

        printBanner(port);

        server = new BookCommandServer(port);
        server.start(
                msg -> System.out.println("[" + LocalDateTime.now().format(TIME_FMT) + "] " + msg),
                success -> {
                    if (success) {
                        System.out.println("[服务端] 启动成功，等待客户端连接...");
                        printHelp();
                    } else {
                        System.err.println("[服务端] 启动失败，请检查端口是否被占用");
                    }
                }
        );

        // 等待服务端异步启动完成（最多等10秒）
        System.out.print("正在启动服务端...");
        for (int i = 0; i < 100 && !server.isRunning(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        System.out.println();

        if (!server.isRunning()) {
            System.err.println("[服务端] 启动失败，端口可能被占用或数据库不可连接");
            System.err.println("按回车键退出...");
            try { System.in.read(); } catch (Exception ignored) {}
            return;
        }

        // 注册 Ctrl+C 优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null && server.isRunning()) {
                System.out.println();
                System.out.println("[服务端] 收到终止信号，正在关闭...");
                server.stop();
                System.out.println("[服务端] 已安全关闭");
            }
        }));

        // 控制台交互循环
        Scanner scanner = new Scanner(System.in, Charset.forName("UTF-8"));
        while (server.isRunning()) {
            System.out.print("> ");
            System.out.flush();

            if (!scanner.hasNextLine()) break;
            String cmd = scanner.nextLine().trim();
            if (cmd.isEmpty()) continue;

            switch (cmd.toLowerCase()) {
                case "stop":
                case "exit":
                case "quit":
                    break;
                case "help":
                case "?":
                    printHelp();
                    continue;
                case "status":
                    System.out.println("[服务端] 运行中 — 端口: " + port + " | 协议: TCP/JSON");
                    continue;
                case "clear":
                case "cls":
                    for (int i = 0; i < 50; i++) System.out.println();
                    continue;
                default:
                    System.out.println("未知命令: " + cmd + " (输入 'help' 查看可用命令)");
                    continue;
            }
            break;
        }

        System.out.println("[服务端] 正在停止...");
        server.stop();
        System.out.println("[服务端] 已停止，按回车键退出...");
        scanner.close();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("  可用命令:");
        System.out.println("  ┌──────────┬────────────────┐");
        System.out.println("  │ stop     │ 停止并退出服务端 │");
        System.out.println("  │ status   │ 查看运行状态    │");
        System.out.println("  │ help / ? │ 显示此帮助      │");
        System.out.println("  │ cls      │ 清屏            │");
        System.out.println("  └──────────┴────────────────┘");
        System.out.println();
    }

    private static void printBanner(int port) {
        System.out.println();
        System.out.println("  ========================================");
        System.out.println("    图书管理系统 — 后台服务端  V8.0");
        System.out.println("    协议: TCP / JSON 文本行");
        System.out.println("    绑定: 127.0.0.1:" + port);
        System.out.println("    线程: 线程池 20 并发");
        System.out.println("  ========================================");
        System.out.println();
    }
}
