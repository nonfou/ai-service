package com.nonfou.github.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码验证工具
 */
public class PasswordVerifier {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String plainPassword = "admin123";
        String hashedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHs";

        System.out.println("明文密码: " + plainPassword);
        System.out.println("哈希密码: " + hashedPassword);
        System.out.println("哈希长度: " + hashedPassword.length());
        System.out.println("验证结果: " + encoder.matches(plainPassword, hashedPassword));

        // 生成正确的哈希
        String correctHash = encoder.encode(plainPassword);
        System.out.println("\n正确的哈希值应该是:");
        System.out.println(correctHash);
        System.out.println("验证正确的哈希: " + encoder.matches(plainPassword, correctHash));
    }
}
