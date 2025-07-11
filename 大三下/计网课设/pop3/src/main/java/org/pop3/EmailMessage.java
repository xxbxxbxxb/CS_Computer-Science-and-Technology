package org.pop3;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;

/**
 * 邮件消息类
 * 用于解析和处理POP3协议接收到的邮件内容
 * 支持MIME格式解码、附件提取、多部分邮件处理等功能
 */
public class EmailMessage {
    private int messageNumber;          // 邮件序号
    private String from;               // 发件人（解码后）
    private String fromRaw;            // 发件人（原始格式）
    private String to;                 // 收件人（解码后）
    private String toRaw;              // 收件人（原始格式）
    private String subject;            // 主题（解码后）
    private String subjectRaw;         // 主题（原始格式）
    private Date date;                 // 邮件日期
    private String content;            // 邮件内容（解码后）
    private String contentRaw;         // 邮件内容（原始格式）
    private String rawHeader;          // 原始邮件头
    private String charset = "UTF-8";  // 字符集，默认UTF-8
    private String contentType;        // 内容类型
    private String boundary;           // 多部分邮件的分隔符
    private boolean isMultipart = false; // 是否为多部分邮件
    private List<EmailPart> parts = new ArrayList<>();        // 邮件各部分列表
    private List<EmailPart> attachments = new ArrayList<>();  // 附件列表

    /**
     * 邮件部分类
     * 表示邮件的一个部分（可能是正文、附件或嵌套的多部分）
     */
    public static class EmailPart {
        private String contentType;        // 内容类型（简化版）
        private String fullContentType;    // 完整的Content-Type头
        private String charset;            // 字符集
        private String transferEncoding;   // 传输编码方式
        private String content;            // 原始内容
        private String decodedContent;     // 解码后的文本内容
        private byte[] decodedBytes;       // 解码后的二进制数据（用于附件）
        private String fileName;           // 附件文件名
        private String contentDisposition; // 内容处置方式
        private boolean isAttachment = false; // 是否为附件
        private long size;                 // 附件大小（字节）
        private String boundary;           // 嵌套multipart的分隔符
        private boolean isMultipart = false; // 是否为multipart类型
        private List<EmailPart> subParts = new ArrayList<>(); // 子部分列表

        /**
         * 默认构造函数，设置默认字符集为UTF-8
         */
        public EmailPart() {
            this.charset = "UTF-8";
        }

