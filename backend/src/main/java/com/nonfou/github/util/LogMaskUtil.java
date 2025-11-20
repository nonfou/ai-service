package com.nonfou.github.util;

/**
 * 日志脱敏工具类
 * 用于在日志中隐藏敏感信息,防止敏感数据泄露
 *
 * <p>使用示例:</p>
 * <pre>
 * log.info("用户登录: email={}", LogMaskUtil.maskEmail(email));
 * log.info("API调用: key={}", LogMaskUtil.maskApiKey(apiKey));
 * </pre>
 *
 * @author Security Team
 * @since 2025-11-20
 */
public class LogMaskUtil {

    /**
     * 邮箱脱敏
     * <p>示例: user@example.com -> u***@example.com</p>
     *
     * @param email 原始邮箱地址
     * @return 脱敏后的邮箱地址
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }

        if (!email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }

        String username = parts[0];
        String domain = parts[1];

        // 用户名部分脱敏
        String maskedUsername;
        if (username.length() <= 1) {
            maskedUsername = "*";
        } else if (username.length() <= 3) {
            maskedUsername = username.charAt(0) + "**";
        } else {
            maskedUsername = username.charAt(0) + "***" + username.charAt(username.length() - 1);
        }

        return maskedUsername + "@" + domain;
    }

    /**
     * 手机号脱敏
     * <p>示例: 13812345678 -> 138****5678</p>
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }

        if (phone.length() < 7) {
            return "***";
        }

        if (phone.length() == 11) {
            // 中国手机号: 138****5678
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            // 其他长度: 保留前3位和后4位
            int prefixLength = Math.min(3, phone.length() - 4);
            int suffixLength = Math.min(4, phone.length() - prefixLength);
            return phone.substring(0, prefixLength) + "****" +
                   phone.substring(phone.length() - suffixLength);
        }
    }

    /**
     * API Key 脱敏
     * <p>示例: sk-abc123def456 -> sk-abc***456</p>
     *
     * @param apiKey 原始API Key
     * @return 脱敏后的API Key
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "***";
        }

        if (apiKey.length() < 10) {
            return "***";
        }

        // 保留前6位和后3位
        return apiKey.substring(0, 6) + "***" +
               apiKey.substring(apiKey.length() - 3);
    }

    /**
     * 密码脱敏
     * <p>完全隐藏,只显示长度</p>
     *
     * @param password 原始密码
     * @return 脱敏后的密码(只显示长度)
     */
    public static String maskPassword(String password) {
        if (password == null || password.isBlank()) {
            return "***";
        }

        return "***(" + password.length() + "位)";
    }

    /**
     * 身份证号脱敏
     * <p>示例: 110101199001011234 -> 110***1234</p>
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return "***";
        }

        if (idCard.length() < 8) {
            return "***";
        }

        // 保留前3位和后4位
        return idCard.substring(0, 3) + "***" +
               idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡号脱敏
     * <p>示例: 6222021234567890123 -> 6222****0123</p>
     *
     * @param cardNo 原始银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String cardNo) {
        if (cardNo == null || cardNo.isBlank()) {
            return "***";
        }

        if (cardNo.length() < 8) {
            return "***";
        }

        // 保留前4位和后4位
        return cardNo.substring(0, 4) + "****" +
               cardNo.substring(cardNo.length() - 4);
    }

    /**
     * IP 地址脱敏
     * <p>示例: 192.168.1.100 -> 192.168.***.***</p>
     *
     * @param ip 原始IP地址
     * @return 脱敏后的IP地址
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "***";
        }

        // IPv4
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.***";
            }
        }

        // IPv6 - 保留前两段
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":***:***";
            }
        }

        return "***";
    }

    /**
     * 通用脱敏方法
     * <p>保留前n位和后m位,中间用星号替代</p>
     *
     * @param text   原始文本
     * @param prefix 保留前缀长度
     * @param suffix 保留后缀长度
     * @return 脱敏后的文本
     */
    public static String mask(String text, int prefix, int suffix) {
        if (text == null || text.isBlank()) {
            return "***";
        }

        int length = text.length();

        if (length <= prefix + suffix) {
            return "***";
        }

        String prefixPart = text.substring(0, prefix);
        String suffixPart = text.substring(length - suffix);

        return prefixPart + "***" + suffixPart;
    }

    /**
     * 完全隐藏
     * <p>将所有内容替换为星号</p>
     *
     * @param text 原始文本
     * @return 脱敏后的文本(全星号)
     */
    public static String maskAll(String text) {
        if (text == null || text.isBlank()) {
            return "***";
        }

        return "***";
    }
}
