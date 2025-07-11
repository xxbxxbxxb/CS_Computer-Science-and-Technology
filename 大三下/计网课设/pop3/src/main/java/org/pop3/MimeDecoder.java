package org.pop3;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MIME解码器类
 * 用于解码MIME编码的文本，主要处理邮件头部中的编码文本
 * 支持Base64和Quoted-Printable两种编码方式
 */
public class MimeDecoder {
    /**
     * 编码字符串的正则表达式模式
     * 匹配格式: =?字符集?编码方式?编码数据?=
     * 例如: =?UTF-8?B?5Lit5paH?= 或 =?GB2312?Q?=D6=D0=CE=C4?=
     */
    private static final Pattern ENCODED_WORD_PATTERN =
            Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]+)\\?=");

    /**
     * 解码MIME编码的文本
     * @param encodedText 编码的文本
     * @return 解码后的文本
     */
    public static String decode(String encodedText) {
        // 处理空值情况
        if (encodedText == null || encodedText.isEmpty()) {
            return encodedText;
        }
        // 调用手动解码方法
        return manualDecode(encodedText);
    }

    /**
     * 手动解码MIME编码的文本
     * 使用正则表达式匹配所有编码部分并逐个解码
     * @param encodedText 编码的文本
     * @return 解码后的文本
     */
    private static String manualDecode(String encodedText) {
        Matcher matcher = ENCODED_WORD_PATTERN.matcher(encodedText);
        StringBuffer result = new StringBuffer();

        // 查找所有匹配的编码部分
        while (matcher.find()) {
            String charset = matcher.group(1);      // 字符集 (如 UTF-8, GB2312)
            String encoding = matcher.group(2).toUpperCase();  // 编码方式 (B=Base64, Q=Quoted-Printable)
            String encodedData = matcher.group(3);  // 编码的数据

            // 解码单个编码字符串
            String decodedWord = decodeWord(charset, encoding, encodedData);
            // 替换原文中的编码部分为解码后的文本
            matcher.appendReplacement(result, decodedWord);
        }

        // 添加剩余的非编码部分
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解码单个编码字符串
     * @param charset 字符集
     * @param encoding 编码方式 (B或Q)
     * @param encodedData 编码的数据
     * @return 解码后的字符串
     */
    private static String decodeWord(String charset, String encoding, String encodedData) {
        try {
            byte[] decodedBytes;

            if ("B".equals(encoding)) {
                // Base64解码
                decodedBytes = Base64.getDecoder().decode(encodedData);
            } else if ("Q".equals(encoding)) {
                // Quoted-Printable解码
                decodedBytes = decodeQuotedPrintable(encodedData);
            } else {
                // 未知编码方式，返回原文
                return "=?" + charset + "?" + encoding + "?" + encodedData + "?=";
            }

            // 使用指定字符集创建字符串
            return new String(decodedBytes, charset);
        } catch (Exception e) {
            // 解码失败，返回原文
            return "=?" + charset + "?" + encoding + "?" + encodedData + "?=";
        }
    }

    /**
     * 解码Quoted-Printable编码的数据
     * Quoted-Printable编码规则：
     * - 下划线(_)表示空格
     * - =XX表示一个字节，XX是该字节的十六进制表示
     *
     * @param encoded 编码的字符串
     * @return 解码后的字节数组
     */
    private static byte[] decodeQuotedPrintable(String encoded) {
        // 将下划线替换为空格
        encoded = encoded.replace('_', ' ');

        ByteArrayInputStream bais = new ByteArrayInputStream(encoded.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int b;
        // 逐字节处理
        while ((b = bais.read()) != -1) {
            if (b == '=') {
                // 读取接下来的两个字符作为十六进制数
                int b1 = bais.read();
                int b2 = bais.read();
                if (b1 != -1 && b2 != -1) {
                    // 将十六进制字符转换为字节值
                    int value = Character.digit((char) b1, 16) * 16 +
                            Character.digit((char) b2, 16);
                    baos.write(value);
                }
            } else {
                // 普通字符，直接写入
                baos.write(b);
            }
        }
        return baos.toByteArray();
    }

}