        // Getters and setters - 各属性的获取和设置方法
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public String getFullContentType() { return fullContentType; }
        public void setFullContentType(String fullContentType) { this.fullContentType = fullContentType; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
        public String getTransferEncoding() { return transferEncoding; }
        public void setTransferEncoding(String transferEncoding) { this.transferEncoding = transferEncoding; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getDecodedContent() { return decodedContent; }
        public void setDecodedContent(String decodedContent) { this.decodedContent = decodedContent; }
        public byte[] getDecodedBytes() { return decodedBytes; }
        public void setDecodedBytes(byte[] decodedBytes) { this.decodedBytes = decodedBytes; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentDisposition() { return contentDisposition; }
        public void setContentDisposition(String contentDisposition) { this.contentDisposition = contentDisposition; }
        public boolean isAttachment() { return isAttachment; }
        public void setAttachment(boolean attachment) { isAttachment = attachment; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getBoundary() { return boundary; }
        public void setBoundary(String boundary) { this.boundary = boundary; }
        public boolean isMultipart() { return isMultipart; }
        public void setMultipart(boolean multipart) { isMultipart = multipart; }
        public List<EmailPart> getSubParts() { return subParts; }
    }

    /**
     * 构造函数
     * @param messageNumber 邮件序号
     * @param rawContent 原始邮件内容
     */
    public EmailMessage(int messageNumber, String rawContent) {
        this.messageNumber = messageNumber;
        this.contentRaw = rawContent;
        parseMessage(rawContent);
    }

    /**
     * 解析邮件内容
     * @param rawContent 原始邮件内容
     */
    private void parseMessage(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return;
        }

        String[] lines = rawContent.split("\r?\n");
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        boolean inHeader = true;  // 标记是否在处理邮件头
        String currentHeaderLine = "";  // 当前正在处理的头部行

        // 逐行解析邮件
        for (String line : lines) {
            if (inHeader) {
                // 空行表示邮件头结束
                if (line.trim().isEmpty()) {
                    if (!currentHeaderLine.isEmpty()) {
                        parseHeaderLine(currentHeaderLine);
                    }
                    inHeader = false;
                    continue;
                }

                // 处理折叠的头部行（以空格或制表符开头的行是上一行的延续）
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    currentHeaderLine += " " + line.trim();
                } else {
                    // 解析上一个完整的头部行
                    if (!currentHeaderLine.isEmpty()) {
                        parseHeaderLine(currentHeaderLine);
                    }
                    currentHeaderLine = line;
                }
                headerBuilder.append(line).append("\n");
            } else {
                // 收集邮件正文内容
                contentBuilder.append(line).append("\n");
            }
        }

        // 处理最后一个头部行
        if (!currentHeaderLine.isEmpty()) {
            parseHeaderLine(currentHeaderLine);
        }

        this.rawHeader = headerBuilder.toString();

        // 根据邮件类型处理内容
        if (isMultipart && boundary != null) {
            parseMultipartContent(contentBuilder.toString());
        } else {
            this.content = decodeContent(contentBuilder.toString());
        }
    }

    /**
     * 解析邮件头部行
     * @param line 头部行内容
     */
    private void parseHeaderLine(String line) {
        if (line.startsWith("From:")) {
            this.fromRaw = line.substring(5).trim();
            this.from = MimeDecoder.decode(this.fromRaw);
        } else if (line.startsWith("To:")) {
            this.toRaw = line.substring(3).trim();
            this.to = MimeDecoder.decode(this.toRaw);
        } else if (line.startsWith("Subject:")) {
            this.subjectRaw = line.substring(8).trim();
            this.subject = MimeDecoder.decode(this.subjectRaw);
        } else if (line.startsWith("Date:")) {
            String dateStr = line.substring(5).trim();
            this.date = parseDate(dateStr);
        } else if (line.toLowerCase().startsWith("content-type:")) {
            parseContentType(line.substring(13).trim());
        }
    }

    /**
     * 解析Content-Type头部
     * @param contentTypeValue Content-Type的值
     */
    private void parseContentType(String contentTypeValue) {
        this.contentType = contentTypeValue;

        // 检查是否为多部分邮件
        if (contentTypeValue.toLowerCase().startsWith("multipart/")) {
            this.isMultipart = true;
            // 提取boundary参数
            int boundaryIndex = contentTypeValue.toLowerCase().indexOf("boundary=");
            if (boundaryIndex != -1) {
                String boundaryPart = contentTypeValue.substring(boundaryIndex + 9);
                this.boundary = extractBoundary(boundaryPart);
            }
        }

        // 提取字符集
        int charsetIndex = contentTypeValue.toLowerCase().indexOf("charset=");
        if (charsetIndex != -1) {
            String charsetPart = contentTypeValue.substring(charsetIndex + 8);
            this.charset = charsetPart.replaceAll("[\"']", "").split("[;\\s]")[0].trim();
        }
    }

    /**
     * 提取boundary值
     * @param boundaryPart 包含boundary的字符串部分
     * @return boundary值
     */
    private String extractBoundary(String boundaryPart) {
        // 处理带引号的boundary
        if (boundaryPart.startsWith("\"")) {
            int endQuote = boundaryPart.indexOf("\"", 1);
            if (endQuote != -1) {
                return boundaryPart.substring(1, endQuote);
            }
        }

        // 处理不带引号的boundary
        String[] parts = boundaryPart.split("[;\\s]");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            return parts[0].replaceAll("[\"']", "");
        }

        return null;
    }

    /**
     * 解析多部分邮件内容
     * @param rawContent 原始内容
     */
    private void parseMultipartContent(String rawContent) {
        if (boundary == null) {
            this.content = rawContent;
            return;
        }

        // 使用boundary分割邮件各部分
        String[] parts = rawContent.split("--" + boundary);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            part = part.replaceFirst("^[\r\n]+", "");

            // 跳过空部分和结束标记
            if (part.trim().isEmpty() || part.trim().equals("--")) {
                continue;
            }

            EmailPart emailPart = parsePart(part);
            if (emailPart != null) {
                // 如果这个部分本身也是multipart，递归处理
                if (emailPart.isMultipart() && emailPart.getBoundary() != null) {
                    parseNestedMultipart(emailPart);
                    // 递归添加所有子部分
                    addPartsRecursively(emailPart);
                } else {
                    this.parts.add(emailPart);
                    if (emailPart.isAttachment()) {
                        this.attachments.add(emailPart);
                    }
                }
            }
        }

