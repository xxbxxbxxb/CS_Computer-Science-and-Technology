package org.pop3;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "pop3client.properties";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    // 加载配置
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("加载配置文件失败: " + e.getMessage());
            }
        }
    }

    // 保存配置
    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "POP3 Client Configuration");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    // 获取服务器配置
    public String getServer() {
        return properties.getProperty("server", "pop.163.com");
    }

    public void setServer(String server) {
        properties.setProperty("server", server);
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("port", "995"));
    }

    public void setPort(int port) {
        properties.setProperty("port", String.valueOf(port));
    }

    public boolean isUseSSL() {
        return Boolean.parseBoolean(properties.getProperty("useSSL", "true"));
    }

    public void setUseSSL(boolean useSSL) {
        properties.setProperty("useSSL", String.valueOf(useSSL));
    }

    public String getUsername() {
        return properties.getProperty("username", "");
    }

    public void setUsername(String username) {
        properties.setProperty("username", username);
    }

    // 获取保存的邮件下载目录
    public String getDownloadDirectory() {
        return properties.getProperty("downloadDir", "./downloads");
    }

    public void setDownloadDirectory(String dir) {
        properties.setProperty("downloadDir", dir);
    }

    // 获取是否记住密码（注意：实际应用中应该加密存储）
    public boolean isRememberPassword() {
        return Boolean.parseBoolean(properties.getProperty("rememberPassword", "false"));
    }

    public void setRememberPassword(boolean remember) {
        properties.setProperty("rememberPassword", String.valueOf(remember));
    }

    // 获取最后查看的邮件序号
    public int getLastViewedMessage() {
        return Integer.parseInt(properties.getProperty("lastViewed", "0"));
    }

    public void setLastViewedMessage(int messageNumber) {
        properties.setProperty("lastViewed", String.valueOf(messageNumber));
    }
}