package org.pop3;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

/**
 * POP3邮件客户端图形用户界面类
 *
 * 功能说明：
 * 1. 提供友好的图形界面来操作POP3邮件客户端
 * 2. 支持SSL/TLS加密连接
 * 3. 支持邮件列表查看、邮件内容显示、附件管理
 * 4. 采用现代化的UI设计，使用自定义颜色方案
 * 5. 支持邮件导出、附件下载和预览等功能
 *
 * @author POP3客户端开发团队
 * @version 2.0
 */
public class POP3SSLClientGUI extends JFrame {
    // ========== 核心业务对象 ==========
    private POP3SSLClient client;  // POP3客户端实例，负责与服务器通信

    // ========== 连接配置UI组件 ==========
    private JTextField serverField;      // 服务器地址输入框
    private JTextField portField;        // 端口号输入框
    private JTextField usernameField;    // 用户名输入框
    private JPasswordField passwordField; // 密码/授权码输入框
    private JCheckBox sslCheckBox;       // SSL加密选择框

    // ========== 功能按钮 ==========
    private JButton connectButton;    // 连接服务器按钮
    private JButton loginButton;      // 登录邮箱按钮
    private JButton getListButton;    // 获取邮件列表按钮
    private JButton disconnectButton; // 断开连接按钮
    private JButton quitButton;       // 退出程序按钮

    // ========== 邮件列表相关组件 ==========
    private JList<String> messageList;          // 邮件列表显示组件
    private DefaultListModel<String> listModel; // 邮件列表数据模型

    // ========== 邮件内容显示组件 ==========
    private JTextArea messageContentArea; // 邮件内容文本区域
    private JLabel statusLabel;           // 状态栏标签
    private JLabel mailboxInfoLabel;      // 邮箱信息标签（显示邮件数量等）
    private JCheckBox showRawCheckBox;    // 显示原始内容复选框

    // ========== 附件管理组件 ==========
    private JTable attachmentTable;                    // 附件列表表格
    private DefaultTableModel attachmentTableModel;    // 附件表格数据模型
    private EmailMessage currentEmail;                 // 当前查看的邮件对象
    private JPanel attachmentPanel;                    // 附件面板容器

    // ========== 布局组件 ==========
    private JSplitPane mainSplitPane;    // 主分割面板（左右分割）
    private JSplitPane contentSplitPane; // 内容分割面板（上下分割）