        // 生成合并后的邮件正文
        generateCompositeContent();
    }

    /**
     * 递归添加邮件部分
     * @param parentPart 父部分
     */
    private void addPartsRecursively(EmailPart parentPart) {
        for (EmailPart subPart : parentPart.getSubParts()) {
            if (subPart.isMultipart() && subPart.getBoundary() != null) {
                // 如果子部分也是multipart，继续递归
                parseNestedMultipart(subPart);
                addPartsRecursively(subPart);
            } else {
                this.parts.add(subPart);
                if (subPart.isAttachment()) {
                    this.attachments.add(subPart);
                }
            }
        }
    }

    /**
     * 解析嵌套的多部分邮件
     * @param parentPart 父部分
     */
    private void parseNestedMultipart(EmailPart parentPart) {
        String content = parentPart.getContent();
        if (content == null || parentPart.getBoundary() == null) {
            return;
        }

        String[] subParts = content.split("--" + parentPart.getBoundary());

        for (String subPartContent : subParts) {
            subPartContent = subPartContent.replaceFirst("^[\r\n]+", "");

            if (subPartContent.trim().isEmpty() || subPartContent.trim().equals("--")) {
                continue;
            }

            EmailPart subPart = parsePart(subPartContent);
            if (subPart != null) {
                parentPart.getSubParts().add(subPart);
            }
        }
    }

    /**
     * 解析邮件的一个部分
     * @param partContent 部分内容
     * @return 解析后的EmailPart对象
     */
    private EmailPart parsePart(String partContent) {
        if (partContent.trim().isEmpty()) {
            return null;
        }

        EmailPart part = new EmailPart();
        String[] lines = partContent.split("\r?\n");
        StringBuilder contentBuilder = new StringBuilder();
        boolean inPartHeader = true;  // 标记是否在处理部分头
        String currentHeaderLine = "";

        // 逐行解析部分内容
        for (String line : lines) {
            if (inPartHeader) {
                // 空行表示部分头结束
                if (line.trim().isEmpty()) {
                    if (!currentHeaderLine.isEmpty()) {
                        parsePartHeaderLine(part, currentHeaderLine);
                    }
                    inPartHeader = false;
                    continue;
                }

                // 处理折叠的头部行
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    currentHeaderLine += " " + line.trim();
                } else {
                    if (!currentHeaderLine.isEmpty()) {
                        parsePartHeaderLine(part, currentHeaderLine);
                    }
                    currentHeaderLine = line;
                }
            } else {
                contentBuilder.append(line).append("\n");
            }
        }

        // 处理最后一个头部行
        if (!currentHeaderLine.isEmpty() && inPartHeader) {
            parsePartHeaderLine(part, currentHeaderLine);
        }

        String rawPartContent = contentBuilder.toString().trim();
        part.setContent(rawPartContent);

        // 检查是否是multipart类型
        if (part.getContentType() != null && part.getContentType().toLowerCase().startsWith("multipart/")) {
            part.setMultipart(true);
            // boundary已在parsePartHeaderLine中提取
        } else {
            // 只有非multipart部分才解码内容
            decodePartContent(part);
        }

        // 设置附件大小
        if (part.getDecodedBytes() != null) {
            part.setSize(part.getDecodedBytes().length);
        }

