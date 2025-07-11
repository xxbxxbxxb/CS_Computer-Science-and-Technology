package org.pop3;
import javax.swing.*;

/**
 * 主程序入口类
 * 负责启动POP3邮件客户端程序
 * 支持GUI和CLI两种运行模式
 */
public class Main {
    /**
     * 程序主入口
     * @param args 命令行参数
     *           --cli 或 -c: 使用命令行界面
     *           --help 或 -h: 显示帮助信息
     */
    public static void main(String[] args) {
        // 检查命令行参数
        boolean useCLI = false;      // 是否使用命令行界面
        boolean showHelp = false;    // 是否显示帮助信息

        // 解析命令行参数
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--cli") || arg.equalsIgnoreCase("-c")) {
                useCLI = true;
            } else if (arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("-h")) {
                showHelp = true;
            }
        }

        // 如果需要显示帮助，显示后退出
        if (showHelp) {
            printHelp();
            return;
        }

        // 显示程序启动信息
        System.out.println("=== POP3邮件客户端程序 (SSL支持版) ===");
        System.out.println("版本: 2.0");
        System.out.println("功能: 连接POP3服务器、SSL加密、认证、邮件管理");
        System.out.println();

        if (useCLI) {
            // 启动命令行界面
            System.out.println("启动命令行界面...\n");
            POP3SSLClientCLI cli = new POP3SSLClientCLI();
            cli.start();
        } else {
            // 启动GUI界面
            System.out.println("启动图形界面...");

            try {
                // 设置系统原生外观
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("无法设置系统外观: " + e.getMessage());
            }

            // 在事件分发线程中创建和显示GUI
            SwingUtilities.invokeLater(() -> new POP3SSLClientGUI().setVisible(true));
        }
    }

    /**
     * 打印帮助信息
     * 显示程序的使用说明和支持的邮件服务器
     */
    private static void printHelp() {
        System.out.println("POP3邮件客户端 - 使用说明");
        System.out.println("===========================");
        System.out.println();
        System.out.println("用法: java -jar pop3client.jar [选项]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --cli, -c    使用命令行界面");
        System.out.println("  --help, -h   显示此帮助信息");
        System.out.println();
        System.out.println("默认启动图形界面，使用 --cli 参数启动命令行界面");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar pop3client.jar          # 启动GUI");
        System.out.println("  java -jar pop3client.jar --cli    # 启动CLI");
        System.out.println();
        System.out.println("支持的邮件服务器:");
        System.out.println("  - 网易邮箱 (pop.163.com:995)");
        System.out.println("  - QQ邮箱 (pop.qq.com:995)");
        System.out.println("  - Gmail (pop.gmail.com:995)");
        System.out.println("  - Outlook (outlook.office365.com:995)");
        System.out.println();
        System.out.println("注意: 大多数邮箱需要使用授权码而非密码登录");
    }
}