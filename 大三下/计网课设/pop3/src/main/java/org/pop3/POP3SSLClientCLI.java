package org.pop3;

import java.io.Console;
import java.io.File;
import java.util.List;
import java.util.Scanner;

public class POP3SSLClientCLI {
    private POP3SSLClient client;
    private Scanner scanner;
    private boolean running = true;
    private EmailMessage currentEmail;
    private ConfigManager config;

    // ANSI颜色代码
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    public POP3SSLClientCLI() {
        this.scanner = new Scanner(System.in);
        this.config = new ConfigManager();
    }

    public void start() {
        printWelcome();

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    connectToServer();
                    break;
                case "2":
                    login();
                    break;
                case "3":
                    listMessages();
                    break;
                case "4":
                    viewMessage();
                    break;
                case "5":
                    viewAttachments();
                    break;
                case "6":
                    downloadAttachment();
                    break;
                case "7":
                    saveCompleteEmail();
                    break;
                case "8":
                    deleteMessage();
                    break;
                case "9":
                    showMailboxInfo();
                    break;
                case "10":
                    disconnect();
                    break;
                case "0":
                case "q":
                case "quit":
                case "exit":
                    quit();
                    break;
                default:
                    printError("无效的选项，请重新选择");
            }

            if (running) {
                System.out.println("\n按回车键继续...");
                scanner.nextLine();
            }
        }
    }

    private void printWelcome() {
        clearScreen();
        System.out.println(CYAN + "╔══════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + BOLD + "      POP3 邮件客户端 (命令行版)        " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + "         版本 2.0 - SSL 支持             " + CYAN + "║" + RESET);
        System.out.println(CYAN + "╚══════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private void printMenu() {
        System.out.println("\n" + BLUE + "========== 主菜单 ==========" + RESET);
        System.out.println("当前状态: " + getConnectionStatus());
        System.out.println();
        System.out.println("1.  连接服务器");
        System.out.println("2.  登录邮箱");
        System.out.println("3.  列出所有邮件");
        System.out.println("4.  查看邮件内容");
        System.out.println("5.  查看邮件附件");
        System.out.println("6.  下载附件");
        System.out.println("7.  保存完整邮件");
        System.out.println("8.  删除邮件");
        System.out.println("9.  显示邮箱信息");
        System.out.println("10. 断开连接");
        System.out.println("0.  退出程序");
        System.out.println(BLUE + "============================" + RESET);
        System.out.print("请选择操作: ");
    }

    private String getConnectionStatus() {
        if (client == null || !client.isConnected()) {
            return RED + "未连接" + RESET;
        } else if (!client.isAuthenticated()) {
            return YELLOW + "已连接，未登录" + RESET;
        } else {
            return GREEN + "已登录" + RESET;
        }
    }

    private void connectToServer() {
        clearScreen();
        System.out.println(BOLD + "=== 连接到邮件服务器 ===" + RESET);

        // 从配置读取默认值
        String defaultServer = config.getServer();
        int defaultPort = config.getPort();
        boolean defaultSSL = config.isUseSSL();

        System.out.print("服务器地址 [" + defaultServer + "]: ");
        String server = scanner.nextLine().trim();
        if (server.isEmpty()) server = defaultServer;

        System.out.print("端口号 [" + defaultPort + "]: ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? defaultPort : Integer.parseInt(portStr);

        System.out.print("使用SSL加密 (y/n) [" + (defaultSSL ? "y" : "n") + "]: ");
        String sslChoice = scanner.nextLine().trim().toLowerCase();
        boolean useSSL = sslChoice.isEmpty() ? defaultSSL : !sslChoice.equals("n");

        // 保存配置
        config.setServer(server);
        config.setPort(port);
        config.setUseSSL(useSSL);
        config.saveConfig();

        System.out.println("\n正在连接到 " + server + ":" + port + (useSSL ? " (SSL)" : "") + "...");

        client = new POP3SSLClient(server, port, useSSL);
        if (client.connect()) {
            printSuccess("成功连接到服务器！");
        } else {
            printError("连接失败，请检查服务器地址和端口");
            client = null;
        }
    }

    private void login() {
        if (!checkConnection()) return;

        if (client.isAuthenticated()) {
            printWarning("您已经登录");
            return;
        }

        clearScreen();
        System.out.println(BOLD + "=== 登录邮箱 ===" + RESET);

        String defaultUsername = config.getUsername();
        System.out.print("用户名/邮箱" + (defaultUsername.isEmpty() ? "" : " [" + defaultUsername + "]") + ": ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty() && !defaultUsername.isEmpty()) {
            username = defaultUsername;
        }

        String password;
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("密码/授权码: ");
            password = new String(passwordChars);
        } else {
            System.out.print("密码/授权码: ");
            password = scanner.nextLine();
        }

        System.out.println("\n正在登录...");
        if (client.login(username, password)) {
            printSuccess("登录成功！");
            // 保存用户名
            config.setUsername(username);
            config.saveConfig();
            showMailboxInfo();
        } else {
            printError("登录失败，请检查用户名和密码\n注意：大多数邮箱需要使用授权码而非密码");
        }
    }

    private void listMessages() {
        if (!checkAuthentication()) return;

        clearScreen();
        System.out.println(BOLD + "=== 邮件列表 ===" + RESET);

        List<String> messages = client.getMessageList();
        if (messages == null || messages.isEmpty()) {
            printWarning("邮箱中没有邮件");
            return;
        }

        System.out.println("共 " + GREEN + messages.size() + RESET + " 封邮件\n");
        System.out.println(String.format("%-6s %-10s", "序号", "大小"));
        System.out.println("─────────────────────");

        for (String message : messages) {
            String[] parts = message.split(" ");
            if (parts.length >= 2) {
                String num = parts[0];
                String size = formatFileSize(Long.parseLong(parts[1]));
                System.out.println(String.format("%-6s %-10s", num, size));
            }
        }
    }

    private void viewMessage() {
        if (!checkAuthentication()) return;

        System.out.print("请输入邮件序号: ");
        String input = scanner.nextLine().trim();

        try {
            int messageNumber = Integer.parseInt(input);
            System.out.println("\n正在获取邮件内容...");

            String rawContent = client.retrieveMessage(messageNumber);
            if (rawContent == null) {
                printError("获取邮件失败");
                return;
            }

            currentEmail = new EmailMessage(messageNumber, rawContent);

            clearScreen();
            System.out.println(BOLD + "=== 邮件详情 ===" + RESET);
            System.out.println(currentEmail.getDetailedInfo());

            System.out.println("\n" + BOLD + "=== 邮件正文 ===" + RESET);
            String content = currentEmail.getContent();
            if (content != null && !content.trim().isEmpty()) {
                // 限制显示行数，避免内容过长
                String[] lines = content.split("\n");
                int maxLines = 50;
                for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
                    System.out.println(lines[i]);
                }
                if (lines.length > maxLines) {
                    System.out.println("\n... 内容过长，已截断 ...");
                }
            } else {
                System.out.println("无内容或内容解码失败");
            }

            if (!currentEmail.getAttachments().isEmpty()) {
                System.out.println("\n" + YELLOW + "此邮件包含 " +
                        currentEmail.getAttachments().size() + " 个附件" + RESET);
            }

        } catch (NumberFormatException e) {
            printError("无效的邮件序号");
        }
    }

    private void viewAttachments() {
        if (currentEmail == null) {
            printWarning("请先查看一封邮件");
            return;
        }

        List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
        if (attachments.isEmpty()) {
            printWarning("当前邮件没有附件");
            return;
        }

        clearScreen();
        System.out.println(BOLD + "=== 附件列表 ===" + RESET);
        System.out.println("邮件序号: " + currentEmail.getMessageNumber());
        System.out.println();

        System.out.println(String.format("%-4s %-30s %-20s %-10s",
                "序号", "文件名", "类型", "大小"));
        System.out.println("─".repeat(70));

        for (int i = 0; i < attachments.size(); i++) {
            EmailMessage.EmailPart att = attachments.get(i);
            String fileName = att.getFileName() != null ? att.getFileName() : "未命名附件";
            String type = att.getContentType() != null ? att.getContentType() : "未知";
            String size = formatFileSize(att.getSize());

            System.out.println(String.format("%-4d %-30s %-20s %-10s",
                    i + 1, truncate(fileName, 30), truncate(type, 20), size));
        }
    }

    private void downloadAttachment() {
        if (currentEmail == null) {
            printWarning("请先查看一封邮件");
            return;
        }

        List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
        if (attachments.isEmpty()) {
            printWarning("当前邮件没有附件");
            return;
        }

        viewAttachments();

        System.out.print("\n请输入要下载的附件序号 (0下载全部): ");
        String input = scanner.nextLine().trim();

        try {
            int choice = Integer.parseInt(input);

            if (choice == 0) {
                downloadAllAttachments();
            } else if (choice > 0 && choice <= attachments.size()) {
                EmailMessage.EmailPart attachment = attachments.get(choice - 1);
                downloadSingleAttachment(attachment);
            } else {
                printError("无效的附件序号");
            }

        } catch (NumberFormatException e) {
            printError("请输入有效的数字");
        }
    }

    private void downloadSingleAttachment(EmailMessage.EmailPart attachment) {
        String fileName = attachment.getFileName() != null ?
                attachment.getFileName() : "attachment";

        System.out.print("保存为 [" + fileName + "]: ");
        String saveAs = scanner.nextLine().trim();
        if (saveAs.isEmpty()) saveAs = fileName;

        File targetFile = new File(saveAs);
        if (currentEmail.saveAttachment(attachment, targetFile)) {
            printSuccess("附件已保存到: " + targetFile.getAbsolutePath());
        } else {
            printError("保存附件失败");
        }
    }

    private void downloadAllAttachments() {
        System.out.print("保存到目录 [./attachments]: ");
        String dirPath = scanner.nextLine().trim();
        if (dirPath.isEmpty()) dirPath = "./attachments";

        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
        int successCount = 0;

        for (EmailMessage.EmailPart attachment : attachments) {
            String fileName = attachment.getFileName() != null ?
                    attachment.getFileName() : "attachment_" + (successCount + 1);
            File targetFile = new File(dir, fileName);

            if (currentEmail.saveAttachment(attachment, targetFile)) {
                successCount++;
                System.out.println(GREEN + "?" + RESET + " 已保存: " + fileName);
            } else {
                System.out.println(RED + "?" + RESET + " 失败: " + fileName);
            }
        }

        printSuccess("共保存 " + successCount + "/" + attachments.size() + " 个附件");
    }

    private void saveCompleteEmail() {
        if (currentEmail == null) {
            printWarning("请先查看一封邮件");
            return;
        }

        System.out.print("保存到目录 [./emails]: ");
        String dirPath = scanner.nextLine().trim();
        if (dirPath.isEmpty()) dirPath = "./emails";

        File targetDir = new File(dirPath, "email_" + currentEmail.getMessageNumber() +
                "_" + System.currentTimeMillis());

        if (currentEmail.saveCompleteEmail(targetDir)) {
            printSuccess("完整邮件已保存到: " + targetDir.getAbsolutePath());
        } else {
            printError("保存失败");
        }
    }

    private void deleteMessage() {
        if (!checkAuthentication()) return;

        System.out.print("请输入要删除的邮件序号: ");
        String input = scanner.nextLine().trim();

        try {
            int messageNumber = Integer.parseInt(input);

            System.out.print(YELLOW + "确定要删除邮件 #" + messageNumber + " 吗？(y/n): " + RESET);
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("y") || confirm.equals("yes")) {
                if (client.deleteMessage(messageNumber)) {
                    printSuccess("邮件删除成功");
                    if (currentEmail != null && currentEmail.getMessageNumber() == messageNumber) {
                        currentEmail = null;
                    }
                } else {
                    printError("删除失败");
                }
            } else {
                System.out.println("已取消删除");
            }

        } catch (NumberFormatException e) {
            printError("无效的邮件序号");
        }
    }

    private void showMailboxInfo() {
        if (!checkAuthentication()) return;

        int[] stat = client.getMailboxStat();
        if (stat != null) {
            System.out.println("\n" + CYAN + "邮箱信息:" + RESET);
            System.out.println("邮件数量: " + GREEN + stat[0] + RESET);
            System.out.println("总大小: " + GREEN + formatFileSize(stat[1]) + RESET);
        }
    }

    private void disconnect() {
        if (client == null) {
            printWarning("当前未连接");
            return;
        }

        client.quit();
        client = null;
        currentEmail = null;
        printSuccess("已断开连接");
    }

    private void quit() {
        System.out.print(YELLOW + "确定要退出吗？(y/n): " + RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            if (client != null) {
                client.quit();
            }
            running = false;
            System.out.println("\n" + GREEN + "感谢使用，再见！" + RESET);
        }
    }

    // 辅助方法
    private boolean checkConnection() {
        if (client == null || !client.isConnected()) {
            printError("请先连接到服务器");
            return false;
        }
        return true;
    }

    private boolean checkAuthentication() {
        if (!checkConnection()) return false;
        if (!client.isAuthenticated()) {
            printError("请先登录");
            return false;
        }
        return true;
    }

    private void printSuccess(String message) {
        System.out.println(GREEN + "? " + message + RESET);
    }

    private void printError(String message) {
        System.out.println(RED + "? " + message + RESET);
    }

    private void printWarning(String message) {
        System.out.println(YELLOW + "! " + message + RESET);
    }

    private void clearScreen() {
        // 跨平台清屏
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // 如果清屏失败，打印一些空行
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    public static void main(String[] args) {
        POP3SSLClientCLI cli = new POP3SSLClientCLI();
        cli.start();
    }
}