        return part;
    }

    /**
     * 解析部分的头部行
     * @param part EmailPart对象
     * @param line 头部行内容
     */
    private void parsePartHeaderLine(EmailPart part, String line) {
        if (line.toLowerCase().startsWith("content-type:")) {
            String contentTypeLine = line.substring(13).trim();
            part.setFullContentType(contentTypeLine);  // 保存完整的Content-Type

            // 提取主要的content-type
            String mainContentType = contentTypeLine.split(";")[0].trim();
            part.setContentType(mainContentType);

            // 检查是否是multipart
            if (mainContentType.toLowerCase().startsWith("multipart/")) {
                part.setMultipart(true);
                // 提取boundary
                int boundaryIndex = contentTypeLine.toLowerCase().indexOf("boundary=");
                if (boundaryIndex != -1) {
                    String boundaryPart = contentTypeLine.substring(boundaryIndex + 9);
                    String boundary = extractBoundary(boundaryPart);
                    if (boundary != null) {
                        part.setBoundary(boundary);
                    }
                }
            }

            // 提取字符集
            int charsetIndex = contentTypeLine.toLowerCase().indexOf("charset=");
            if (charsetIndex != -1) {
                String charsetPart = contentTypeLine.substring(charsetIndex + 8);
                String charset = charsetPart.replaceAll("[\"']", "").split("[;\\s]")[0].trim();
                part.setCharset(charset);
            }

            // 提取文件名（从Content-Type中）
            int nameIndex = contentTypeLine.toLowerCase().indexOf("name=");
            if (nameIndex != -1) {
                String namePart = contentTypeLine.substring(nameIndex + 5);
                String fileName = extractFileName(namePart);
                if (fileName != null) {
                    part.setFileName(MimeDecoder.decode(fileName));
                }
            }
        } else if (line.toLowerCase().startsWith("content-transfer-encoding:")) {
            String encoding = line.substring(26).trim();
            part.setTransferEncoding(encoding);
        } else if (line.toLowerCase().startsWith("content-disposition:")) {
            String disposition = line.substring(20).trim();
            part.setContentDisposition(disposition);

            // 检查是否是附件
            if (disposition.toLowerCase().contains("attachment")) {
                part.setAttachment(true);
            }

            // 提取文件名（从Content-Disposition中）
            int filenameIndex = disposition.toLowerCase().indexOf("filename=");
            if (filenameIndex != -1) {
                String filenamePart = disposition.substring(filenameIndex + 9);
                String fileName = extractFileName(filenamePart);
                if (fileName != null) {
                    part.setFileName(MimeDecoder.decode(fileName));
                    part.setAttachment(true);
                }
            }
        }
    }

    /**
     * 提取文件名
     * @param filenamePart 包含文件名的字符串部分
     * @return 文件名
     */
    private String extractFileName(String filenamePart) {
        // 处理带引号的文件名
        if (filenamePart.startsWith("\"")) {
            int endQuote = filenamePart.indexOf("\"", 1);
            if (endQuote != -1) {
                return filenamePart.substring(1, endQuote);
            }
        }

        // 处理不带引号的文件名
        String[] parts = filenamePart.split("[;\\s]");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            return parts[0];
        }

        return null;
    }

    /**
     * 解码部分内容
     * @param part EmailPart对象
     */
    private void decodePartContent(EmailPart part) {
        String content = part.getContent();
        String encoding = part.getTransferEncoding();
        String charset = part.getCharset();

        if (encoding == null || content.trim().isEmpty()) {
            part.setDecodedContent(content);
            return;
        }

        try {
            byte[] decodedBytes = null;

            // 根据传输编码方式解码
            if (encoding.equalsIgnoreCase("base64")) {
                // Base64解码
                String cleanContent = content.replaceAll("\\s+", "");
                decodedBytes = Base64.getDecoder().decode(cleanContent);
            } else if (encoding.equalsIgnoreCase("quoted-printable")) {
                // Quoted-Printable解码
                String decodedString = decodeQuotedPrintable(content, charset != null ? charset : "UTF-8");
                decodedBytes = decodedString.getBytes(charset != null ? charset : "UTF-8");
            } else if (encoding.equalsIgnoreCase("7bit") || encoding.equalsIgnoreCase("8bit")) {
                // 7bit或8bit编码
                decodedBytes = content.getBytes("ISO-8859-1");
            }

            if (decodedBytes != null) {
                part.setDecodedBytes(decodedBytes);

                // 如果是文本类型，也设置解码后的字符串
                if (part.getContentType() != null && part.getContentType().toLowerCase().startsWith("text/")) {
                    part.setDecodedContent(new String(decodedBytes, charset != null ? charset : "UTF-8"));
                }
            } else {
                part.setDecodedContent(content);
            }
        } catch (Exception e) {
            System.err.println("解码失败 (编码:" + encoding + ", 字符集:" + charset + "): " + e.getMessage());
            part.setDecodedContent(content);
        }
    }

    /**
     * 保存附件到文件
     * @param attachment 附件对象
     * @param targetFile 目标文件
     * @return 是否保存成功
     */
    public boolean saveAttachment(EmailPart attachment, File targetFile) {
        if (!attachment.isAttachment() || attachment.getDecodedBytes() == null) {
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(attachment.getDecodedBytes());
            return true;
        } catch (Exception e) {
            System.err.println("保存附件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 保存完整邮件（包括所有附件）
     * @param targetDirectory 目标目录
     * @return 是否保存成功
     */
    public boolean saveCompleteEmail(File targetDirectory) {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        try {
            // 保存邮件内容
            File emailFile = new File(targetDirectory, "email_content.txt");
            try (FileOutputStream fos = new FileOutputStream(emailFile)) {
                fos.write(getDetailedInfo().getBytes("UTF-8"));
                fos.write("\n\n=== 邮件正文 ===\n".getBytes("UTF-8"));
                fos.write((content != null ? content : "").getBytes("UTF-8"));
            }

            // 保存所有附件
            if (!attachments.isEmpty()) {
                File attachmentsDir = new File(targetDirectory, "attachments");
                attachmentsDir.mkdirs();

                for (EmailPart attachment : attachments) {
                    if (attachment.getFileName() != null) {
                        File attachmentFile = new File(attachmentsDir, attachment.getFileName());
                        saveAttachment(attachment, attachmentFile);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("保存完整邮件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 生成合并后的邮件正文内容
     * 优先显示text/plain部分，其次显示text/html部分
     */
    private void generateCompositeContent() {
        StringBuilder compositeContent = new StringBuilder();

        // 优先显示 text/plain 部分
        for (EmailPart part : parts) {
            if (part.getContentType() != null &&
                    part.getContentType().toLowerCase().startsWith("text/plain") &&
                    !part.isAttachment()) {
                String decodedContent = part.getDecodedContent();
                if (decodedContent != null && !decodedContent.trim().isEmpty()) {
                    compositeContent.append(decodedContent);
                    this.content = compositeContent.toString();
                    return;
                }
            }
        }

        // 如果没有 text/plain，显示第一个文本部分
        for (EmailPart part : parts) {
            if (part.getContentType() != null &&
                    part.getContentType().toLowerCase().startsWith("text/") &&
                    !part.isAttachment()) {
                String decodedContent = part.getDecodedContent();
                if (decodedContent != null && !decodedContent.trim().isEmpty()) {
                    if (part.getContentType().toLowerCase().startsWith("text/html")) {
                        // 简单去除HTML标签
                        String textContent = decodedContent.replaceAll("<[^>]+>", "").trim();
                        compositeContent.append(textContent);
                    } else {
                        compositeContent.append(decodedContent);
                    }
                    this.content = compositeContent.toString();
                    return;
                }
            }
        }

        this.content = "邮件内容解码失败或为空";
    }

    /**
     * 解码邮件正文内容
     * @param rawContent 原始内容
     * @return 解码后的内容
     */
    private String decodeContent(String rawContent) {
        // 检查是否使用Base64编码
        if (rawHeader.toLowerCase().contains("content-transfer-encoding: base64")) {
            try {
                String cleanContent = rawContent.replaceAll("\\s+", "");
                byte[] decodedBytes = Base64.getDecoder().decode(cleanContent);
                return new String(decodedBytes, charset);
            } catch (Exception e) {
                return rawContent;
            }
        }

        // 检查是否使用Quoted-Printable编码
        if (rawHeader.toLowerCase().contains("content-transfer-encoding: quoted-printable")) {
            return decodeQuotedPrintable(rawContent, charset);
        }

        // 默认使用MIME解码
        return MimeDecoder.decode(rawContent);
    }

    /**
     * 解码Quoted-Printable编码的内容
     * @param content 编码内容
     * @param charset 字符集
     * @return 解码后的内容
     */
    private String decodeQuotedPrintable(String content, String charset) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");

        // 逐行处理
        for (String line : lines) {
            // 处理软换行（以=结尾的行）
            if (line.endsWith("=")) {
                result.append(decodeQPLine(line.substring(0, line.length() - 1)));
            } else {
                result.append(decodeQPLine(line)).append("\n");
            }
        }

        try {
            // 将结果转换为指定字符集
            byte[] bytes = result.toString().getBytes("ISO-8859-1");
            return new String(bytes, charset);
        } catch (Exception e) {
            return result.toString();
        }
    }

    /**
     * 解码Quoted-Printable编码的单行
     * @param line 编码行
     * @return 解码后的行
     */
    private String decodeQPLine(String line) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            // =XX表示一个编码字符，其中XX是十六进制值
            if (c == '=' && i + 2 < line.length()) {
                try {
                    String hex = line.substring(i + 1, i + 3);
                    int value = Integer.parseInt(hex, 16);
                    result.append((char) value);
                    i += 2;  // 跳过已处理的两个字符
                } catch (NumberFormatException e) {
                    // 解析失败，保留原字符
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 解析日期字符串
     * 支持多种日期格式
     * @param dateStr 日期字符串
     * @return 解析后的Date对象
     */
    private Date parseDate(String dateStr) {
        // 支持的日期格式列表
        String[] patterns = {
                "EEE, dd MMM yyyy HH:mm:ss Z",     // 标准格式: Wed, 27 Oct 2021 14:30:00 +0800
                "dd MMM yyyy HH:mm:ss Z",           // 无星期: 27 Oct 2021 14:30:00 +0800
                "EEE, dd MMM yyyy HH:mm:ss",       // 无时区: Wed, 27 Oct 2021 14:30:00
                "dd MMM yyyy HH:mm:ss"              // 简化格式: 27 Oct 2021 14:30:00
        };

        // 尝试每种格式
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                // 继续尝试下一个格式
            }
        }
        // 所有格式都失败，返回当前日期
        return new Date();
    }

    /**
     * 获取邮件的详细信息
     * @return 格式化的详细信息字符串
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 邮件信息 ===\n");
        info.append("邮件序号: ").append(messageNumber).append("\n");
        info.append("发件人: ").append(from != null ? from : "未知").append("\n");
        info.append("收件人: ").append(to != null ? to : "未知").append("\n");
        info.append("主题: ").append(subject != null ? subject : "无主题").append("\n");
        info.append("日期: ").append(date != null ?
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) : "未知").append("\n");

        // 如果是多部分邮件，显示额外信息
        if (isMultipart) {
            info.append("邮件类型: ").append(contentType).append("\n");
            info.append("邮件部分: ").append(parts.size()).append(" 个\n");
            info.append("附件数量: ").append(attachments.size()).append(" 个\n");
        } else {
            info.append("字符集: ").append(charset).append("\n");
        }

        // 添加附件信息
        if (!attachments.isEmpty()) {
            info.append("\n=== 附件列表 ===\n");
            for (int i = 0; i < attachments.size(); i++) {
                EmailPart att = attachments.get(i);
                info.append(i + 1).append(". ");
                info.append(att.getFileName() != null ? att.getFileName() : "未命名附件");
                info.append(" (").append(formatFileSize(att.getSize())).append(")\n");
            }
        }

        return info.toString();
    }

    /**
     * 格式化文件大小
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

    // Getters - 各属性的获取方法
    public int getMessageNumber() { return messageNumber; }
    public String getFrom() { return from; }
    public String getFromRaw() { return fromRaw; }
    public String getTo() { return to; }
    public String getToRaw() { return toRaw; }
    public String getSubject() { return subject; }
    public String getSubjectRaw() { return subjectRaw; }
    public Date getDate() { return date; }
    public String getContent() { return content; }
    public String getContentRaw() { return contentRaw; }
    public String getRawHeader() { return rawHeader; }
    public String getCharset() { return charset; }
    public boolean isMultipart() { return isMultipart; }
    public List<EmailPart> getParts() { return parts; }
    public String getContentType() { return contentType; }
    public String getBoundary() { return boundary; }
    public List<EmailPart> getAttachments() { return attachments; }
}