    // ========== UI设计常量 - 颜色方案 ==========
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);   // 主色调 - 蓝色
    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);    // 成功色 - 绿色
    private static final Color DANGER_COLOR = new Color(192, 57, 43);     // 危险色 - 红色
    private static final Color WARNING_COLOR = new Color(243, 156, 18);   // 警告色 - 橙色
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 247); // 背景色 - 浅灰
    private static final Color PANEL_BACKGROUND = Color.WHITE;             // 面板背景色 - 白色

    // ========== UI设计常量 - 字体设置 ==========
    private static final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 14);   // 标题字体
    private static final Font NORMAL_FONT = new Font("微软雅黑", Font.PLAIN, 12); // 普通字体
    private static final Font BUTTON_FONT = new Font("微软雅黑", Font.PLAIN, 12); // 按钮字体

    /**
     * 构造函数
     * 初始化GUI界面，设置事件处理器，应用UI默认设置
     */
    public POP3SSLClientGUI() {
        initializeGUI();        // 初始化界面组件
        setupEventHandlers();   // 设置事件处理器
        setUIDefaults();       // 设置UI默认值
    }

    /**
     * 设置全局UI默认值
     * 统一设置整个应用的字体样式
     */
    private void setUIDefaults() {
        // 设置全局字体
        UIManager.put("Button.font", BUTTON_FONT);      // 按钮字体
        UIManager.put("Label.font", NORMAL_FONT);       // 标签字体
        UIManager.put("TextField.font", NORMAL_FONT);   // 文本框字体
        UIManager.put("TextArea.font", NORMAL_FONT);    // 文本区域字体
        UIManager.put("Table.font", NORMAL_FONT);       // 表格字体
        UIManager.put("List.font", NORMAL_FONT);        // 列表字体
    }

    /**
     * 初始化GUI界面
     * 创建窗口框架，设置布局，添加各个面板组件
     */
    private void initializeGUI() {
        // ========== 设置窗口基本属性 ==========
        setTitle("POP3 邮件客户端 - 安全邮件管理系统");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 使用自定义关闭操作

        // 尝试设置窗口图标
        try {
            setIconImage(new ImageIcon(getClass().getResource("/mail-icon.png")).getImage());
        } catch (Exception e) {
            // 如果没有图标文件，使用默认图标
        }

        // 设置整体背景色
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(0, 0));

        // 添加窗口关闭监听器，确保正确退出
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quitApplication();
            }
        });

        // ========== 创建主要面板 ==========
        // 顶部：连接配置面板
        add(createConnectionPanel(), BorderLayout.NORTH);

        // 中部：主分割面板（左侧邮件列表 + 右侧内容区域）
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createListPanel(), createContentPanel());
        mainSplitPane.setDividerLocation(450);    // 设置分割位置
        mainSplitPane.setDividerSize(8);          // 分割条宽度
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(BACKGROUND_COLOR);

        add(mainSplitPane, BorderLayout.CENTER);

        // 底部：状态栏
        add(createStatusPanel(), BorderLayout.SOUTH);

        // ========== 设置窗口大小和位置 ==========
        setSize(1500, 900);                              // 默认窗口大小
        setMinimumSize(new Dimension(1200, 700));        // 最小窗口大小
        setLocationRelativeTo(null);                     // 居中显示

        // 设置窗口最大化
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * 创建连接配置面板
     * 包含服务器配置、认证信息输入和操作按钮
     *
     * @return 配置好的连接面板
     */
    private JPanel createConnectionPanel() {
        // 创建主面板容器
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(PANEL_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)), // 底部边框
                BorderFactory.createEmptyBorder(10, 10, 10, 10)  // 内边距
        ));

        // 创建标题
        JLabel titleLabel = new JLabel("连接配置");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(PRIMARY_COLOR);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // ========== 创建配置输入区域 ==========
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(PANEL_BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);  // 组件间距

        // ========== 第一行：服务器配置 ==========
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        configPanel.add(createLabel("邮件服务器:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;  // 水平拉伸
        serverField = createTextField("pop.163.com", 20);  // 默认网易邮箱服务器
        configPanel.add(serverField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        configPanel.add(createLabel("端口:"), gbc);

        gbc.gridx = 3;
        portField = createTextField("995", 6);  // 默认SSL端口
        configPanel.add(portField, gbc);

        gbc.gridx = 4;
        sslCheckBox = new JCheckBox("SSL加密", true);  // 默认启用SSL
        sslCheckBox.setFont(NORMAL_FONT);
        sslCheckBox.setBackground(PANEL_BACKGROUND);
        sslCheckBox.setForeground(PRIMARY_COLOR);
        configPanel.add(sslCheckBox, gbc);

        // ========== 第二行：认证信息 ==========
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(createLabel("用户名:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = createTextField("xxx@163.com", 20);  // 示例用户名
        configPanel.add(usernameField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        configPanel.add(createLabel("授权码:"), gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 2;  // 跨两列
        passwordField = createPasswordField("xxx", 15);  // 示例授权码
        configPanel.add(passwordField, gbc);

        mainPanel.add(configPanel, BorderLayout.CENTER);

        // ========== 创建按钮面板 ==========
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(PANEL_BACKGROUND);

        // 创建功能按钮
        connectButton = createStyledButton("连接服务器", PRIMARY_COLOR);
        loginButton = createStyledButton("登录邮箱", SUCCESS_COLOR);
        getListButton = createStyledButton("获取邮件", PRIMARY_COLOR);
        disconnectButton = createStyledButton("断开连接", WARNING_COLOR);
        quitButton = createStyledButton("退出程序", DANGER_COLOR);

        // 设置初始按钮状态（未连接时部分按钮禁用）
        loginButton.setEnabled(false);
        getListButton.setEnabled(false);
        disconnectButton.setEnabled(false);

        // 添加快捷键（Alt+字母）
        connectButton.setMnemonic(KeyEvent.VK_C);
        loginButton.setMnemonic(KeyEvent.VK_L);
        getListButton.setMnemonic(KeyEvent.VK_G);
        disconnectButton.setMnemonic(KeyEvent.VK_D);
        quitButton.setMnemonic(KeyEvent.VK_Q);

        // 将按钮添加到面板
        buttonPanel.add(connectButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(getListButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));  // 垂直分隔线
        buttonPanel.add(disconnectButton);
        buttonPanel.add(quitButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // SSL选项变化监听器 - 自动切换端口号
        sslCheckBox.addActionListener(e -> {
            if (sslCheckBox.isSelected()) {
                portField.setText("995");  // SSL端口
            } else {
                portField.setText("110");  // 普通端口
            }
        });

        return mainPanel;
    }

    /**
     * 创建邮件列表面板
     * 显示邮件列表，提供邮件选择和基本操作功能
     *
     * @return 配置好的邮件列表面板
     */
    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 5),           // 外边距
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true)  // 圆角边框
        ));

        // ========== 创建标题区域 ==========
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel listTitle = new JLabel("邮件列表");
        listTitle.setFont(TITLE_FONT);
        listTitle.setForeground(PRIMARY_COLOR);
        headerPanel.add(listTitle, BorderLayout.NORTH);

        // 邮箱信息标签（显示邮件数量等统计信息）
        mailboxInfoLabel = new JLabel("邮箱信息: 未连接");
        mailboxInfoLabel.setFont(NORMAL_FONT);
        mailboxInfoLabel.setForeground(Color.GRAY);
        headerPanel.add(mailboxInfoLabel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);

        // ========== 创建邮件列表 ==========
        listModel = new DefaultListModel<>();
        messageList = new JList<>(listModel);
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 单选模式
        messageList.setFont(NORMAL_FONT);
        messageList.setBackground(Color.WHITE);
        messageList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 设置自定义列表项渲染器，美化显示效果
        messageList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));  // 增加内边距
                if (isSelected) {
                    label.setBackground(PRIMARY_COLOR.brighter());  // 选中时的背景色
                    label.setForeground(Color.WHITE);               // 选中时的文字色
                }
                return label;
            }
        });

        // 将列表放入滚动面板
        JScrollPane scrollPane = new JScrollPane(messageList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        // ========== 创建操作按钮面板 ==========
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.setBackground(PANEL_BACKGROUND);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 创建操作按钮
        JButton viewButton = createSmallButton("查看邮件", PRIMARY_COLOR);
        JButton deleteButton = createSmallButton("删除", DANGER_COLOR);
        JButton refreshButton = createSmallButton("刷新", SUCCESS_COLOR);

        // 绑定按钮事件
        viewButton.addActionListener(e -> viewSelectedMessage());
        deleteButton.addActionListener(e -> deleteSelectedMessage());
        refreshButton.addActionListener(e -> refreshMessageList());

        // 添加工具提示
        viewButton.setToolTipText("查看选中的邮件内容");
        deleteButton.setToolTipText("删除选中的邮件");
        refreshButton.setToolTipText("刷新邮件列表");

        buttonPanel.add(viewButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建邮件内容显示面板
     * 包含邮件内容文本区域和附件管理区域
     *
     * @return 配置好的内容面板
     */
    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);

        // 创建垂直分割面板（上部显示邮件内容，下部显示附件）
        contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        contentSplitPane.setDividerLocation(550);    // 分割位置
        contentSplitPane.setDividerSize(8);          // 分割条大小
        contentSplitPane.setBorder(null);
        contentSplitPane.setBackground(BACKGROUND_COLOR);

        // ========== 上部分：邮件内容区域 ==========
        JPanel emailContentPanel = new JPanel(new BorderLayout());
        emailContentPanel.setBackground(PANEL_BACKGROUND);
        emailContentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 5, 5, 10),
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true)
        ));

        // 创建标题和工具栏
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel contentTitle = new JLabel("邮件内容");
        contentTitle.setFont(TITLE_FONT);
        contentTitle.setForeground(PRIMARY_COLOR);
        headerPanel.add(contentTitle, BorderLayout.NORTH);

        // ========== 创建工具栏 ==========
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setBackground(PANEL_BACKGROUND);

        // 显示原始内容复选框
        showRawCheckBox = new JCheckBox("显示原始内容");
        showRawCheckBox.setFont(NORMAL_FONT);
        showRawCheckBox.setBackground(PANEL_BACKGROUND);
        showRawCheckBox.addActionListener(e -> {
            int selectedIndex = messageList.getSelectedIndex();
            if (selectedIndex != -1 && !messageContentArea.getText().isEmpty()) {
                viewSelectedMessage();  // 重新加载邮件内容
            }
        });

        // 功能按钮
        JButton exportButton = createSmallButton("导出", new Color(52, 152, 219));
        JButton saveCompleteButton = createSmallButton("保存全部", new Color(46, 204, 113));
        JButton copyButton = createSmallButton("复制", new Color(155, 89, 182));

        // 设置工具提示
        exportButton.setToolTipText("导出当前邮件内容");
        saveCompleteButton.setToolTipText("保存邮件及所有附件");
        copyButton.setToolTipText("复制邮件内容到剪贴板");

        // 绑定按钮事件
        exportButton.addActionListener(e -> exportCurrentEmail());
        saveCompleteButton.addActionListener(e -> saveCompleteEmail());
        copyButton.addActionListener(e -> {
            if (!messageContentArea.getText().isEmpty()) {
                messageContentArea.selectAll();
                messageContentArea.copy();
                messageContentArea.select(0, 0);  // 取消选择
                JOptionPane.showMessageDialog(this, "内容已复制到剪贴板", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 添加工具栏组件
        toolBar.add(showRawCheckBox);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(exportButton);
        toolBar.add(saveCompleteButton);
        toolBar.add(copyButton);

        headerPanel.add(toolBar, BorderLayout.SOUTH);
        emailContentPanel.add(headerPanel, BorderLayout.NORTH);

        // ========== 创建邮件内容文本区域 ==========
        messageContentArea = new JTextArea();
        messageContentArea.setEditable(false);     // 只读
        messageContentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));  // 等宽字体
        messageContentArea.setLineWrap(true);      // 自动换行
        messageContentArea.setWrapStyleWord(true); // 按单词换行
        messageContentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messageContentArea.setBackground(new Color(252, 252, 252));

        JScrollPane contentScrollPane = new JScrollPane(messageContentArea);
        contentScrollPane.setBorder(null);
        contentScrollPane.getViewport().setBackground(new Color(252, 252, 252));
        emailContentPanel.add(contentScrollPane, BorderLayout.CENTER);

        // ========== 下部分：附件面板 ==========
        attachmentPanel = createAttachmentPanel();
        attachmentPanel.setVisible(false); // 默认隐藏，有附件时显示

        // 设置分割面板的两个组件
        contentSplitPane.setTopComponent(emailContentPanel);
        contentSplitPane.setBottomComponent(attachmentPanel);

        panel.add(contentSplitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建附件管理面板
     * 显示附件列表，提供下载、预览等功能
     *
     * @return 配置好的附件面板
     */
    private JPanel createAttachmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 10, 10),
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true)
        ));
        panel.setPreferredSize(new Dimension(0, 250));  // 设置首选高度

        // ========== 创建标题区域 ==========
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PANEL_BACKGROUND);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel attachmentTitle = new JLabel("附件");
        attachmentTitle.setFont(TITLE_FONT);
        attachmentTitle.setForeground(PRIMARY_COLOR);
        headerPanel.add(attachmentTitle, BorderLayout.WEST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // ========== 创建附件表格 ==========
        String[] columnNames = {"文件名", "类型", "大小", "操作"};
        attachmentTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // 所有单元格不可编辑
            }
        };

        attachmentTable = new JTable(attachmentTableModel);
        attachmentTable.setFont(NORMAL_FONT);
        attachmentTable.setRowHeight(30);  // 行高
        attachmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 单选
        attachmentTable.setGridColor(new Color(230, 230, 230));  // 网格线颜色
        attachmentTable.setSelectionBackground(PRIMARY_COLOR.brighter());  // 选中背景色
        attachmentTable.setSelectionForeground(Color.WHITE);              // 选中文字色

        // 设置列宽
        attachmentTable.getColumnModel().getColumn(0).setPreferredWidth(250);  // 文件名列
        attachmentTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // 类型列
        attachmentTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // 大小列
        attachmentTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // 操作列

        // 设置表头样式
        attachmentTable.getTableHeader().setFont(NORMAL_FONT);
        attachmentTable.getTableHeader().setBackground(new Color(240, 240, 240));
        attachmentTable.getTableHeader().setForeground(Color.DARK_GRAY);
        attachmentTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(230, 230, 230)));

        JScrollPane scrollPane = new JScrollPane(attachmentTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        // ========== 创建操作按钮面板 ==========
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(PANEL_BACKGROUND);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton downloadButton = createSmallButton("下载选中", PRIMARY_COLOR);
        JButton downloadAllButton = createSmallButton("下载全部", SUCCESS_COLOR);
        JButton previewButton = createSmallButton("预览", new Color(52, 152, 219));

        // 设置工具提示
        downloadButton.setToolTipText("下载选中的附件");
        downloadAllButton.setToolTipText("下载所有附件");
        previewButton.setToolTipText("预览文本类型的附件");

        // 绑定按钮事件
        downloadButton.addActionListener(e -> downloadSelectedAttachment());
        downloadAllButton.addActionListener(e -> downloadAllAttachments());
        previewButton.addActionListener(e -> previewSelectedAttachment());

        buttonPanel.add(downloadButton);
        buttonPanel.add(downloadAllButton);
        buttonPanel.add(previewButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建状态栏面板
     * 显示当前操作状态和帮助信息
     *
     * @return 配置好的状态栏面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));  // 深色背景
        panel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // 左侧：状态信息
        statusLabel = new JLabel("状态: 就绪 - 支持SSL加密和附件管理");
        statusLabel.setFont(NORMAL_FONT);
        statusLabel.setForeground(Color.WHITE);
        panel.add(statusLabel, BorderLayout.WEST);

        // 右侧：帮助提示
        JLabel helpLabel = new JLabel("提示: 使用邮箱授权码登录，而非邮箱密码");
        helpLabel.setFont(new Font("微软雅黑", Font.ITALIC, 11));
        helpLabel.setForeground(new Color(189, 195, 199));
        panel.add(helpLabel, BorderLayout.EAST);

        return panel;
    }

    // ========== UI组件创建辅助方法 ==========

    /**
     * 创建标签组件
     * @param text 标签文本
     * @return 配置好的标签
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(NORMAL_FONT);
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    /**
     * 创建文本输入框
     * @param text 默认文本
     * @param columns 列数
     * @return 配置好的文本框
     */
    private JTextField createTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(NORMAL_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),  // 边框
                BorderFactory.createEmptyBorder(5, 8, 5, 8)  // 内边距
        ));
        return field;
    }

    /**
     * 创建密码输入框
     * @param text 默认文本
     * @param columns 列数
     * @return 配置好的密码框
     */
    private JPasswordField createPasswordField(String text, int columns) {
        JPasswordField field = new JPasswordField(text, columns);
        field.setFont(NORMAL_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }

    /**
     * 创建样式化的按钮（主要功能按钮）
     * @param text 按钮文本
     * @param bgColor 背景颜色
     * @return 配置好的按钮
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);     // 不绘制边框
        button.setFocusPainted(false);      // 不绘制焦点
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));  // 手型光标
        button.setPreferredSize(new Dimension(120, 35));   // 首选大小

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());  // 鼠标进入时变暗
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);  // 鼠标离开时恢复
            }
        });

        return button;
    }

    /**
     * 创建小型按钮（次要功能按钮）
     * @param text 按钮文本
     * @param bgColor 背景颜色
     * @return 配置好的小按钮
     */
    private JButton createSmallButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, 28));

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * 设置事件处理器
     * 为主要功能按钮绑定事件监听器
     */
    private void setupEventHandlers() {
        connectButton.addActionListener(e -> connectToServer());
        loginButton.addActionListener(e -> loginToServer());
        getListButton.addActionListener(e -> getMessageList());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        quitButton.addActionListener(e -> quitApplication());
    }

    /**
     * 连接到服务器
     * 使用后台线程执行连接操作，避免阻塞UI
     */
    private void connectToServer() {
        // 获取用户输入
        String server = serverField.getText().trim();
        String portStr = portField.getText().trim();
        boolean useSSL = sslCheckBox.isSelected();

        // 验证输入
        if (server.isEmpty()) {
            showErrorDialog("请输入服务器地址");
            return;
        }

        connectButton.setEnabled(false);  // 禁用连接按钮，防止重复点击
        updateStatus("正在连接服务器...");

        // 使用SwingWorker在后台执行连接操作
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // 解析端口号，如果为空则使用默认端口
                    int port = portStr.isEmpty() ? (useSSL ? 995 : 110) : Integer.parseInt(portStr);

                    // 创建客户端实例
                    client = new POP3SSLClient(server, port, useSSL);

                    // 发布进度信息
                    publish("正在连接到服务器... " + (useSSL ? "(使用SSL)" : "(普通连接)"));

                    // 执行连接
                    return client.connect();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("端口号格式错误");
                }
            }

            @Override
            protected void process(List<String> chunks) {
                // 更新状态信息
                for (String status : chunks) {
                    updateStatus(status);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();  // 获取连接结果
                    if (success) {
                        // 连接成功
                        updateStatus("已连接到服务器: " + server + ":" + portField.getText() +
                                (useSSL ? " (SSL)" : " (普通)"));
                        loginButton.setEnabled(true);      // 启用登录按钮
                        disconnectButton.setEnabled(true); // 启用断开按钮
                        showSuccessDialog("连接成功！");
                    } else {
                        // 连接失败
                        updateStatus("连接失败");
                        connectButton.setEnabled(true);  // 重新启用连接按钮
                        showErrorDialog("连接服务器失败，请检查服务器地址和端口");
                    }
                } catch (Exception e) {
                    // 异常处理
                    updateStatus("连接失败: " + e.getMessage());
                    connectButton.setEnabled(true);
                    String errorMsg = e instanceof IllegalArgumentException ?
                            e.getMessage() : "连接过程中发生错误";
                    showErrorDialog(errorMsg);
                }
            }
        };

        worker.execute();  // 启动后台任务
    }

    /**
     * 登录到服务器
     * 使用后台线程执行登录操作
     */
    private void loginToServer() {
        // 获取认证信息
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();

        // 验证输入
        if (username.isEmpty() || passwordChars.length == 0) {
            Arrays.fill(passwordChars, '\0');  // 清空密码数组
            showErrorDialog("请输入用户名和密码");
            return;
        }

        loginButton.setEnabled(false);  // 禁用登录按钮
        updateStatus("正在登录...");

        // 使用SwingWorker在后台执行登录操作
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            private final String password = new String(passwordChars);  // 转换密码为字符串

            @Override
            protected Boolean doInBackground() throws Exception {
                publish("正在进行身份验证...");
                return client.login(username, password);  // 执行登录
            }

            @Override
            protected void process(List<String> chunks) {
                for (String status : chunks) {
                    updateStatus(status);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();  // 获取登录结果
                    if (success) {
                        // 登录成功
                        updateStatus("登录成功");
                        getListButton.setEnabled(true);  // 启用获取邮件按钮
                        showSuccessDialog("登录成功！");
                        updateMailboxInfo();             // 更新邮箱信息
                        getMessageList();                // 自动获取邮件列表
                    } else {
                        // 登录失败
                        updateStatus("登录失败");
                        loginButton.setEnabled(true);  // 重新启用登录按钮
                        showErrorDialog("登录失败，请检查用户名和密码\n注意：需要使用邮箱授权码");
                    }
                } catch (Exception e) {
                    updateStatus("登录失败: " + e.getMessage());
                    loginButton.setEnabled(true);
                    showErrorDialog("登录过程中发生错误");
                } finally {
                    Arrays.fill(passwordChars, '\0');  // 清空密码字符数组，保证安全
                }
            }
        };

        worker.execute();
    }

    /**
     * 断开服务器连接
     * 提示用户确认后断开连接
     */
    private void disconnectFromServer() {
        // 显示确认对话框
        int confirm = showConfirmDialog("确定要断开当前连接吗？", "确认断开");

        if (confirm != JOptionPane.YES_OPTION) {
            return;  // 用户取消操作
        }

        // 断开连接
        if (client != null) {
            client.quit();
            client = null;
        }

        // 重置UI状态
        resetUIAfterDisconnect();
        updateStatus("已断开连接");
        showInfoDialog("已断开与服务器的连接");
    }

    /**
     * 重置UI状态（断开连接后）
     * 恢复到初始状态，清空所有数据
     */
    private void resetUIAfterDisconnect() {
        // 恢复按钮状态
        connectButton.setEnabled(true);
        loginButton.setEnabled(false);
        getListButton.setEnabled(false);
        disconnectButton.setEnabled(false);

        // 清空数据
        listModel.clear();                      // 清空邮件列表
        messageContentArea.setText("");         // 清空邮件内容
        mailboxInfoLabel.setText("邮箱信息: 未连接");  // 重置邮箱信息

        // 清空附件表格
        attachmentTableModel.setRowCount(0);
        attachmentPanel.setVisible(false);      // 隐藏附件面板
        currentEmail = null;                    // 清空当前邮件
    }

    /**
     * 获取邮件列表
     * 从服务器获取邮件列表并显示
     */
    private void getMessageList() {
        updateStatus("正在获取邮件列表...");
        listModel.clear();  // 清空现有列表

        // 使用后台线程获取邮件列表
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return client.getMessageList();  // 从服务器获取邮件列表
            }

            @Override
            protected void done() {
                try {
                    List<String> messages = get();  // 获取结果
                    if (messages != null) {
                        // 将邮件添加到列表模型
                        for (String message : messages) {
                            listModel.addElement(message);
                        }
                        updateStatus("邮件列表获取完成，共 " + messages.size() + " 封邮件");
                    } else {
                        updateStatus("获取邮件列表失败");
                    }
                } catch (Exception e) {
                    updateStatus("获取邮件列表失败: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    /**
     * 查看选中的邮件
     * 下载并显示选中邮件的内容
     */
    private void viewSelectedMessage() {
        int selectedIndex = messageList.getSelectedIndex();
        if (selectedIndex == -1) {
            showInfoDialog("请选择要查看的邮件");
            return;
        }

        // 获取选中邮件的信息
        String selectedMessage = listModel.getElementAt(selectedIndex);
        String[] parts = selectedMessage.split(" ");
        if (parts.length > 0) {
            try {
                int messageNumber = Integer.parseInt(parts[0]);  // 提取邮件序号
                updateStatus("正在下载邮件内容...");

                // 使用后台线程下载邮件内容
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return client.retrieveMessage(messageNumber);  // 获取邮件内容
                    }

                    @Override
                    protected void done() {
                        try {
                            String content = get();
                            if (content != null) {
                                // 创建邮件对象并显示
                                currentEmail = new EmailMessage(messageNumber, content);
                                displayEmailContent(currentEmail);      // 显示邮件内容
                                updateAttachmentTable(currentEmail);    // 更新附件表格
                                updateStatus("邮件内容加载完成");
                            } else {
                                updateStatus("获取邮件内容失败");
                                messageContentArea.setText("获取邮件内容失败");
                            }
                        } catch (Exception e) {
                            updateStatus("获取邮件内容失败: " + e.getMessage());
                            messageContentArea.setText("获取邮件内容失败: " + e.getMessage());
                        }
                    }
                };
                worker.execute();

            } catch (NumberFormatException e) {
                showErrorDialog("邮件序号解析错误");
            }
        }
    }

    /**
     * 显示邮件内容
     * 根据用户选择显示原始内容或解析后的内容
     *
     * @param email 邮件对象
     */
    private void displayEmailContent(EmailMessage email) {
        StringBuilder display = new StringBuilder();
        boolean showRaw = showRawCheckBox.isSelected();  // 是否显示原始内容

        if (showRaw) {
            // 显示原始邮件内容
            display.append("=== 原始邮件内容 ===\n");
            display.append("邮件序号: ").append(email.getMessageNumber()).append("\n\n");
            display.append("=== 原始邮件头 ===\n");
            display.append(email.getRawHeader()).append("\n");
            display.append("=== 原始邮件正文 ===\n");

            // 提取原始正文内容
            String rawContent = email.getContentRaw();
            if (rawContent != null && !rawContent.isEmpty()) {
                String[] lines = rawContent.split("\n");
                boolean inContent = false;
                StringBuilder contentBuilder = new StringBuilder();

                // 跳过邮件头，只显示正文
                for (String line : lines) {
                    if (!inContent && line.trim().isEmpty()) {
                        inContent = true;
                        continue;
                    }
                    if (inContent) {
                        contentBuilder.append(line).append("\n");
                    }
                }
                display.append(contentBuilder.toString());
            } else {
                display.append("无原始内容");
            }
        } else {
            // 显示解析后的邮件内容
            display.append(email.getDetailedInfo());  // 邮件详细信息
            display.append("\n=== 邮件内容 ===\n");
            String content = email.getContent();
            if (content != null && !content.trim().isEmpty()) {
                display.append(content);
            } else {
                display.append("无内容或内容解码失败");
            }
        }

        // 设置文本并滚动到顶部
        messageContentArea.setText(display.toString());
        messageContentArea.setCaretPosition(0);
    }

    /**
     * 更新附件表格
     * 显示邮件的附件列表
     *
     * @param email 邮件对象
     */
    private void updateAttachmentTable(EmailMessage email) {
        attachmentTableModel.setRowCount(0);  // 清空表格

        List<EmailMessage.EmailPart> attachments = email.getAttachments();
        if (attachments.isEmpty()) {
            // 没有附件，隐藏附件面板
            attachmentPanel.setVisible(false);
            contentSplitPane.setDividerLocation(contentSplitPane.getHeight());
        } else {
            // 有附件，显示附件面板
            attachmentPanel.setVisible(true);
            contentSplitPane.setDividerLocation(550);

            // 添加每个附件到表格
            for (EmailMessage.EmailPart attachment : attachments) {
                String fileName = attachment.getFileName() != null ?
                        attachment.getFileName() : "未命名附件";
                String contentType = attachment.getContentType() != null ?
                        attachment.getContentType() : "未知";
                String size = formatFileSize(attachment.getSize());

                attachmentTableModel.addRow(new Object[]{fileName, contentType, size, "下载"});
            }
        }
    }

    /**
     * 格式化文件大小
     * 将字节数转换为人类可读的格式
     *
     * @param size 文件大小（字节）
     * @return 格式化后的大小字符串
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }

    /**
     * 下载选中的附件
     * 让用户选择保存位置并下载附件
     */
    private void downloadSelectedAttachment() {
        int selectedRow = attachmentTable.getSelectedRow();
        if (selectedRow == -1) {
            showInfoDialog("请选择要下载的附件");
            return;
        }

        if (currentEmail == null) {
            return;
        }

        List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
        if (selectedRow >= attachments.size()) {
            return;
        }

        EmailMessage.EmailPart attachment = attachments.get(selectedRow);

        // 显示文件保存对话框
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(attachment.getFileName() != null ?
                attachment.getFileName() : "attachment"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();

            // 保存附件
            if (currentEmail.saveAttachment(attachment, targetFile)) {
                showSuccessDialog("附件已保存到: " + targetFile.getAbsolutePath());
            } else {
                showErrorDialog("附件下载失败");
            }
        }
    }

    /**
     * 下载所有附件
     * 让用户选择目录并下载所有附件
     */
    private void downloadAllAttachments() {
        if (currentEmail == null || currentEmail.getAttachments().isEmpty()) {
            showInfoDialog("没有附件可下载");
            return;
        }

        // 选择保存目录
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetDir = fileChooser.getSelectedFile();

            List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
            int successCount = 0;

            // 逐个保存附件
            for (EmailMessage.EmailPart attachment : attachments) {
                String fileName = attachment.getFileName() != null ?
                        attachment.getFileName() : "attachment_" + (successCount + 1);
                File targetFile = new File(targetDir, fileName);

                if (currentEmail.saveAttachment(attachment, targetFile)) {
                    successCount++;
                }
            }

            showInfoDialog(String.format("已下载 %d/%d 个附件到: %s",
                    successCount, attachments.size(), targetDir.getAbsolutePath()));
        }
    }

    /**
     * 预览选中的附件
     * 只支持预览文本类型的附件
     */
    private void previewSelectedAttachment() {
        int selectedRow = attachmentTable.getSelectedRow();
        if (selectedRow == -1) {
            showInfoDialog("请选择要预览的附件");
            return;
        }

        if (currentEmail == null) {
            return;
        }

        List<EmailMessage.EmailPart> attachments = currentEmail.getAttachments();
        if (selectedRow >= attachments.size()) {
            return;
        }

        EmailMessage.EmailPart attachment = attachments.get(selectedRow);
        String contentType = attachment.getContentType();
        String fileName = attachment.getFileName();

        // ========== 判断是否可以预览 ==========
        boolean canPreview = false;

        // 通过内容类型判断
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            canPreview = lowerContentType.startsWith("text/") ||
                    lowerContentType.contains("xml") ||
                    lowerContentType.contains("json") ||
                    lowerContentType.contains("javascript") ||
                    lowerContentType.contains("html");
        }

        // 通过文件扩展名判断
        if (!canPreview && fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            canPreview = lowerFileName.endsWith(".txt") ||
                    lowerFileName.endsWith(".log") ||
                    lowerFileName.endsWith(".xml") ||
                    lowerFileName.endsWith(".json") ||
                    lowerFileName.endsWith(".html") ||
                    lowerFileName.endsWith(".htm") ||
                    lowerFileName.endsWith(".css") ||
                    lowerFileName.endsWith(".js") ||
                    lowerFileName.endsWith(".java") ||
                    lowerFileName.endsWith(".py") ||
                    lowerFileName.endsWith(".cpp") ||
                    lowerFileName.endsWith(".c") ||
                    lowerFileName.endsWith(".h") ||
                    lowerFileName.endsWith(".sql") ||
                    lowerFileName.endsWith(".csv") ||
                    lowerFileName.endsWith(".md") ||
                    lowerFileName.endsWith(".yaml") ||
                    lowerFileName.endsWith(".yml") ||
                    lowerFileName.endsWith(".properties") ||
                    lowerFileName.endsWith(".conf") ||
                    lowerFileName.endsWith(".cfg") ||
                    lowerFileName.endsWith(".ini");
        }

        if (!canPreview) {
            showInfoDialog("此文件类型不支持预览\n支持的类型：文本文件、代码文件、配置文件等");
            return;
        }

        // ========== 获取附件内容 ==========
        String content = null;

        // 如果有解码后的字节数据，尝试转换为字符串
        if (attachment.getDecodedBytes() != null) {
            try {
                // 尝试使用附件指定的字符集
                String charset = attachment.getCharset();
                if (charset == null || charset.isEmpty() || charset.equals("gb18030")) {
                    // 尝试自动检测字符集
                    content = tryDecodeWithMultipleCharsets(attachment.getDecodedBytes());
                } else {
                    content = new String(attachment.getDecodedBytes(), charset);
                }
            } catch (Exception e) {
                System.err.println("解码附件内容失败: " + e.getMessage());
            }
        }

        // 如果还是没有内容，尝试原始内容
        if (content == null && attachment.getContent() != null) {
            content = attachment.getContent();
        }

        if (content != null && !content.trim().isEmpty()) {
            // ========== 创建预览对话框 ==========
            JDialog previewDialog = new JDialog(this, "预览: " + fileName, true);
            previewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            previewDialog.setLayout(new BorderLayout());

            // 创建文本区域显示内容
            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // ========== 添加工具栏 ==========
            JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toolBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            // 添加字符集选择下拉框
            JLabel charsetLabel = new JLabel("字符集:");
            JComboBox<String> charsetCombo = new JComboBox<>(new String[]{
                    "UTF-8", "GBK", "GB2312", "ISO-8859-1", "US-ASCII", "UTF-16", "Big5"
            });

            // 设置当前字符集
            String currentCharset = attachment.getCharset();
            if (currentCharset != null && !currentCharset.isEmpty()) {
                charsetCombo.setSelectedItem(currentCharset.toUpperCase());
            } else {
                charsetCombo.setSelectedItem("UTF-8");
            }

            // 字符集切换监听器
            charsetCombo.addActionListener(e -> {
                String selectedCharset = (String) charsetCombo.getSelectedItem();
                if (attachment.getDecodedBytes() != null) {
                    try {
                        // 使用新字符集重新解码
                        String redecodedContent = new String(attachment.getDecodedBytes(), selectedCharset);
                        textArea.setText(redecodedContent);
                        textArea.setCaretPosition(0);
                    } catch (Exception ex) {
                        showErrorDialog("使用字符集 " + selectedCharset + " 解码失败");
                    }
                }
            });

            // 添加复制按钮
            JButton copyButton = new JButton("复制全部");
            copyButton.addActionListener(e -> {
                textArea.selectAll();
                textArea.copy();
                textArea.select(0, 0);
                JOptionPane.showMessageDialog(previewDialog, "内容已复制到剪贴板", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            });

            // 添加换行切换复选框
            JCheckBox wrapCheckBox = new JCheckBox("自动换行", true);
            wrapCheckBox.addActionListener(e -> {
                textArea.setLineWrap(wrapCheckBox.isSelected());
                textArea.setWrapStyleWord(wrapCheckBox.isSelected());
            });

            // 组装工具栏
            toolBar.add(charsetLabel);
            toolBar.add(charsetCombo);
            toolBar.add(Box.createHorizontalStrut(20));
            toolBar.add(wrapCheckBox);
            toolBar.add(Box.createHorizontalStrut(20));
            toolBar.add(copyButton);

            previewDialog.add(toolBar, BorderLayout.NORTH);
            previewDialog.add(scrollPane, BorderLayout.CENTER);

            // ========== 添加状态栏 ==========
            JLabel statusBar = new JLabel(String.format(" 文件大小: %s | 行数: %d",
                    formatFileSize(content.length()),
                    content.split("\n").length));
            statusBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            previewDialog.add(statusBar, BorderLayout.SOUTH);

            // 设置对话框大小和位置
            previewDialog.setSize(800, 600);
            previewDialog.setLocationRelativeTo(this);
            previewDialog.setVisible(true);

        } else {
            showInfoDialog("无法预览此附件：内容为空或无法解码");
        }
    }

    /**
     * 尝试使用多种字符集解码
     * 当无法确定正确的字符集时使用
     *
     * @param bytes 待解码的字节数组
     * @return 解码后的字符串
     */
    private String tryDecodeWithMultipleCharsets(byte[] bytes) {
        // 尝试使用UTF-8解码，如果失败则使用默认字符集
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return new String(bytes);  // 使用平台默认字符集
        }
    }

    /**
     * 保存完整邮件
     * 将邮件内容和所有附件保存到指定目录
     */
    private void saveCompleteEmail() {
        if (currentEmail == null) {
            showInfoDialog("请先选择一封邮件");
            return;
        }

        // 选择保存目录
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // 创建以邮件序号和时间戳命名的子目录
            File targetDir = new File(fileChooser.getSelectedFile(),
                    "email_" + currentEmail.getMessageNumber() + "_" + System.currentTimeMillis());

            if (currentEmail.saveCompleteEmail(targetDir)) {
                showSuccessDialog("完整邮件已保存到: " + targetDir.getAbsolutePath());
            } else {
                showErrorDialog("保存失败");
            }
        }
    }

    /**
     * 删除选中的邮件
     * 标记邮件为删除状态（实际删除在退出时执行）
     */
    private void deleteSelectedMessage() {
        int selectedIndex = messageList.getSelectedIndex();
        if (selectedIndex == -1) {
            showInfoDialog("请选择要删除的邮件");
            return;
        }

        // 确认删除
        int confirm = showConfirmDialog("确定要删除这封邮件吗？", "确认删除");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 获取邮件序号
        String selectedMessage = listModel.getElementAt(selectedIndex);
        String[] parts = selectedMessage.split(" ");
        if (parts.length > 0) {
            try {
                int messageNumber = Integer.parseInt(parts[0]);
                updateStatus("正在删除邮件...");

                // 执行删除操作
                if (client.deleteMessage(messageNumber)) {
                    updateStatus("邮件删除成功");
                    showSuccessDialog("邮件删除成功");
                    refreshMessageList();  // 刷新邮件列表
                } else {
                    updateStatus("邮件删除失败");
                    showErrorDialog("邮件删除失败");
                }
            } catch (NumberFormatException e) {
                showErrorDialog("邮件序号解析错误");
            }
        }
    }

    /**
     * 刷新邮件列表
     * 重新获取邮件列表和邮箱统计信息
     */
    private void refreshMessageList() {
        if (client != null && client.isAuthenticated()) {
            getMessageList();  // 重新获取邮件列表

            // 更新邮箱统计信息
            int[] stat = client.getMailboxStat();
            if (stat != null) {
                mailboxInfoLabel.setText(String.format("邮箱信息: %d 封邮件, 总大小 %s",
                        stat[0], formatFileSize(stat[1])));
            }
        }
    }

    /**
     * 导出当前邮件
     * 将邮件内容保存为文本文件
     */
    private void exportCurrentEmail() {
        if (messageContentArea.getText().isEmpty()) {
            showInfoDialog("没有邮件内容可导出");
            return;
        }

        // 显示文件保存对话框
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("email_" + System.currentTimeMillis() + ".txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                FileWriter writer = new FileWriter(file);
                writer.write(messageContentArea.getText());
                writer.close();
                showSuccessDialog("邮件已导出到: " + file.getAbsolutePath());
            } catch (Exception e) {
                showErrorDialog("导出失败: " + e.getMessage());
            }
        }
    }

    /**
     * 退出应用程序
     * 确认后断开连接并退出
     */
    private void quitApplication() {
        // 显示确认对话框
        int confirm = showConfirmDialog("确定要退出程序吗？", "确认退出");

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 断开连接
        if (client != null) {
            client.quit();
            updateStatus("已断开连接并退出");
        }

        // 退出程序
        System.exit(0);
    }

    /**
     * 更新状态栏信息
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        statusLabel.setText("状态: " + status);
    }

    /**
     * 更新邮箱信息
     * 显示邮件数量和总大小
     */
    private void updateMailboxInfo() {
        SwingWorker<int[], Void> worker = new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws Exception {
                return client.getMailboxStat();  // 获取邮箱统计信息
            }

            @Override
            protected void done() {
                try {
                    int[] stat = get();
                    if (stat != null) {
                        mailboxInfoLabel.setText(String.format("邮箱信息: %d 封邮件, 总大小 %s",
                                stat[0], formatFileSize(stat[1])));
                    }
                } catch (Exception e) {
                    System.err.println("获取邮箱信息失败: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ========== 统一的对话框方法 ==========

    /**
     * 显示成功对话框
     * @param message 消息内容
     */
    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示错误对话框
     * @param message 错误消息
     */
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 显示信息对话框
     * @param message 信息内容
     */
    private void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示确认对话框
     * @param message 确认消息
     * @param title 对话框标题
     * @return 用户选择的结果
     */
    private int showConfirmDialog(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
    }

    /**
     * 检查是否已连接到服务器
     * @return 连接状态
     */
    private boolean isConnected() {
        return client != null && client.isConnected();
    }
}