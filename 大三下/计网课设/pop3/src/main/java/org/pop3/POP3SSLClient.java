package org.pop3;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * POP3 SSL客户端类
 * 实现POP3协议的核心功能，支持SSL/TLS加密连接
 * 提供连接、认证、邮件获取、删除等基本操作
 */
public class POP3SSLClient {
    private Socket socket;              // 网络套接字
    private BufferedReader reader;      // 输入流读取器
    private PrintWriter writer;         // 输出流写入器
    private String server;              // 服务器地址
    private int port;                   // 服务器端口
    private boolean connected = false;  // 连接状态标志
    private boolean authenticated = false; // 认证状态标志
    private boolean useSSL = false;     // 是否使用SSL
    private Timer keepAliveTimer;       // 保持连接的定时器
    private static final int KEEP_ALIVE_INTERVAL = 30 * 1000; // 保持活动间隔：30秒

    /**
     * 构造函数
     * @param server 服务器地址
     * @param port 服务器端口
     * @param useSSL 是否使用SSL加密
     */
    public POP3SSLClient(String server, int port, boolean useSSL) {
        this.server = server;
        this.port = port;
        this.useSSL = useSSL;
    }

    /**
     * 连接到POP3服务器
     * @return 连接是否成功
     */
    public boolean connect() {
        try {
            if (useSSL) {
                // 创建SSL套接字
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(server, port);
                System.out.println("使用SSL连接到: " + server + ":" + port);
            } else {
                // 创建普通套接字
                socket = new Socket(server, port);
                System.out.println("使用普通连接到: " + server + ":" + port);
            }

            // 设置套接字选项
            socket.setKeepAlive(true);  // 启用TCP保持活动
            socket.setSoTimeout(KEEP_ALIVE_INTERVAL); // 设置读取超时

            // 创建输入输出流
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), "UTF-8"));

            writer = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), "UTF-8"), true);

            // 读取服务器欢迎消息
            String response = reader.readLine();
            System.out.println("服务器响应: " + response);

            // 检查响应是否为成功状态
            if (response != null && response.startsWith("+OK")) {
                connected = true;
                return true;
            }
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            handleDisconnection();
        }
        return false;
    }

    /**
     * 登录到POP3服务器
     * @param username 用户名
     * @param password 密码或授权码
     * @return 登录是否成功
     */
    public boolean login(String username, String password) {
        if (!isConnected()) {
            System.err.println("未连接到服务器");
            return false;
        }

        try {
            // 发送USER命令
            writer.println("USER " + username);
            String response = reader.readLine();
            System.out.println("USER响应: " + response);

            if (!response.startsWith("+OK")) {
                return false;
            }

            // 发送PASS命令
            writer.println("PASS " + password);
            response = reader.readLine();
            System.out.println("PASS响应: " + response);

            if (response.startsWith("+OK")) {
                authenticated = true;
                startKeepAlive();  // 启动保持连接机制
                return true;
            }
        } catch (IOException e) {
            System.err.println("登录失败: " + e.getMessage());
            handleDisconnection();
        }
        return false;
    }

    /**
     * 获取邮箱统计信息
     * @return 包含邮件数量和总大小的数组，[0]=邮件数，[1]=总大小（字节）
     */
    public int[] getMailboxStat() {
        if (!authenticated) {
            System.err.println("用户未认证");
            return null;
        }

        try {
            // 发送STAT命令
            writer.println("STAT");
            String response = reader.readLine();

            // 解析响应：+OK 邮件数 总大小
            if (response.startsWith("+OK")) {
                String[] parts = response.split(" ");
                if (parts.length >= 3) {
                    int count = Integer.parseInt(parts[1]);  // 邮件数量
                    int size = Integer.parseInt(parts[2]);   // 总大小
                    return new int[]{count, size};
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("获取邮箱统计失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取邮件列表
     * @return 邮件列表，每个元素格式为"序号 大小"
     */
    public List<String> getMessageList() {
        if (!authenticated) {
            System.err.println("用户未认证");
            return null;
        }

        List<String> messageList = new ArrayList<>();
        try {
            // 发送LIST命令
            writer.println("LIST");
            String response = reader.readLine();

            if (response.startsWith("+OK")) {
                String line;
                // 读取邮件列表，直到遇到单独的"."行
                while ((line = reader.readLine()) != null && !line.equals(".")) {
                    messageList.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("获取邮件列表失败: " + e.getMessage());
        }
        return messageList;
    }

    /**
     * 获取指定邮件的内容
     * @param messageNumber 邮件序号
     * @return 邮件的完整内容
     */
    public String retrieveMessage(int messageNumber) {
        if (!authenticated) {
            System.err.println("用户未认证");
            return null;
        }

        StringBuilder messageContent = new StringBuilder();
        try {
            // 发送RETR命令
            writer.println("RETR " + messageNumber);
            String response = reader.readLine();

            if (response.startsWith("+OK")) {
                String line;
                // 读取邮件内容，直到遇到单独的"."行
                while ((line = reader.readLine()) != null && !line.equals(".")) {
                    messageContent.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            System.err.println("获取邮件内容失败: " + e.getMessage());
        }
        return messageContent.toString();
    }

    /**
     * 标记删除指定邮件
     * 注意：邮件只是被标记删除，实际删除在QUIT命令后执行
     * @param messageNumber 邮件序号
     * @return 是否成功标记删除
     */
    public boolean deleteMessage(int messageNumber) {
        if (!authenticated) {
            System.err.println("用户未认证");
            return false;
        }

        try {
            // 发送DELE命令
            writer.println("DELE " + messageNumber);
            String response = reader.readLine();
            return response.startsWith("+OK");
        } catch (IOException e) {
            System.err.println("删除邮件失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 退出并关闭连接
     * 此时会真正删除被标记删除的邮件
     */
    public void quit() {
        // 停止保持连接定时器
        stopKeepAlive();

        // 保存当前的连接资源引用
        BufferedReader tempReader = this.reader;
        PrintWriter tempWriter = this.writer;
        Socket tempSocket = this.socket;

        // 立即重置连接状态
        connected = false;
        authenticated = false;
        this.reader = null;
        this.writer = null;
        this.socket = null;

        try {
            // 如果连接仍然有效，发送QUIT命令
            if (tempWriter != null && tempSocket != null && !tempSocket.isClosed()) {
                tempWriter.println("QUIT");
                if (tempReader != null) {
                    tempSocket.setSoTimeout(5000);  // 设置5秒超时
                    String response = tempReader.readLine();
                    System.out.println("QUIT响应: " + response);
                }
            }
        } catch (IOException e) {
            System.err.println("发送QUIT命令失败: " + e.getMessage());
        } finally {
            // 确保资源被正确关闭
            closeQuietly(tempReader);
            closeQuietly(tempWriter);
            closeQuietly(tempSocket);
        }
    }

    /**
     * 启动保持连接的定时器
     * 定期发送NOOP命令以保持连接活跃
     */
    private void startKeepAlive() {
        keepAliveTimer = new Timer(true);  // 使用守护线程
        keepAliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isConnected() && authenticated) {
                    try {
                        // 发送NOOP（空操作）命令
                        writer.println("NOOP");
                        String response = reader.readLine();
                        if (!response.startsWith("+OK")) {
                            // 服务器响应异常，处理断连
                            handleDisconnection();
                        }
                    } catch (IOException e) {
                        // 连接已断开
                        handleDisconnection();
                    }
                }
            }
        }, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL);
    }

    /**
     * 停止保持连接的定时器
     */
    private void stopKeepAlive() {
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
    }

    /**
     * 处理连接断开的情况
     * 重置连接和认证状态
     */
    private void handleDisconnection() {
        connected = false;
        authenticated = false;
        stopKeepAlive();
    }

    /**
     * 静默关闭AutoCloseable资源
     * @param resource 需要关闭的资源
     */
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // 静默处理关闭异常
            }
        }
    }

    /**
     * 静默关闭Socket
     * @param socket 需要关闭的Socket
     */
    private void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                // 静默处理关闭异常
            }
        }
    }

    /**
     * 检查是否已连接到服务器
     * @return 连接状态
     */
    public boolean isConnected() { return connected; }

    /**
     * 检查是否已通过认证
     * @return 认证状态
     */
    public boolean isAuthenticated() { return authenticated; }
}